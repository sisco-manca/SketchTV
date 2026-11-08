#!/usr/bin/env python3
"""Run core offline checks; optional suites are explicit and never connect to a TV."""
import argparse
import os
from pathlib import Path
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[1]
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--parsers',action='store_true',help='Requires JSON_JAR')
    parser.add_argument('--browser',action='store_true',help='Requires Playwright + Chromium')
    parser.add_argument('--build',action='store_true',help='Requires JDK/Android SDK; builds but never installs')
    args=parser.parse_args()
    env=os.environ.copy()
    if env.get('JAVA_HOME'):env['PATH']=str(Path(env['JAVA_HOME'])/'bin')+os.pathsep+env.get('PATH','')
    commands=[
        [sys.executable,'tests/test_config.py'],
        [sys.executable,'tests/check_layout.py'],
        [sys.executable,'tests/check_privacy.py'],
        [sys.executable,'tests/test_device.py'],
        ['sh','tests/test_network.sh'],
        ['sh','tests/test_widgets.sh'],
        ['sh','tests/test_text.sh'],
        ['sh','tests/test_catalog.sh'],
    ]
    if args.parsers:commands.append(['sh','tests/test_parsers.sh'])
    if args.browser:commands.append([sys.executable,'tests/test_preview.py'])
    if args.build:commands.append(['sh','build.sh'])
    for command in commands:
        print('+ '+' '.join(command),flush=True)
        try:subprocess.run(command,cwd=ROOT,env=env,check=True)
        except (OSError,subprocess.CalledProcessError):return 1
    print('Selected checks passed. No real device was accessed.')
    return 0
if __name__=='__main__':sys.exit(main())
