$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { throw "Install GitHub CLI first" }
gh auth status
if ((git branch --show-current) -ne "main") { throw "Release only from main" }
if (git status --porcelain) { throw "Commit changes before releasing" }
if (!(Test-Path local.properties) -or !(Select-String -Path local.properties -Pattern '^storeFile=' -Quiet)) {
    throw "Configure your ORIGINAL release signing certificate in local.properties first"
}
$tag = (gh release view -R bobbyfch/idlix --json tagName --jq .tagName 2>$null)
if ($tag -match '^v(\d+)\.(\d+)\.(\d+)$') {
    $major=[int]$Matches[1]; $minor=[int]$Matches[2]; $patch=[int]$Matches[3]+1
} else { $major=1; $minor=0; $patch=0 }
if ($patch -gt 999) { throw "Patch component cannot exceed 999" }
$version="$major.$minor.$patch"
$code=1000000+$major*100000+$minor*1000+$patch
$env:IDLIX_RELEASE_VERSION=$version
$env:IDLIX_VERSION_CODE=[string]$code
& .\gradlew.bat --no-daemon testDebugUnitTest assembleRelease
if ($LASTEXITCODE -ne 0) { throw "Unit tests or release compilation failed" }
$apk="app\build\outputs\apk\release\app-release.apk"
if (!(Test-Path $apk)) { throw "No signed APK produced" }
$sdk=if ($env:ANDROID_HOME) {$env:ANDROID_HOME} else {$env:ANDROID_SDK_ROOT}
if (!$sdk) { throw "ANDROID_HOME or ANDROID_SDK_ROOT must be set for signature verification" }
$signer=Get-ChildItem (Join-Path $sdk "build-tools") -Recurse -Filter apksigner.bat |
    Sort-Object FullName -Descending | Select-Object -First 1
if (!$signer) { throw "Android SDK apksigner.bat missing" }
$proof=(& $signer.FullName verify --print-certs $apk | Out-String)
if ($LASTEXITCODE -ne 0) { throw "APK signature verification failed" }
if ($proof -notmatch '1e378630a9defaed4ac79e895f1d0367e091b38be5c7d003b56413d4d28bf4f1') {
    throw "Wrong release certificate; keeping the original update identity is mandatory"
}
gh api "repos/bobbyfch/idlix/git/ref/tags/v$version" 2>$null | Out-Null
if ($LASTEXITCODE -eq 0) { throw "Version tag already reserved, refusing duplicate" }
Copy-Item $apk "IDLIX-$version.apk"
gh release create "v$version" "IDLIX-$version.apk" -R bobbyfch/idlix --target (git rev-parse HEAD) --title "IDLIX $version" --notes "Locally built and signature-verified Android TV / touch release."
if ($LASTEXITCODE -ne 0) { throw "GitHub release publication failed" }
Remove-Item "IDLIX-$version.apk"
Write-Host "Published v$version with versionCode $code"
