# usage: python3 audit.py <dump.xml> <label>  — flags clickable targets that are off-screen, tiny, or overlapping
import sys, re, xml.etree.ElementTree as ET
DP = 2.75
root = ET.parse(sys.argv[1]).getroot()
W = H = 0
nodes = []
for n in root.iter('node'):
    b = re.findall(r'\d+', n.get('bounds', '[0,0][0,0]')); x1, y1, x2, y2 = map(int, b)
    W = max(W, x2); H = max(H, y2)
    if n.get('clickable') == 'true' or n.get('long-clickable') == 'true':
        name = n.get('content-desc') or n.get('text') or ''
        if not name:
            name = ' / '.join(c.get('text') or c.get('content-desc') for c in n.iter('node') if (c.get('text') or c.get('content-desc')))[:40]
        nodes.append((name or '<unnamed>', x1, y1, x2, y2))
issues = []
for name, x1, y1, x2, y2 in nodes:
    w, h = (x2 - x1) / DP, (y2 - y1) / DP
    if w <= 1 or h <= 1: continue  # scrolled out
    if min(w, h) < 32: issues.append(f"small {w:.0f}x{h:.0f}dp: {name}")
    if name == '<unnamed>': issues.append(f"no label {w:.0f}x{h:.0f}dp at {x1},{y1}")
for i, a in enumerate(nodes):
    for b in nodes[i+1:]:
        ox = min(a[3], b[3]) - max(a[1], b[1]); oy = min(a[4], b[4]) - max(a[2], b[2])
        if ox > 8 and oy > 8:
            inside = (a[1] <= b[1] and a[3] >= b[3] and a[2] <= b[2] and a[4] >= b[4]) or (b[1] <= a[1] and b[3] >= a[3] and b[2] <= a[2] and b[4] >= a[4])
            if not inside: issues.append(f"overlap: '{a[0]}' x '{b[0]}'")
print(f"[{sys.argv[2]}] {len(nodes)} targets, {len(issues)} issues")
for i in sorted(set(issues)): print("   ", i)
