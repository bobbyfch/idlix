#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v gh >/dev/null || { echo "Install GitHub CLI first" >&2; exit 1; }
gh auth status >/dev/null
[[ "$(git branch --show-current)" == "main" ]] || { echo "Release only from main" >&2; exit 1; }
[[ -z "$(git status --porcelain)" ]] || { echo "Commit changes before releasing" >&2; exit 1; }
[[ -f local.properties ]] || { echo "Configure release signing in local.properties" >&2; exit 1; }

store_file="$(sed -n 's/^storeFile=//p' local.properties | head -n1)"
store_password="$(sed -n 's/^storePassword=//p' local.properties | head -n1)"
key_alias="$(sed -n 's/^keyAlias=//p' local.properties | head -n1)"
[[ -n "$store_file" && -n "$store_password" && -n "$key_alias" ]] || {
  echo "local.properties must contain storeFile, storePassword, and keyAlias" >&2; exit 1;
}
if [[ "$store_file" != /* ]]; then store_file="$PWD/$store_file"; fi
[[ -f "$store_file" ]] || { echo "Signing keystore not found: $store_file" >&2; exit 1; }

tag="$(gh release view -R bobbyfch/idlix --json tagName --jq .tagName 2>/dev/null || true)"
if [[ "$tag" =~ ^v([0-9]+)[.]([0-9]+)[.]([0-9]+)$ ]]; then
  major="$(echo "$tag" | cut -d. -f1 | tr -d v)"
  minor="$(echo "$tag" | cut -d. -f2)"
  patch="$(echo "$tag" | cut -d. -f3)"
  patch="$((10#$patch+1))"
else
  major=1; minor=0; patch=0
fi
(( patch <= 999 )) || { echo "Patch limit exceeded" >&2; exit 1; }
version="$major.$minor.$patch"
latest_run="$(gh run list --repo bobbyfch/idlix --workflow android-apk.yml --branch main --limit 1 --json number --jq '.[0].number // 0')" || {
  echo "Could not read the latest Android Actions run number" >&2; exit 1;
}
[[ "$latest_run" =~ ^[0-9]+$ ]] || { echo "Invalid Android Actions run number" >&2; exit 1; }
next_run=$((latest_run+1))
code=$((1200000+next_run*1000+patch))
export IDLIX_RELEASE_VERSION="$version" IDLIX_VERSION_CODE="$code"
./gradlew --no-daemon testDebugUnitTest assembleRelease
apk="app/build/outputs/apk/release/app-release.apk"
test -s "$apk"
sdk="$(printenv ANDROID_HOME || printenv ANDROID_SDK_ROOT || true)"
[[ -n "$sdk" ]] || { echo "Android SDK needed to verify signing" >&2; exit 1; }
signer="$(find "$sdk/build-tools" -type f -name apksigner | sort -V | tail -1)"
[[ -x "$signer" ]] || { echo "apksigner missing" >&2; exit 1; }
proof="$("$signer" verify --print-certs "$apk")"
apk_fingerprint="$(sed -n 's/^Signer #1 certificate SHA-256 digest: *//p' <<< "$proof" | head -n1 | tr -d ': ' | tr '[:lower:]' '[:upper:]')"
[[ "$apk_fingerprint" =~ ^[0-9A-F]{64}$ ]] || { echo "APK signature verification failed" >&2; exit 1; }
export IDLIX_STORE_PASSWORD="$store_password"
key_info="$(keytool -list -v -keystore "$store_file" -storetype JKS -alias "$key_alias" -storepass:env IDLIX_STORE_PASSWORD)"
unset IDLIX_STORE_PASSWORD
key_fingerprint="$(sed -n 's/^ *SHA256: *//p' <<< "$key_info" | head -n1 | tr -d ': ' | tr '[:lower:]' '[:upper:]')"
[[ "$key_fingerprint" =~ ^[0-9A-F]{64}$ && "$apk_fingerprint" == "$key_fingerprint" ]] || {
  echo "APK signer does not match the key configured in local.properties" >&2; exit 1;
}

if gh api "repos/bobbyfch/idlix/git/ref/tags/v$version" >/dev/null 2>&1; then
  echo "Tag collision: refusing duplicate" >&2; exit 1
fi
cp "$apk" "IDLIX-$version.apk"
notes="Locally built and signature-verified Android TV/touch release."
notes+=$'\n\n<!-- IDLIX_VERSION_CODE='"$code"' -->'
gh release create "v$version" "IDLIX-$version.apk" -R bobbyfch/idlix \
  --target "$(git rev-parse HEAD)" --title "IDLIX $version" --notes "$notes"
rm "IDLIX-$version.apk"
echo "Published v$version, versionCode=$code"
