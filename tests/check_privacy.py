from pathlib import Path
import re
import subprocess
root=Path(__file__).resolve().parents[1]
# Include new files as well as tracked files; honor .gitignore for local keys/build output.
files=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=root).decode().split('\0')
for name in filter(None,files):
    p=root/name
    assert p.suffix not in ('.apk','.aab','.jks','.keystore','.log','.pem','.key'), name
    if not p.is_file() or p.name=='check_privacy.py' or p.suffix in ('.ttf','.png'):continue
    text=p.read_text()
    assert not re.search(r'(?:192\.168\.|10\.[0-9]+\.[0-9]+\.|/Users/|/workspace/)',text),name
print('PASS: no private LAN address, personal path or signing key in publishable files')
