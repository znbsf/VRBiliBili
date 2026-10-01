"""Regenerate tiny owned H.264/AAC fixture with an already installed Blender.

blender --background --factory-startup --disable-autoexec --threads 1 \
  --python tools/generate-playback-fixture.py -- captures/fixture.mp4
Only the 2D sequencer encodes 72 tiny color frames and a generated sine wave.
Normal browser tests use the committed fixture and do not need Blender.
"""
from pathlib import Path
import sys, math, struct, wave
import bpy

output = Path(sys.argv[sys.argv.index('--')+1]).resolve()
output.parent.mkdir(parents=True,exist_ok=True)
sound = output.with_suffix('.wav')
with wave.open(str(sound),'wb') as wav:
    wav.setnchannels(1);wav.setsampwidth(2);wav.setframerate(48000)
    wav.writeframes(b''.join(struct.pack('<h',int(800*math.sin(2*math.pi*440*i/48000))) for i in range(48000*6)))
scene=bpy.context.scene
scene.render.resolution_x=240;scene.render.resolution_y=136;scene.render.resolution_percentage=100
scene.render.fps=12;scene.frame_start=1;scene.frame_end=72
scene.render.use_sequencer=True
editor=scene.sequence_editor_create()
for i in range(12):
    strip=editor.sequences.new_effect(name='Owned color '+str(i),type='COLOR',channel=1,frame_start=1+i*6,frame_end=7+i*6)
    strip.color=(.12,.32,.65) if i%2 else (.65,.32,.12)
editor.sequences.new_sound(name='Owned sine tone',filepath=str(sound),channel=2,frame_start=1)
scene.render.image_settings.file_format='FFMPEG'
scene.render.ffmpeg.format='MPEG4';scene.render.ffmpeg.codec='H264';scene.render.ffmpeg.constant_rate_factor='MEDIUM'
scene.render.ffmpeg.audio_codec='AAC';scene.render.ffmpeg.audio_channels='STEREO';scene.render.ffmpeg.audio_mixrate=48000;scene.render.ffmpeg.audio_bitrate=96
scene.render.filepath=str(output)
bpy.ops.render.render(animation=True)
assert output.is_file() and output.stat().st_size>1000
print('Owned fixture encoded:',output.stat().st_size,'bytes')
