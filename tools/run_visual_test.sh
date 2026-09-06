#!/usr/bin/env bash
set -u
mkdir -p proof/screenshots proof/reports
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
adb shell input keyevent 82
until adb shell cmd package path com.android.systemui >/dev/null 2>&1 && \
      adb shell 'test -d /storage/emulated/0/Android' >/dev/null 2>&1; do sleep 2; done
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb logcat -c

adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r \
  com.mr131.redplayer.test/androidx.test.runner.AndroidJUnitRunner | tee proof/instrumentation.txt
test_status=${PIPESTATUS[0]}
if ! grep -q 'OK (1 test)' proof/instrumentation.txt; then test_status=1; fi

adb exec-out run-as com.mr131.redplayer tar -C files/screenshots -cf - . | tar -C proof/screenshots -xf - || true
cp -R app/build/reports/androidTests proof/reports/ || true
adb shell uiautomator dump /sdcard/current-window.xml || true
adb pull /sdcard/current-window.xml proof/ || true
adb logcat -d -v threadtime > proof/logcat.txt
adb shell dumpsys activity activities > proof/activities.txt

python tools/validate_screenshots.py proof/screenshots --required \
  01-home.png 02-settings.png 03-more-menu.png 04-private-vault.png \
  05-search.png 06-playback-speed.png 07-network-stream.png 08-cloud-import.png \
  09-video-library.png 10-system-video-picker.png 11-screen-08-subtitle-downloader.png \
  12-screen-12-technical-inspector.png 13-screen-18-history-recovery.png \
  14-equalizer-bass.png 15-sleep-timer.png 16-decoder-settings.png \
  17-vault-pin.png 18-subtitle-choices.png
validation_status=$?

if grep -Eq 'FATAL EXCEPTION.*com\.mr131\.redplayer|ANR in com\.mr131\.redplayer' proof/logcat.txt; then
  exit 20
fi
test "$test_status" -eq 0 && test "$validation_status" -eq 0
