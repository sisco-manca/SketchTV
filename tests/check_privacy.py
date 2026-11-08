from pathlib import Path
import re
root = Path(__file__).resolve().parents[1]
# Inspect only files intended for version control, not locally generated output.
for folder in ("src", "res", "docs", "scripts", "tests"):
    for p in (root / folder).rglob("*"):
        if not p.is_file() or p.name == "check_privacy.py": continue
        text = p.read_text()
        assert not re.search(r"(?:192\.168\.|10\.[0-9]+\.[0-9]+\.|/Users/|/workspace/)", text), p
import subprocess
tracked = subprocess.check_output(["git", "ls-files", "-z"], cwd=root).decode().split("\0")
for name in filter(None, tracked):
    assert Path(name).suffix not in (".apk", ".jks", ".keystore", ".log"), name
print("PASS: no private LAN address, personal path or tracked signing key")
