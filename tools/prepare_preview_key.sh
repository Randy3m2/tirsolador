#!/usr/bin/env bash
set -euo pipefail
umask 077
mkdir -p "$HOME/.android"
if [ -n "${TIRSO_SIGNING_KEY_B64:-}" ]; then
  printf '%s' "$TIRSO_SIGNING_KEY_B64" | base64 --decode > "$HOME/.android/debug.keystore"
fi
if [ ! -f "$HOME/.android/debug.keystore" ]; then
  if [ "${CI:-}" = true ] && [ "${TIRSO_ALLOW_EPHEMERAL_KEY:-}" != 1 ]; then
    echo 'Signing key missing. Restore the existing key using TIRSO_SIGNING_KEY_B64; refusing to break installed updates.' >&2
    exit 1
  fi
  keytool -genkeypair -keystore "$HOME/.android/debug.keystore" -storepass android -keypass android -alias androiddebugkey -dname 'CN=Android Debug,O=Tirsolador,C=DO' -keyalg RSA -keysize 2048 -validity 10000
fi
if [ "${TIRSO_ALLOW_EPHEMERAL_KEY:-}" != 1 ] && [ "${CI:-}" = true ]; then
  actual=$(keytool -exportcert -keystore "$HOME/.android/debug.keystore" -storepass android -alias androiddebugkey | sha256sum | cut -d ' ' -f 1)
  expected=$(tr -d '\r\n' < config/signing-cert.sha256)
  [ "$actual" = "$expected" ] || { echo 'Signing certificate mismatch: refusing to publish an incompatible update.' >&2; exit 1; }
fi
