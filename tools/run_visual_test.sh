#!/usr/bin/env bash
set -uo pipefail
mkdir -p proof/screenshots proof/reports proof/device-state
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
adb shell input keyevent 82
until adb shell cmd package path com.android.systemui >/dev/null 2>&1 && \
      adb shell 'test -d /storage/emulated/0/Android' >/dev/null 2>&1; do sleep 2; done
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb logcat -c
adb shell getprop > proof/device-state/properties.txt
adb shell wm size > proof/device-state/display.txt
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER com.mr131.redplayer \
  > proof/device-state/launcher-activity.txt

collect_evidence() {
  adb pull /sdcard/Android/data/com.mr131.redplayer/files/screenshots/. proof/screenshots/ \
    > proof/pull-screenshots.txt 2>&1 || true
  cp -R app/build/reports/androidTests proof/reports/ 2>/dev/null || true
  cp -R app/build/outputs/androidTest-results proof/reports/ 2>/dev/null || true
  adb shell uiautomator dump /sdcard/current-window.xml >/dev/null 2>&1 || true
  adb pull /sdcard/current-window.xml proof/ >/dev/null 2>&1 || true
  adb logcat -d -v threadtime > proof/logcat.txt || true
  adb shell dumpsys activity activities > proof/activities.txt || true
  adb shell dumpsys window windows > proof/windows.txt || true
}
trap collect_evidence EXIT

dismiss_system_wait_dialog() {
  local dump_path="$1"
  if grep -q 'resource-id="android:id/aerr_wait"' "$dump_path"; then
    adb shell input keyevent KEYCODE_TAB
    adb shell input keyevent KEYCODE_TAB
    adb shell input keyevent KEYCODE_ENTER
    return 0
  fi
  return 1
}

wait_for_android_ready() {
  local ready=0
  adb shell input keyevent KEYCODE_HOME
  for attempt in $(seq 1 90); do
    if adb shell uiautomator dump /sdcard/system-ready.xml >/dev/null 2>&1 && \
       adb pull /sdcard/system-ready.xml proof/device-state/system-ready.xml >/dev/null 2>&1; then
      if dismiss_system_wait_dialog proof/device-state/system-ready.xml; then
        sleep 1
        continue
      fi
      if adb shell service check activity 2>/dev/null | grep -q 'found' && \
         adb shell cmd package list packages android 2>/dev/null | grep -q '^package:android$'; then
        ready=1
        break
      fi
    fi
    sleep 1
  done
  test "$ready" -eq 1
}

install_apk() {
  local apk="$1"
  for attempt in 1 2 3 4 5; do
    if adb install -r -t "$apk"; then return 0; fi
    sleep 5
  done
  return 1
}
install_apk app/build/outputs/apk/debug/app-debug.apk 2>&1 | tee proof/install-app.txt
app_install_status=${PIPESTATUS[0]}
test "$app_install_status" -eq 0 || exit 11
install_apk app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk 2>&1 | tee proof/install-test-app.txt
test_install_status=${PIPESTATUS[0]}
test "$test_install_status" -eq 0 || exit 12
adb shell pm path com.mr131.redplayer | grep -q '^package:' || exit 13
adb shell pm grant com.mr131.redplayer android.permission.READ_EXTERNAL_STORAGE || true
wait_for_android_ready || exit 14

# Prove that the installed launcher activity starts and reaches its unique home marker.
adb shell am force-stop com.mr131.redplayer
adb shell am start -S -n com.mr131.redplayer/.MainActivity | tee proof/launcher.txt
launcher_status=${PIPESTATUS[0]}
test "$launcher_status" -eq 0 || exit 15
launcher_ready=0
for attempt in $(seq 1 90); do
  if adb shell uiautomator dump /sdcard/launcher-window.xml >/dev/null 2>&1 && \
     adb pull /sdcard/launcher-window.xml proof/device-state/launcher-window.xml >/dev/null 2>&1 && \
     grep -q 'content-desc="131 Red Player Home Ready"' proof/device-state/launcher-window.xml; then
    launcher_ready=1
    break
  fi
  dismiss_system_wait_dialog proof/device-state/launcher-window.xml || true
  sleep 1
done
test "$launcher_ready" -eq 1 || exit 16
adb shell dumpsys activity activities > proof/device-state/launcher-activities.txt
grep -Eq 'mResumedActivity.*com\.mr131\.redplayer/.MainActivity|topResumedActivity=.*com\.mr131\.redplayer/.MainActivity' \
  proof/device-state/launcher-activities.txt || exit 17

timeout 15m adb shell am instrument -w -r \
  com.mr131.redplayer.test/androidx.test.runner.AndroidJUnitRunner | tee proof/instrumentation.txt
test_status=${PIPESTATUS[0]}
if ! grep -q 'OK (2 tests)' proof/instrumentation.txt; then test_status=1; fi

collect_evidence

python tools/validate_screenshots.py proof/screenshots --required \
  01-home.png 02-settings.png 03-more-menu.png 04-private-vault.png \
  05-search.png 06-playback-speed.png 07-network-stream.png 08-cloud-import.png \
  09-video-library.png 10-system-video-picker.png 11-screen-08-subtitle-downloader.png \
  12-screen-12-technical-inspector.png 13-screen-18-history-recovery.png \
  14-equalizer-bass.png 15-sleep-timer.png 16-decoder-settings.png \
  17-vault-pin.png 18-subtitle-choices.png 19-vlc-codec-player.png \
  20-network-address.png 21-backup-restore.png 22-advanced-playback.png 23-video-trash.png 24-music-library.png 25-playback-lab.png 26-video-bookmarks.png 27-video-transform.png \
  28-repeat-mode.png 29-screen-orientation.png 30-seek-step.png 31-custom-sleep-timer.png \
  32-about-privacy.png \
  --reject-unexpected
validation_status=$?

python tools/validate_android_log.py proof/logcat.txt com.mr131.redplayer
log_status=$?

grep -Eq 'mResumedActivity.*com\.mr131\.redplayer/|topResumedActivity=.*com\.mr131\.redplayer/' proof/activities.txt
foreground_status=$?

test "$test_status" -eq 0 && test "$validation_status" -eq 0 && \
  test "$log_status" -eq 0 && test "$foreground_status" -eq 0
