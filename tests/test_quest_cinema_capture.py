"""Offline capture acceptance regression; every device/tool process is mocked."""
import contextlib
import builtins
import importlib.util
import io
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import PIL
from PIL import Image

SCRIPT = Path(__file__).resolve().parents[1] / 'tools/Test-QuestCinema.py'
SPEC = importlib.util.spec_from_file_location('quest_capture_wrapper', SCRIPT)
WRAPPER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(WRAPPER)
NAMES = sorted(WRAPPER.XR_CAPTURES)


def image_bytes(image_format='PNG', size=(1440, 1440)):
    buffer = io.BytesIO()
    Image.new('RGB', size, (30, 35, 40)).save(buffer, format=image_format)
    return buffer.getvalue()


class CaptureAcceptanceTests(unittest.TestCase):
    def run_wrapper(self, *, requests=None, format='PNG', capture_returncode=0, write_image=True,
                    corrupt=False, size=(1440, 1440), stale=False, instrument_exit=0,
                    passed=True, status=-1, android_corrupt=False, capture_exception=None):
        requests = NAMES if requests is None else requests
        state = {'request_index': 0, 'capture_calls': [], 'processes_started': 0, 'android_calls': 0}
        self.last_state = state
        with tempfile.TemporaryDirectory(prefix='vrbili-offline-capture-') as directory:
            output = Path(directory) / 'output'
            if stale:
                output.mkdir()
                (output / NAMES[0]).write_bytes(image_bytes())
            android = b'corrupt synthetic capture' if android_corrupt else image_bytes(size=(64, 64))
            capture = b'corrupt synthetic capture' if corrupt else image_bytes(format, size)

            class Process:
                def __init__(self, args, stdout, stderr):
                    self_args = ['FAKE_ADB', '-s', 'OFFLINE_SERIAL', 'shell', 'am', 'instrument']
                    if args[:6] != self_args:
                        raise AssertionError('Unexpected process invocation')
                    state['processes_started'] += 1
                    stdout.write('INSTRUMENTATION_RESULT: passed=' + str(passed).lower() + '\n')
                    stdout.write('INSTRUMENTATION_CODE: ' + str(status) + '\n')
                    stdout.flush()

                def poll(self):
                    return None if state['request_index'] < len(requests) else instrument_exit

                def terminate(self):
                    state['request_index'] = len(requests)

            def run(args, **kwargs):
                if args[:3] == ['FAKE_ADB', '-s', 'OFFLINE_SERIAL']:
                    if args[3:6] == ['shell', 'run-as', 'io.github.vrbilibili.quest.debug']:
                        assert args[6:] == ['rm', '-f', 'files/cinema-capture.txt']
                        return subprocess.CompletedProcess(args, 0, '', '')
                    if args[3:6] == ['exec-out', 'run-as', 'io.github.vrbilibili.quest.debug']:
                        assert args[6:] == ['cat', 'files/cinema-capture.txt']
                        name = requests[state['request_index']]
                        state['request_index'] += 1
                        return subprocess.CompletedProcess(args, 0, name, '')
                if args[:3] == ['FAKE_META', 'capture', 'screenshot']:
                    destination = Path(args[args.index('--output') + 1])
                    state['capture_calls'].append(destination.name)
                    if capture_exception is not None:
                        raise capture_exception
                    if write_image:
                        destination.write_bytes(capture)
                    return subprocess.CompletedProcess(args, capture_returncode, '{}', '')
                raise AssertionError('Unexpected subprocess invocation')

            def check_output(args, **kwargs):
                assert args[:6] == ['FAKE_ADB', '-s', 'OFFLINE_SERIAL', 'exec-out', 'run-as', 'io.github.vrbilibili.quest.debug']
                assert args[6] == 'cat' and args[7].removeprefix('files/') in WRAPPER.ANDROID_CAPTURES
                state['android_calls'] += 1
                return android

            argv = ['Test-QuestCinema.py', '--serial', 'OFFLINE_SERIAL', '--adb', 'FAKE_ADB', '--metavr', 'FAKE_META', '--output', str(output)]
            with patch.object(sys, 'argv', argv), patch.object(WRAPPER.subprocess, 'run', run), \
                 patch.object(WRAPPER.subprocess, 'Popen', Process), patch.object(WRAPPER.subprocess, 'check_output', check_output), \
                 patch.object(WRAPPER.time, 'sleep', lambda seconds: None), contextlib.redirect_stdout(io.StringIO()):
                WRAPPER.main()
            state['image_names'] = sorted(path.name for path in output.glob('*.png'))
        return state

    def rejected(self, **kwargs):
        with self.assertRaises((SystemExit, ValueError, OSError)):
            self.run_wrapper(**kwargs)

    def test_complete_png_captures_pass(self):
        state = self.run_wrapper()
        self.assertEqual(len(state['capture_calls']), 4)
        self.assertEqual(state['android_calls'], 8)
        self.assertEqual(len(state['image_names']), 12)

    def test_meta_jpeg_under_png_filename_passes(self):
        state = self.run_wrapper(format='JPEG')
        self.assertEqual(len(state['image_names']), 12)

    def test_nonzero_capture_result_rejected_even_with_image(self):
        self.rejected(capture_returncode=1)

    def test_nonzero_capture_without_image_rejected(self):
        self.rejected(capture_returncode=1, write_image=False)

    def test_zero_capture_result_without_file_rejected(self):
        self.rejected(write_image=False)

    def test_corrupt_image_rejected(self):
        self.rejected(corrupt=True)

    def test_wrong_xr_dimensions_rejected(self):
        self.rejected(size=(64, 64))

    def test_missing_one_required_capture_rejected(self):
        self.rejected(requests=NAMES[:-1])

    def test_no_capture_handshake_rejected(self):
        self.rejected(requests=[])

    def test_stale_output_rejected_before_device_process(self):
        self.rejected(stale=True)
        self.assertEqual(self.last_state['processes_started'], 0)

    def test_old_decoder_rejected_before_device_process(self):
        with patch.object(PIL, '__version__', '11.1.0'):
            self.rejected()
        self.assertEqual(self.last_state['processes_started'], 0)

    def test_missing_decoder_rejected_before_device_process(self):
        original = builtins.__import__

        def import_without_pillow(name, *args, **kwargs):
            if name == 'PIL':
                raise ImportError('injected missing decoder')
            return original(name, *args, **kwargs)

        with patch.object(builtins, '__import__', import_without_pillow):
            self.rejected()
        self.assertEqual(self.last_state['processes_started'], 0)

    def test_instrumentation_transport_failure_rejected(self):
        self.rejected(instrument_exit=1)

    def test_failed_journey_rejected(self):
        self.rejected(passed=False)

    def test_failed_instrumentation_status_rejected(self):
        self.rejected(status=0)

    def test_corrupt_android_capture_rejected(self):
        self.rejected(android_corrupt=True)

    def test_missing_capture_tool_rejected(self):
        self.rejected(capture_exception=FileNotFoundError('injected fake executable'))

    def test_capture_tool_timeout_rejected(self):
        self.rejected(capture_exception=subprocess.TimeoutExpired(['FAKE_META'], 25))

    def test_repeated_handshake_only_captured_once(self):
        state = self.run_wrapper(requests=[NAMES[0], NAMES[0], *NAMES[1:]])
        self.assertEqual(state['capture_calls'].count(NAMES[0]), 1)

    def test_unknown_handshake_does_not_count_as_capture(self):
        state = self.run_wrapper(requests=['unrecognized.png', *NAMES])
        self.assertEqual(set(state['capture_calls']), set(NAMES))


if __name__ == '__main__':
    unittest.main()
