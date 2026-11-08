#!/usr/bin/env bash
set -euo pipefail
adb install old/*.apk
adb shell run-as com.tirso.tirsolador.preview sh -c '"mkdir -p files; echo preserved > files/upgrade-marker"'
adb install -r dist/Tirsolador-v1.0.0.apk
adb root
adb wait-for-device
result=$(adb shell cat /data/user/0/com.tirso.tirsolador.preview/files/upgrade-marker | tr -d '\r\n')
[ "$result" = preserved ]
adb shell am start -W -n com.tirso.tirsolador.preview/com.tirso.tirsolador.MainActivity
sleep 3
adb shell pidof com.tirso.tirsolador.preview
echo 'Existing installation updated successfully; private data preserved.'
