"""Bounded, read-only off-headset diagnosis. Never wakes, casts, or overrides proximity."""
import argparse
import datetime
import json
import pathlib
import re
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', required=True)
    parser.add_argument('--metavr', required=True)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--package', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    if not re.fullmatch(r'[a-zA-Z0-9_.]+', args.package):
        parser.error('Invalid Android package')

    def run(argv):
        try:
            result = subprocess.run(argv, capture_output=True, text=True,
                                    encoding='utf-8', errors='replace', timeout=15)
            return {'exitCode': result.returncode, 'stdout': result.stdout,
                    'stderr': result.stderr}
        except subprocess.TimeoutExpired:
            return {'timeout': True}

    adb = [args.adb, '-s', args.serial, 'shell']
    cli = [args.metavr, '-d', args.serial]
    evidence = {
        'timeUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
        'serial': args.serial, 'package': args.package,
        'proximity': run(cli + ['device', 'proximity', '--status', '--json']),
        'windowFocus': run(cli + ['window', 'focus', '--json']),
        'process': run(adb + ['pidof', args.package]),
        'power': run(adb + ['dumpsys', 'power']),
        'xrFocus': 'unverified: Android window focus is not XR session focus',
        'mutations': [],
    }
    target = pathlib.Path(args.output)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(evidence, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'Saved {target}; no XR acceptance inferred.')


if __name__ == '__main__':
    main()
