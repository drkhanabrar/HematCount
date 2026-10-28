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
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r com.hematcount.app.debug.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tee qa-test.log
grep -E "FAILURES|Exception|Error" qa-test.log | head -12 | while IFS= read -r l; do echo "::warning::$l"; done
mkdir -p "qa-out/$PROFILE"
adb exec-out run-as $PKG tar -c -C files qa > qa.tar || true
tar -xf qa.tar -C "qa-out/$PROFILE" --strip-components=1 || true
cp qa-test.log "qa-out/$PROFILE/test-log.txt" || true
ls -la "qa-out/$PROFILE"
