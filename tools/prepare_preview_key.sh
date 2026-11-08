#!/usr/bin/env bash
set -euo pipefail
umask 077
mkdir -p "$HOME/.android"
if [ ! -f "$HOME/.android/debug.keystore" ]; then
  keytool -genkeypair -keystore "$HOME/.android/debug.keystore" \
    -storepass android -keypass android -alias androiddebugkey \
    -dname 'CN=Android Debug,O=Tirsolador,C=DO' -keyalg RSA -keysize 2048 -validity 10000
fi
