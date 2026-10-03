"""Copy an existing Flutter 3.47.5 SDK, then patch only that private copy."""
import argparse
import json
import shutil
import subprocess
from pathlib import Path

PATCHES = "modal_barrier text_selection mouse_cursor image_anim layout_builder navigation_drawer popup_menu fab null_safety_for_selectable_region selectable_region editable_text text_field scroll_position scrollable scrollable_gesture draggable_scrollable_sheet scaffold text text_painter sliver refresh_indicator bottom_sheet_android scroll_view navigator".split()
parser = argparse.ArgumentParser()
parser.add_argument("--source", required=True, type=Path)
parser.add_argument("--destination", type=Path, default=Path(__file__).resolve().parents[1] / ".tools/flutter-quest")
args = parser.parse_args()
source, destination = args.source.resolve(), args.destination.resolve()
if destination.exists():
    raise SystemExit("Destination already exists; select a new empty path. Existing SDKs are never reset.")
if source == destination or source in destination.parents:
    raise SystemExit("Destination must be outside the source SDK.")
if not (source / ".git").is_dir():
    raise SystemExit("Source must include Flutter Git metadata.")
version = json.loads(subprocess.check_output([str(source / "bin/flutter.bat"), "--version", "--machine"], text=True))
if version["frameworkVersion"] != "3.47.5":
    raise SystemExit("Expected Flutter 3.47.5; mismatched frameworks require a patch review.")
if subprocess.check_output(["git", "-C", str(source), "status", "--porcelain", "--", "packages/flutter"]).strip():
    raise SystemExit("Source framework is modified; use an unmodified SDK.")
shutil.copytree(source, destination, symlinks=True)
patch_dir = Path(__file__).resolve().parents[1] / "clients/piliplus/lib/scripts"
for name in PATCHES:
    patch = str(patch_dir / (name + ".patch"))
    subprocess.run(["git", "-C", str(destination), "apply", "--check", patch], check=True)
    subprocess.run(["git", "-C", str(destination), "apply", patch], check=True)
print("Patched private SDK:", destination)
