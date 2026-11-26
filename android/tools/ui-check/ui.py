# usage: python3 ui.py "<cmd>; <cmd>; ..."   cmds: route R | rot 0|1 | tap REGEX | shot NAME | scan LABEL | back | wait N | swipe up|down | crash
import sys, re, subprocess, time, xml.etree.ElementTree as ET, os
ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
S = os.environ.get("UICHECK_OUT", os.path.join(os.path.dirname(os.path.abspath(__file__)), "out"))
os.makedirs(S, exist_ok=True)
def adb(*a): return subprocess.run([ADB, *a], capture_output=True, text=True).stdout
def dump():
    adb("shell", "uiautomator", "dump", "/sdcard/d.xml"); adb("pull", "/sdcard/d.xml", f"{S}/cur.xml")
    return ET.parse(f"{S}/cur.xml").getroot()
for cmd in [c.strip() for c in sys.argv[1].split(";") if c.strip()]:
    op, _, arg = cmd.partition(" ")
    if op == "route": adb("shell", "am", "start", "-S", "-n", "com.beatpath.app/.MainActivity", "--es", "route", arg); time.sleep(3.2)
    elif op == "rot": adb("shell", "settings", "put", "system", "accelerometer_rotation", "0"); adb("shell", "settings", "put", "system", "user_rotation", arg); time.sleep(2.5)
    elif op == "back": adb("shell", "input", "keyevent", "4"); time.sleep(1.5)
    elif op == "wait": time.sleep(float(arg))
    elif op == "swipe":  # swipe up|down — scroll the page
        adb("shell", "input", "swipe", "540", "1700" if arg == "up" else "700", "540", "700" if arg == "up" else "1700", "300"); time.sleep(1.2)
    elif op == "tap":
        r = dump(); rx = re.compile(arg, re.I); hit = None
        for n in r.iter("node"):
            lab = (n.get("content-desc") or "") + "|" + (n.get("text") or "")
            if any(rx.search(v) for v in (n.get("content-desc") or "", n.get("text") or "") if v):
                x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
                if x2 > x1 and y2 > y1: hit = ((x1 + x2) // 2, (y1 + y2) // 2, lab); break
        if hit: adb("shell", "input", "tap", str(hit[0]), str(hit[1])); print(f"  tap '{arg}' -> {hit[2][:50]}"); time.sleep(1.6)
        else: print(f"  !! not found: {arg}")
    elif op == "shot":
        subprocess.run(f'{ADB} exec-out screencap -p > "{S}/{arg}.png" && sips -Z 1000 "{S}/{arg}.png" >/dev/null', shell=True)
    elif op == "scan":
        dump(); os.replace(f"{S}/cur.xml", f"{S}/d_{arg}.xml"); print(subprocess.run(["python3", "-I", os.path.join(os.path.dirname(os.path.abspath(__file__)), "audit.py"), f"{S}/d_{arg}.xml", arg], capture_output=True, text=True).stdout.rstrip())
    elif op == "crash":
        out = adb("logcat", "-d", "-s", "AndroidRuntime:E"); print("  CRASH:\n" + out[-1500:] if "FATAL" in out else "  no crash"); adb("logcat", "-c")
