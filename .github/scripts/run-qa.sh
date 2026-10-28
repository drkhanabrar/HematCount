#!/usr/bin/env bash
# Runs inside the emulator: install app + tests, run the visual tour, pull screenshots.
set -u
PROFILE="$1"
PKG=com.hematcount.app.debug
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
./gradlew --no-daemon connectedDebugAndroidTest 2>&1 | tee qa-test.log || echo "::warning::Some QA steps failed (screenshots up to the failure are kept)"
grep -E "FAILED|Exception|AssertionError" qa-test.log | head -20 | while IFS= read -r l; do echo "::warning::$l"; done
mkdir -p "qa-out/$PROFILE"
adb exec-out run-as $PKG tar -c -C files qa > qa.tar || true
tar -xf qa.tar -C "qa-out/$PROFILE" --strip-components=1 || true
ls -la "qa-out/$PROFILE"
