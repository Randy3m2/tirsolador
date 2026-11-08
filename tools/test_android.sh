#!/usr/bin/env bash
# Keep diagnostics even when the instrumentation task fails.
set +e
TIRSO_ALLOW_EPHEMERAL_KEY=1 bash tools/prepare_preview_key.sh || exit 1
gradle --no-daemon -PtirsoAbis=x86_64 :app:connectedDebugAndroidTest :app:lintDebug
TEST_RESULT=$?
mkdir -p screenshots
adb logcat -d > screenshots/emulator-logcat.txt
adb pull /sdcard/Download/tirsolador-screenshots screenshots
exit "$TEST_RESULT"
