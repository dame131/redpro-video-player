#!/usr/bin/env bash
set -uo pipefail
mkdir -p proof/screenshots proof/reports proof/device-state
timeout 60s adb wait-for-device || exit 2
boot_ready=0
for attempt in $(seq 1 180); do
  completed=$(timeout 5s adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
  animation=$(timeout 5s adb shell getprop init.svc.bootanim 2>/dev/null | tr -d '\r')
  if [ "$completed" = "1" ] && [ "$animation" = "stopped" ] && \
     timeout 5s adb shell service check activity 2>/dev/null | grep -q 'found' && \
     timeout 5s adb shell cmd package path com.android.systemui >/dev/null 2>&1; then
    boot_ready=1
    break
  fi
  sleep 1
done
test "$boot_ready" -eq 1 || exit 3
timeout 10s adb shell input keyevent 82 || true
timeout 10s adb shell settings put global window_animation_scale 0
timeout 10s adb shell settings put global transition_animation_scale 0
timeout 10s adb shell settings put global animator_duration_scale 0
timeout 10s adb logcat -c
timeout 15s adb shell getprop > proof/device-state/properties.txt
timeout 15s adb shell wm size > proof/device-state/display.txt
timeout 15s adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER com.mr131.redplayer \
  > proof/device-state/launcher-activity.txt

collect_evidence() {
  screenshot_dir=/sdcard/Android/data/com.mr131.redplayer/files/screenshots
  if ! timeout 30s adb pull "$screenshot_dir/." proof/screenshots/ \
      > proof/pull-screenshots.txt 2>&1; then
    # Android 11 blocks shell from Android/data, but this debug app is safely
    # readable through its own UID. Stream each PNG without weakening storage.
    timeout 20s adb exec-out run-as com.mr131.redplayer ls -1 "$screenshot_dir" \
      > proof/screenshot-files.txt 2>> proof/pull-screenshots.txt || true
    while IFS= read -r screenshot; do
      case "$screenshot" in
        *.png)
          if timeout 30s adb exec-out run-as com.mr131.redplayer cat "$screenshot_dir/$screenshot" \
              > "proof/screenshots/$screenshot"; then
            test -s "proof/screenshots/$screenshot" || rm -f "proof/screenshots/$screenshot"
          else
            rm -f "proof/screenshots/$screenshot"
          fi
          ;;
      esac
    done < proof/screenshot-files.txt
  fi
  cp -R app/build/reports/androidTests proof/reports/ 2>/dev/null || true
  cp -R app/build/outputs/androidTest-results proof/reports/ 2>/dev/null || true
  timeout 10s adb shell uiautomator dump /sdcard/current-window.xml >/dev/null 2>&1 || true
  timeout 10s adb pull /sdcard/current-window.xml proof/ >/dev/null 2>&1 || true
  timeout 20s adb logcat -d -v threadtime > proof/logcat.txt || true
  timeout 20s adb shell dumpsys activity activities > proof/activities.txt || true
  timeout 20s adb shell dumpsys window windows > proof/windows.txt || true
}
trap collect_evidence EXIT

wait_for_android_ready() {
  local ready=0
  timeout 10s adb shell input keyevent KEYCODE_HOME || true
  for attempt in $(seq 1 120); do
    if timeout 5s adb shell service check activity 2>/dev/null | grep -q 'found' && \
       timeout 5s adb shell cmd package list packages android 2>/dev/null | grep -q '^package:android$'; then
      ready=1
      break
    fi
    sleep 1
  done
  test "$ready" -eq 1 || return 1
  timeout 15s adb shell uiautomator dump /sdcard/system-ready.xml >/dev/null 2>&1 || return 1
  timeout 15s adb pull /sdcard/system-ready.xml proof/device-state/system-ready.xml >/dev/null 2>&1 || return 1
  local current_dump=proof/device-state/system-ready.xml
  local anr_count=0
  local clean_count=0
  # A cold CI install can queue separate system_server and System UI warnings.
  # Recover at most three real Wait dialogs, then demand two consecutive clean checks.
  for recovery_check in 1 2 3 4 5 6; do
    if grep -q 'android:id/aerr_wait' "$current_dump"; then
      anr_count=$((anr_count + 1))
      test "$anr_count" -le 3 || return 1
      read -r wait_x wait_y < <(python tools/anr_wait_coordinates.py "$current_dump") || return 1
      timeout 10s adb shell input tap "$wait_x" "$wait_y" || return 1
      clean_count=0
      sleep 15
    else
      timeout 5s adb shell service check activity 2>/dev/null | grep -q 'found' || return 1
      clean_count=$((clean_count + 1))
      test "$clean_count" -ge 2 && return 0
      sleep 10
    fi
    current_dump=proof/device-state/system-recovered-${recovery_check}.xml
    timeout 15s adb shell uiautomator dump /sdcard/system-recovered.xml >/dev/null 2>&1 || return 1
    timeout 15s adb pull /sdcard/system-recovered.xml "$current_dump" >/dev/null 2>&1 || return 1
  done
  return 1
}

install_apk() {
  local apk="$1"
  for attempt in 1 2 3 4 5; do
    if timeout 120s adb install -r -t "$apk"; then return 0; fi
    sleep 5
  done
  return 1
}
# Backup restore competes with Package Manager and can starve a cold API-30 emulator.
timeout 15s adb shell bmgr enable false || true
timeout 15s adb shell settings put secure backup_enabled 0 || true
install_apk app/build/outputs/apk/debug/app-debug.apk 2>&1 | tee proof/install-app.txt
app_install_status=${PIPESTATUS[0]}
test "$app_install_status" -eq 0 || exit 11
install_apk app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk 2>&1 | tee proof/install-test-app.txt
test_install_status=${PIPESTATUS[0]}
test "$test_install_status" -eq 0 || exit 12
timeout 15s adb shell pm path com.mr131.redplayer | grep -q '^package:' || exit 13
timeout 15s adb shell pm grant com.mr131.redplayer android.permission.READ_EXTERNAL_STORAGE || true
# Do not compete with the cold image's BOOT_COMPLETED and package-install receivers.
if ! timeout 180s adb shell am wait-for-broadcast-idle; then
  timeout 180s adb shell cmd activity wait-for-broadcast-idle || exit 14
fi
wait_for_android_ready || exit 14

# Prove that the installed launcher activity starts and reaches its unique home marker.
timeout 15s adb shell am force-stop com.mr131.redplayer
timeout 30s adb shell am start -S -n com.mr131.redplayer/.MainActivity | tee proof/launcher.txt
launcher_status=${PIPESTATUS[0]}
test "$launcher_status" -eq 0 || exit 15
launcher_drawn=0
for attempt in $(seq 1 30); do
  timeout 20s adb shell dumpsys activity activities > proof/device-state/launcher-activities.txt || exit 17
  if grep -Eq 'mResumedActivity.*com\.mr131\.redplayer/.MainActivity|topResumedActivity=.*com\.mr131\.redplayer/.MainActivity' \
      proof/device-state/launcher-activities.txt && \
     grep -q 'reportedDrawn=true' proof/device-state/launcher-activities.txt; then
    launcher_drawn=1
    break
  fi
  sleep 1
done
test "$launcher_drawn" -eq 1 || exit 18

# ScreenTourTest asserts the unique “131 Red Player Home Ready” marker in the
# real launched activity before it is allowed to capture screenshot 01.
timeout 15m adb shell am instrument -w -r \
  com.mr131.redplayer.test/androidx.test.runner.AndroidJUnitRunner | tee proof/instrumentation.txt
test_status=${PIPESTATUS[0]}
if ! grep -q 'OK (2 tests)' proof/instrumentation.txt; then test_status=1; fi

# AndroidJUnitRunner stops the target package after a successful suite. Relaunch
# the real app so the final device-state evidence also proves it is foreground.
if test "$test_status" -eq 0; then
  timeout 30s adb shell am start -W -n com.mr131.redplayer/.MainActivity \
    > proof/post-test-launcher.txt || test_status=1
  post_test_foreground=0
  for attempt in $(seq 1 30); do
    timeout 20s adb shell dumpsys activity activities > proof/device-state/post-test-activities.txt || true
    if grep -Eq 'mResumedActivity.*com\.mr131\.redplayer/.MainActivity|topResumedActivity=.*com\.mr131\.redplayer/.MainActivity' \
        proof/device-state/post-test-activities.txt && \
       grep -q 'reportedDrawn=true' proof/device-state/post-test-activities.txt; then
      post_test_foreground=1
      break
    fi
    sleep 1
  done
  test "$post_test_foreground" -eq 1 || test_status=1
fi

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
