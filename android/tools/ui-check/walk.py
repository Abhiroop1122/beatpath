# usage: python3 walk.py [first] [count]  — walks every lesson in ids.txt like a child would:
#   open it → get past the intro → ask for a hint → press Check with a wrong/empty answer (expects the "Try again" tray)
#   → Try again → Back (expects sad Lou: "Oh no! Do you want to leave?") → Keep going → Back → Leave (expects the Learn tab).
# Prints one line per lesson and a summary. Turn on Me → Calm motion first so screen dumps are fast.
import sys, re, subprocess, time, os, xml.etree.ElementTree as ET, json
ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
OUT = os.environ.get("UICHECK_OUT", os.path.join(os.path.dirname(os.path.abspath(__file__)), "out"))
os.makedirs(OUT, exist_ok=True)
def adb(*a, timeout=40):
    try: return subprocess.run([ADB, *a], capture_output=True, text=True, timeout=timeout).stdout
    except subprocess.TimeoutExpired: return ""
def dump():
    adb("shell", "uiautomator", "dump", "/sdcard/w.xml", timeout=30); adb("pull", "/sdcard/w.xml", f"{OUT}/w.xml")
    try: return ET.parse(f"{OUT}/w.xml").getroot()
    except Exception: return None
def nodes(r):
    return [] if r is None else [((n.get("content-desc") or ""), (n.get("text") or ""), n.get("bounds"), n.get("enabled")) for n in r.iter("node")]
def find(r, rx, enabled_only=True):
    p = re.compile(rx, re.I)
    for d, t, b, e in nodes(r):
        if (p.search(d) or p.search(t)) and b:
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", b))
            if x2 > x1 and y2 > y1 and (not enabled_only or e != "false"): return ((x1 + x2) // 2, (y1 + y2) // 2)
    return None
def tap(pt): adb("shell", "input", "tap", str(pt[0]), str(pt[1])); time.sleep(0.9)
def has(r, rx): return find(r, rx, enabled_only=False) is not None
def crashed():
    o = adb("logcat", "-d", "-s", "AndroidRuntime:E"); adb("logcat", "-c")
    return "FATAL" in o, "\n".join(l for l in o.splitlines() if "Exception" in l or "at com.beatpath" in l)[:600]

ids = [l.strip() for l in open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "ids.txt")) if l.strip()]
first = int(sys.argv[1]) if len(sys.argv) > 1 else 0; count = int(sys.argv[2]) if len(sys.argv) > 2 else len(ids)
# every answer label used by choose(...) pages, read from the lesson sources, so the walker can pick one
SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../app/src/main/java/com/beatpath/app/lessons/")
OPTIONS = set()
for f in ("Basics.kt", "EarTwo.kt"):
    try:
        for grp in re.findall(r'listOf\(((?:\s*"[^"]*"\s*,?)+)\)', open(SRC + f).read()):
            OPTIONS.update(re.findall(r'"([^"]+)"', grp))
    except OSError: pass
def find_option(r):
    for d, t, b, e in nodes(r):
        lab = d.replace(", selected", "")
        if lab in OPTIONS and b:
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", b))
            if x2 > x1 and y2 > y1: return ((x1 + x2) // 2, (y1 + y2) // 2)
    return None
results = []
adb("logcat", "-c")
for lid in ids[first:first + count]:
    res = {"id": lid, "intro": None, "hint": None, "tray": None, "retry": None, "sad": None, "stay": None, "home": None, "crash": None}
    adb("shell", "am", "start", "-S", "-n", "com.beatpath.app/.MainActivity", "--es", "route", lid); time.sleep(2.6)
    r = dump()
    # Studio intro card
    go = find(r, r"^Let's go$")
    if go: tap(go); res["intro"] = "studio"; r = dump()
    # Basics: SHOW pages need taps or Continue; walk forward until a page with Check
    for _ in range(6):
        if find(r, r"^Check$") or find(r, r"^Start over$"): break
        cont = find(r, r"^Continue$")
        if cont: tap(cont); r = dump(); res["intro"] = (res["intro"] or "") + "+cont"; continue
        kb = find(r, r"^Keyboard from", enabled_only=False)
        if kb and has(r, r"Tap \d more key"):
            for dx in (-300, 0, 300): adb("shell", "input", "tap", str(kb[0] + dx), str(kb[1] + 120)); time.sleep(0.4)
            r = dump(); continue
        if has(r, r"Hear both to continue"):   # ear lesson teaching page
            for lab in (r"^High ▲$", r"^Low ▼$"):
                p = find(r, lab)
                if p: tap(p); time.sleep(1.6)
            r = dump(); continue
        break
    h = find(r, r"^Hint$")
    if h: tap(h); res["hint"] = True; r = dump()
    else: res["hint"] = False
    # choose a deliberately wrong-ish answer where possible: ear pads / options → first option; otherwise check empty
    time.sleep(0.6); r = dump()
    # give an answer: an ear pad or option, a key on the keyboard, or a cell in the mini Piano roll
    opt = find(r, r"^(Pad A|Pad B|Up ▲|Same|Step|Leap|Major · bright|Minor · dark|F · IV|Higher|Lower|Low|High|H|A|D|2|3|4|5|7|8|Yes, finished)(, selected)?$")
    if not opt: opt = find_option(r)
    if opt: tap(opt); r = dump()
    elif find(r, r"^Keyboard from", enabled_only=False):
        kb = find(r, r"^Keyboard from", enabled_only=False); adb("shell", "input", "tap", str(kb[0] - 200), str(kb[1] + 150)); time.sleep(0.6); r = dump()
    elif find(r, r"^Piano roll, notes", enabled_only=False):
        pr = find(r, r"^Piano roll, notes", enabled_only=False); adb("shell", "input", "tap", str(pr[0] + 100), str(pr[1] + 200)); time.sleep(0.6); r = dump()
    if find(r, r"^Start over$", enabled_only=False) and not find(r, r"^Check$"):
        res["tray"] = "sequence"; res["retry"] = True   # checked key by key: a wrong key makes Lou say the right one, no tray
    chk = find(r, r"^Check$")
    if chk:
        tap(chk); time.sleep(0.8); r = dump()
        res["tray"] = "fail" if has(r, r"^Try again$|Hear the difference") else ("pass" if has(r, r"^(Continue|Next step|Finish lesson)$") else "none")
        if res["tray"] == "none": adb("shell", "screencap", "-p", "/sdcard/w.png"); adb("pull", "/sdcard/w.png", f"{OUT}/notray_{lid}.png")
        t = find(r, r"^(Try again|Hear the difference)$")
        if t: tap(t); time.sleep(1.2); r = dump(); res["retry"] = not has(r, r"^Try again$")
        elif res["tray"] == "pass":
            c = find(r, r"^(Continue|Next step)$")
            if c: tap(c); r = dump(); res["retry"] = "passed-on"
    else:
        res["tray"] = "no-check-enabled"
    # leave: Back → sad Lou → Keep going → Back → Leave → Learn tab
    adb("shell", "input", "keyevent", "4"); time.sleep(1.4); r = dump()
    if has(r, r"^Try again$|Hear the difference"):   # Back on a missed tray works like Try again; press again
        adb("shell", "input", "keyevent", "4"); time.sleep(1.4); r = dump()
    res["sad"] = has(r, r"Oh no!")
    if not res["sad"]: adb("shell", "screencap", "-p", "/sdcard/w.png"); adb("pull", "/sdcard/w.png", f"{OUT}/nosad_{lid}.png")
    k = find(r, r"^Keep going$")
    if k: tap(k); time.sleep(0.8); r = dump(); res["stay"] = not has(r, r"Oh no!")
    if not has(r, r"Learn tab"):
        adb("shell", "input", "keyevent", "4"); time.sleep(1.4); r = dump()
    lv = find(r, r"^Leave$")
    if lv: tap(lv); time.sleep(1.6); r = dump()
    res["home"] = has(r, r"Learn tab")
    c, trace = crashed(); res["crash"] = trace if c else False
    ok = res["sad"] and res["home"] and not res["crash"] and res["tray"] in ("fail", "pass", "sequence")
    print(("OK  " if ok else "!!  ") + json.dumps(res), flush=True)
    results.append(res)
bad = [r for r in results if not (r["sad"] and r["home"] and not r["crash"] and r["tray"] in ("fail", "pass", "sequence"))]
print(f"\n{len(results) - len(bad)}/{len(results)} lessons walked cleanly")
for b in bad: print("  needs a look:", b["id"], {k: v for k, v in b.items() if k != "id"})
