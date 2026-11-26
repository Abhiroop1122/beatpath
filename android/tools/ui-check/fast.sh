#!/bin/sh
# usage: tools/ui-check/fast.sh <rotation 0|1>  — launch every lesson (ids.txt) and report crashes only (~3 s each)
ADB=${ADB:-~/Library/Android/sdk/platform-tools/adb}
IDS="$(dirname "$0")/ids.txt"
$ADB shell settings put system accelerometer_rotation 0; $ADB shell settings put system user_rotation ${1:-0}; $ADB logcat -c
n=0; for id in $(cat "$IDS"); do
  $ADB shell am start -S -n com.beatpath.app/.MainActivity --es route $id >/dev/null; sleep 2.4
  if $ADB logcat -d -s AndroidRuntime:E | grep -q FATAL; then echo "CRASH $id"; $ADB logcat -d -s AndroidRuntime:E | grep -E "Exception|at com.beatpath" | head -4; $ADB logcat -c; fi
  n=$((n+1))
done; echo "rotation ${1:-0}: $n launched"
