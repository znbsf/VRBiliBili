"""Inspect an APK without executing it or contacting a device."""
import argparse,hashlib,json,re,subprocess,zipfile
from pathlib import Path
p=argparse.ArgumentParser()
p.add_argument('--apk',type=Path,required=True)
p.add_argument('--aapt',type=Path,required=True)
p.add_argument('--version-code',type=int,default=6)
p.add_argument('--native-jar',type=Path)
p.add_argument('--output',type=Path,required=True)
a=p.parse_args()
def run(*args):return subprocess.check_output([str(a.aapt),*map(str,args)],text=True,encoding='utf-8',errors='replace')
badging=run('dump','badging',a.apk)
manifest=run('dump','xmltree',a.apk,'AndroidManifest.xml')
errors=[]
if not re.search(r"package: name='io.github.vrbilibili.quest' versionCode='"+str(a.version_code)+r"' versionName='0.1.0'",badging):errors.append('wrong package/version')
if "native-code: 'arm64-v8a'" not in badging:errors.append('wrong ABI')
if 'application-debuggable' in badging:errors.append('debuggable')
if re.search(r'Cinema|com\.oculus|oculus\.software|headtracking|libossdk|openxr',manifest,re.I):errors.append('XR manifest entry remains')
with zipfile.ZipFile(a.apk) as z:
 names=z.namelist();libs={n:hashlib.sha256(z.read(n)).hexdigest() for n in names if n.startswith('lib/') and n.endswith('.so')}
 if any(re.search(r'meta|openxr|ovr|ossdk|spatial',n,re.I) for n in libs):errors.append('XR native library remains')
 dex_hits=[]
 for n in names:
  if re.fullmatch(r'classes\d*\.dex',n):
   data=z.read(n)
   for needle in [b'Lcom/meta/spatial/',b'Lcom/example/piliplus/Cinema',b'Lcom/oculus/']:
    if needle in data:dex_hits.append(n+':'+needle.decode())
 if dex_hits:errors.append('XR classes remain')
 if any('meta-spatial-sdk' in n for n in names):errors.append('historical Meta assets packaged')
 for required in ['assets/third_party/open-panel/DART-LICENSES.txt','assets/third_party/open-panel/PiliPlus-GPL-3.0.txt','assets/third_party/open-panel/NATIVE-LICENSES.txt']:
  if required not in names:errors.append('missing license '+required)
 native_matches={}
 if a.native_jar:
  with zipfile.ZipFile(a.native_jar) as jar:
   for n in jar.namelist():
    if n.endswith('.so'):native_matches[n]=libs.get(n)==hashlib.sha256(jar.read(n)).hexdigest()
  if not native_matches.get('lib/arm64-v8a/libmpv.so'):errors.append('libmpv differs from pinned backend')
result={'apk':a.apk.name,'bytes':a.apk.stat().st_size,'sha256':hashlib.sha256(a.apk.read_bytes()).hexdigest(),'package':'io.github.vrbilibili.quest','versionName':'0.1.0','versionCode':a.version_code,'native_libraries':libs,'pinned_native_jar_matches':native_matches,'dex_xr_hits':dex_hits,'errors':errors,'passed':not errors}
a.output.write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps(result,indent=2))
raise SystemExit(bool(errors))
