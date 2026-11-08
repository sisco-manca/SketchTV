# Shared validator is the source of truth for rectangles and focus-ring spacing.
from pathlib import Path
import json
import sys
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from configure import validate, neighbors
c=validate(json.loads((ROOT/'project.json').read_text()))
assert len(neighbors(c['tiles']))==len(c['tiles'])
print('PASS: all focus targets are inside the safe area with space for focus rings')
