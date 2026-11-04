#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v gh >/dev/null || { echo "Install GitHub CLI first" >&2; exit 1; }
gh auth status >/dev/null
[[ "$(git branch --show-current)" == "main" ]] || { echo "Release only from main" >&2; exit 1; }
[[ -z "$(git status --porcelain)" ]] || { echo "Commit changes before releasing" >&2; exit 1; }
[[ -f local.properties ]] && grep -q '^storeFile=' local.properties || {
  echo "Configure ORIGINAL release signing certificate in local.properties" >&2; exit 1;
}
tag="$(gh release view -R bobbyfch/idlix --json tagName --jq .tagName 2>/dev/null || true)"
if [[ "$tag" =~ ^v([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  major="$(echo "$tag" | cut -d. -f1 | tr -d v)"
  minor="$(echo "$tag" | cut -d. -f2)"
  patch="$(echo "$tag" | cut -d. -f3)"
  patch="$((10#$patch+1))"
else
  major=1; minor=0; patch=0
fi
(( patch <= 999 )) || { echo "Patch limit exceeded" >&2; exit 1; }
version="$major.$minor.$patch"
code=$((1000000+major*100000+minor*1000+patch))
export IDLIX_RELEASE_VERSION="$version" IDLIX_VERSION_CODE="$code"
./gradlew --no-daemon testDebugUnitTest assembleRelease
apk="app/build/outputs/apk/release/app-release.apk"
test -s "$apk"
sdk="$(printenv ANDROID_HOME || printenv ANDROID_SDK_ROOT || true)"
[[ -n "$sdk" ]] || { echo "Android SDK needed to verify signing" >&2; exit 1; }
signer="$(find "$sdk/build-tools" -type f -name apksigner | sort -V | tail -1)"
[[ -x "$signer" ]] || { echo "apksigner missing" >&2; exit 1; }
proof="$("$signer" verify --print-certs "$apk")"
grep -iq '1e378630a9defaed4ac79e895f1d0367e091b38be5c7d003b56413d4d28bf4f1' <<< "$proof" || {
  echo "Certificate mismatch: this APK cannot update the official app" >&2; exit 1;
}
if gh api "repos/bobbyfch/idlix/git/ref/tags/v$version" >/dev/null 2>&1; then
  echo "Tag collision: refusing duplicate" >&2; exit 1
fi
cp "$apk" "IDLIX-$version.apk"
gh release create "v$version" "IDLIX-$version.apk" -R bobbyfch/idlix \
  --target "$(git rev-parse HEAD)" --title "IDLIX $version" \
  --notes "Locally built and signature-verified Android TV/touch release."
rm "IDLIX-$version.apk"
echo "Published v$version, versionCode=$code"
