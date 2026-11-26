#!/usr/bin/env bash
# Runs inside the emulator: install app + tests, run the visual tour, pull screenshots.
set -u
PROFILE="$1"
PKG=com.hematcount.app.debug
adb wait-for-device
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell settings put global hide_error_dialogs 1 || true
adb shell settings put secure anr_show_background 0 || true
adb shell settings put secure show_first_crash_dialog_dev_option 0 || true
sleep 25
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
adb shell input keyevent 82 || true
adb shell input keyevent 3 || true
sleep 3
# wait for full boot, then install with retries (package manager can lag behind boot)
for i in $(seq 1 60); do [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ] && break; sleep 2; done
sleep 10
install_apk() {
  for attempt in 1 2 3 4; do
    out=$(adb install -r -t -g "$1" 2>&1) && { echo "$out"; return 0; }
    echo "::warning::install attempt $attempt for $(basename "$1") failed: $(echo "$out" | tail -2 | tr '\n' ' ')"
    sleep 8
  done
  return 1
}
install_apk app/build/outputs/apk/debug/app-debug.apk
install_apk app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm list instrumentation | while IFS= read -r l; do echo "::notice::$l"; done
adb shell am instrument -w -r com.hematcount.app.debug.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tee qa-test.log
grep -E "FAILURES|Exception|Error" qa-test.log | head -12 | while IFS= read -r l; do echo "::warning::$l"; done
mkdir -p "qa-out/$PROFILE"
adb exec-out run-as $PKG tar -c -C files qa > qa.tar || true
tar -xf qa.tar -C "qa-out/$PROFILE" --strip-components=1 || true
cp qa-test.log "qa-out/$PROFILE/test-log.txt" || true
ls -la "qa-out/$PROFILE"
