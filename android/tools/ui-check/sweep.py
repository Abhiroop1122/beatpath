# Opens every lesson, dismisses the intro, audits layout, screenshots, and checks for crashes.
import subprocess, sys, os, re, time
S = os.environ.get("UICHECK_OUT", os.path.join(os.path.dirname(os.path.abspath(__file__)), "out"))
ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
rot = sys.argv[1]; ids = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "ids.txt")).read().split()
subprocess.run(f"{ADB} logcat -c", shell=True)
os.makedirs(f"{S}/sweep{rot}", exist_ok=True)
for i in ids:
    out = subprocess.run(["python3", "-I", os.path.join(os.path.dirname(os.path.abspath(__file__)), "ui.py"), f"rot {rot}; route {i}; tap Let's go; wait 0.5; scan sw"], capture_output=True, text=True).stdout
    lines = [l for l in out.splitlines() if l.startswith("    ") and not re.search(r"small \d+x\d+dp: (Preview|.* step \d+)|overlap: '(Snap|What does)", l)]
    subprocess.run(f'{ADB} exec-out screencap -p > "{S}/sweep{rot}/{i}.png" && sips -Z 700 "{S}/sweep{rot}/{i}.png" >/dev/null', shell=True)
    crash = subprocess.run(f"{ADB} logcat -d -s AndroidRuntime:E", shell=True, capture_output=True, text=True).stdout
    flag = "CRASH " if "FATAL" in crash else ""
    if "not found" in out: flag += "no-intro "
    print(f"{i}: {flag}{len(lines)} issues" + ("".join("\n" + l for l in lines)))
    if "FATAL" in crash: print(crash[-1200:]); subprocess.run(f"{ADB} logcat -c", shell=True)
