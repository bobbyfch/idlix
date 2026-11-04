#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ ! -f ./gradlew ]]; then
  echo "[ERROR] Gradle wrapper missing" >&2
  exit 1
fi
chmod +x ./gradlew
if [[ "${1:-}" == "release" ]]; then
  ./gradlew --no-daemon testDebugUnitTest assembleRelease
  echo "[OK] Locally signed release: app/build/outputs/apk/release/"
  exit
fi
./gradlew --no-daemon testDebugUnitTest assembleDebug
echo "[OK] app/build/outputs/apk/debug/app-debug.apk"
if [[ "${1:-}" == "run" ]]; then
  if [[ -n "${ANDROID_HOME:-}" && -x "${ANDROID_HOME}/platform-tools/adb" ]]; then
    adb="${ANDROID_HOME}/platform-tools/adb"
  elif [[ -n "${ANDROID_SDK_ROOT:-}" && -x "${ANDROID_SDK_ROOT}/platform-tools/adb" ]]; then
    adb="${ANDROID_SDK_ROOT}/platform-tools/adb"
  else
    adb=adb
  fi
  "$adb" devices
  "$adb" install -r app/build/outputs/apk/debug/app-debug.apk
  "$adb" shell am start -n com.idlix.tv.debug/com.idlix.tv.MainActivity
fi
