from pathlib import Path
import re
root = Path(__file__).resolve().parents[1]
text = (root / "src/dev/sketchtv/home/ReferenceHome.java").read_text()
block = text.split("BOXES={", 1)[1].split("};", 1)[0]
boxes = [tuple(map(int, b.split(","))) for b in re.findall(r"\{([0-9,]+)\}", block)]
assert len(boxes) == 15
for i, (l,t,r,b) in enumerate(boxes):
    assert 0 <= l < r <= 1920 and 0 <= t < b <= 1080, i
    for j, (x,y,z,w) in enumerate(boxes[:i]):
        assert min(r,z) <= max(l,x) or min(b,w) <= max(t,y), (i,j)
print("PASS: 15 focus targets inside the viewport, without overlaps")
