"""Run the app journey and capture OpenXR through Meta's compositor, not Android's display."""
import argparse
import subprocess
import time
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--serial', required=True)
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--metavr', default='metavr')
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    out = Path(args.output).resolve()
    out.mkdir(parents=True, exist_ok=True)
    adb = [args.adb, '-s', args.serial]
    package = 'io.github.vrbilibili.quest.debug'
    # Only our own handshake file, never user media or app data.
    subprocess.run(adb + ['shell', 'run-as', package, 'rm', '-f', 'files/cinema-capture.txt'], check=True)
    captures = set()
    with (out / 'journey.txt').open('w', encoding='utf-8') as log:
        run = subprocess.Popen(adb + ['shell', 'am', 'instrument', '-w',
            package + '.test/com.example.piliplus.QuestUiDriver'], stdout=log, stderr=subprocess.STDOUT)
        deadline = time.monotonic() + 240
        try:
            while run.poll() is None and time.monotonic() < deadline:
                request = subprocess.run(adb + ['exec-out', 'run-as', package,
                    'cat', 'files/cinema-capture.txt'], capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=5)
                name = request.stdout.strip()
                if request.returncode == 0 and name in {'quest-cinema-ui.png', 'quest-pure-ui.png'} and name not in captures:
                    captures.add(name)
                    result = subprocess.run([args.metavr, 'capture', 'screenshot', '--device', args.serial,
                        '--output', str(out / name), '--width', '1440', '--height', '1440', '--json'],
                        capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=25)
                    (out / (name + '.capture.json')).write_text(result.stdout + result.stderr, encoding='utf-8')
                    print(name, 'capture exit', result.returncode, flush=True)
                time.sleep(1)
            if run.poll() is None:
                raise TimeoutError('Quest journey did not complete in 240 seconds')
        finally:
            if run.poll() is None:
                run.terminate()
    print((out / 'journey.txt').read_text(encoding='utf-8'), flush=True)
    for name in ['quest-home-ui.png', 'quest-detail-ui.png', 'quest-quality-ui.png', 'quest-settings-ui.png']:
        data = subprocess.check_output(adb + ['exec-out', 'run-as', package, 'cat', 'files/' + name])
        (out / name).write_bytes(data)
    if 'passed=true' not in (out / 'journey.txt').read_text(encoding='utf-8'):
        raise SystemExit('Quest journey failed; inspect journey.txt')


if __name__ == '__main__':
    main()
