"""Run the Quest journey with verified OpenXR captures (requires Pillow).

Use a new or empty output directory so previous captures cannot satisfy this run.
Meta may write JPEG bytes to a .png filename; validate decoded content.
"""
import argparse
import io
import re
import subprocess
import time
from pathlib import Path


XR_CAPTURES = {'quest-cinema-ui.png', 'quest-pure-ui.png', 'quest-environment-ui.png', 'quest-size-ui.png'}
ANDROID_CAPTURES = ['quest-home-ui.png', 'quest-search-ui.png', 'quest-account-ui.png', 'quest-dynamic-ui.png',
                    'quest-wide-ui.png', 'quest-detail-ui.png', 'quest-quality-ui.png', 'quest-settings-ui.png']


def image_decoder():
    try:
        from PIL import Image, __version__ as pillow_version
    except ImportError as error:
        raise SystemExit('Screenshot validation requires Pillow; install tools/requirements-quest-test.txt in the test environment') from error
    version = re.match(r'^(\d+)\.(\d+)\.(\d+)', pillow_version)
    if version is None or tuple(map(int, version.groups())) < (12, 3, 0):
        raise SystemExit('Screenshot validation requires Pillow >=12.3.0; install tools/requirements-quest-test.txt')
    return Image


def validate_image(source, expected_size=None):
    Image = image_decoder()
    with Image.open(source, formats=('PNG', 'JPEG')) as image:
        if image.format not in {'PNG', 'JPEG'}:
            raise ValueError('capture must contain a PNG or JPEG image')
        image.verify()
    if hasattr(source, 'seek'):
        source.seek(0)
    with Image.open(source, formats=('PNG', 'JPEG')) as image:
        image.load()
        if expected_size is not None and image.size != expected_size:
            raise ValueError('capture has unexpected dimensions')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--serial', required=True)
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--metavr', default='metavr')
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    image_decoder()  # Fail before starting instrumentation when the dependency is absent.
    out = Path(args.output).resolve()
    if out.exists() and any(out.iterdir()):
        raise SystemExit('Output directory must be new or empty; use a fresh directory for this run')
    out.mkdir(parents=True, exist_ok=True)
    adb = [args.adb, '-s', args.serial]
    package = 'io.github.vrbilibili.quest.debug'
    # Only our own handshake file, never user media or app data.
    subprocess.run(adb + ['shell', 'run-as', package, 'rm', '-f', 'files/cinema-capture.txt'], check=True)
    captures = set()
    attempted = set()
    capture_errors = []
    with (out / 'journey.txt').open('w', encoding='utf-8') as log:
        run = subprocess.Popen(adb + ['shell', 'am', 'instrument', '-w',
            package + '.test/com.example.piliplus.QuestUiDriver'], stdout=log, stderr=subprocess.STDOUT)
        deadline = time.monotonic() + 300
        try:
            while run.poll() is None and time.monotonic() < deadline:
                request = subprocess.run(adb + ['exec-out', 'run-as', package,
                    'cat', 'files/cinema-capture.txt'], capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=5)
                name = request.stdout.strip()
                if request.returncode == 0 and name in XR_CAPTURES and name not in attempted:
                    attempted.add(name)
                    try:
                        result = subprocess.run([args.metavr, 'capture', 'screenshot', '--device', args.serial,
                            '--output', str(out / name), '--width', '1440', '--height', '1440', '--json'],
                            capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=25)
                        (out / (name + '.capture.json')).write_text(result.stdout + result.stderr, encoding='utf-8')
                        print(name, 'capture exit', result.returncode, flush=True)
                        if result.returncode != 0:
                            raise ValueError('capture command returned a failure')
                        validate_image(out / name, expected_size=(1440, 1440))
                        captures.add(name)
                    except (OSError, ValueError, subprocess.TimeoutExpired):
                        capture_errors.append(name)
                time.sleep(1)
            if run.poll() is None:
                raise TimeoutError('Quest journey did not complete in 300 seconds')
            if run.poll() != 0:
                raise SystemExit('Instrumentation process failed; inspect journey.txt')
        finally:
            if run.poll() is None:
                run.terminate()
    print((out / 'journey.txt').read_text(encoding='utf-8'), flush=True)
    journey = (out / 'journey.txt').read_text(encoding='utf-8')
    if not re.search(r'^INSTRUMENTATION_RESULT:\s*passed=true\s*$', journey, re.MULTILINE) or not re.search(r'^INSTRUMENTATION_CODE:\s*-1\s*$', journey, re.MULTILINE):
        raise SystemExit('Quest journey failed; inspect journey.txt; no screenshots accepted')
    missing = XR_CAPTURES - captures
    if missing or capture_errors:
        raise SystemExit('OpenXR capture validation failed: ' + ', '.join(sorted(missing | set(capture_errors))))
    for name in ANDROID_CAPTURES:
        data = subprocess.check_output(adb + ['exec-out', 'run-as', package, 'cat', 'files/' + name])
        try:
            validate_image(io.BytesIO(data))
        except (OSError, ValueError) as error:
            raise SystemExit('Android capture validation failed: ' + name) from error
        (out / name).write_bytes(data)



if __name__ == '__main__':
    main()
