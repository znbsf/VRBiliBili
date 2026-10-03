"""Exercise the real debug APK on one explicitly selected Quest. No account data.

The debug-only receiver is guarded by android.permission.DUMP (ADB shell).
Results use decoder counters, not UI timers. Never searches/selects another device.
"""
import argparse
import json
from pathlib import Path
import subprocess
import time

parser = argparse.ArgumentParser()
parser.add_argument('--adb', required=True)
parser.add_argument('--serial', required=True)
parser.add_argument('--output', required=True)
args = parser.parse_args()
out = Path(args.output)
out.mkdir(parents=True, exist_ok=True)
package = 'io.github.vrbilibili.quest.debug'
base = [args.adb, '-s', args.serial]

def adb(*command, binary=False):
    r = subprocess.run(base + list(command), capture_output=True, timeout=20, check=True)
    return r.stdout if binary else r.stdout.decode('utf-8', errors='replace').strip()

def command(name):
    return adb('shell', 'am', 'broadcast', '-n', package + '/com.example.piliplus.SpatialSmokeReceiver',
               '--es', 'command', name)

def status():
    adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
    return json.loads(adb('shell', 'run-as', package, 'cat', 'files/spatial-status.json'))

checks = []
snapshots = []
def check(name, condition, details=None):
    checks.append({'name': name, 'passed': bool(condition), 'details': details})

try:
    model = adb('shell', 'getprop', 'ro.product.model')
    if 'Quest' not in model:
        raise RuntimeError('Selected device is not a Quest')
    adb('shell', 'am', 'force-stop', package)
    adb('shell', 'run-as', package, 'rm', '-f', 'files/spatial-status.json', 'files/spatial-return.txt')
    adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
    adb('shell', 'am', 'start', '-W', '-n', package + '/com.example.piliplus.SpatialSmokeActivity')
    deadline = time.monotonic() + 25
    first = None
    while time.monotonic() < deadline:
        try:
            s = status()
            snapshots.append(s)
            if s['videoFrames'] > 0 and s['spatialSceneReady']:
                first = s
                break
        except (ValueError, subprocess.CalledProcessError):
            pass
        time.sleep(.5)
    check('real_spatial_video_frames', first is not None, first)
    if first is None:
        raise RuntimeError('No decoded frame in spatial scene')
    command('pause')
    time.sleep(.4)
    paused1 = status()
    time.sleep(.8)
    paused2 = status()
    check('pause_stops_media_clock', not paused2['playing'] and
          abs(paused2['positionMs'] - paused1['positionMs']) < 120, [paused1, paused2])
    command('seek')
    time.sleep(.6)
    sought = status()
    check('seek_while_paused', abs(sought['positionMs'] - 3000) < 150 and not sought['playing'], sought)
    command('play')
    time.sleep(1)
    resumed = status()
    check('resume_decodes_more_frames', resumed['videoFrames'] > sought['videoFrames'] and
          resumed['positionMs'] > sought['positionMs'] + 400, resumed)
    check('audio_track_decoded', resumed['audioBuffers'] > 0, resumed['audioBuffers'])
    command('smaller')
    time.sleep(.3)
    scaled = status()
    check('spatial_scale_applied', abs(scaled['scale'] - .7) < .01, scaled['scale'])
    (out / 'spatial-headset.png').write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
    command('pause')
    command('seek')
    time.sleep(.4)
    command('finish')
    time.sleep(.5)
    returned = int(adb('shell', 'run-as', package, 'cat', 'files/spatial-return.txt'))
    check('exit_returns_position', abs(returned - 3000) < 150, returned)
except Exception as exc:
    check('test_completed', False, str(exc))
finally:
    report = {'checks': checks, 'snapshots': snapshots,
              'passed': sum(c['passed'] for c in checks),
              'failed': sum(not c['passed'] for c in checks)}
    (out / 'results.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(report, ensure_ascii=False))
    raise SystemExit(1 if report['failed'] else 0)
