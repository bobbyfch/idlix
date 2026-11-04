$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { throw "Install GitHub CLI first" }
gh auth status
if ((git branch --show-current) -ne "main") { throw "Release only from main" }
if (git status --porcelain) { throw "Commit changes before releasing" }

$propertiesPath = Join-Path (Get-Location) "local.properties"
if (!(Test-Path -LiteralPath $propertiesPath)) { throw "Configure release signing in local.properties first" }
$properties = @{}
foreach ($line in Get-Content -LiteralPath $propertiesPath) {
    if ($line -match '^[ ]*([^#! ]+)[=:][ ]*(.*)$') {
        $properties[$matches[1].Trim()] = $matches[2]
    }
}
$storeFilePath = $properties["storeFile"]
$storePassword = $properties["storePassword"]
$keyAlias = $properties["keyAlias"]
if (!$storeFilePath -or !$storePassword -or !$keyAlias) {
    throw "local.properties must contain storeFile, storePassword, and keyAlias"
}
if ([IO.Path]::IsPathRooted($storeFilePath)) {
    $keystorePath = $storeFilePath
} else {
    $keystorePath = Join-Path (Get-Location) $storeFilePath
}
if (!(Test-Path -LiteralPath $keystorePath)) { throw "Signing keystore not found: $keystorePath" }

$tag = (gh release view -R bobbyfch/idlix --json tagName --jq .tagName 2>$null)
if ($tag -match '^v([0-9]+)[.]([0-9]+)[.]([0-9]+)$') {
    $major=[int]$Matches[1]; $minor=[int]$Matches[2]; $patch=[int]$Matches[3]+1
} else { $major=1; $minor=0; $patch=0 }
if ($patch -gt 999) { throw "Patch component cannot exceed 999" }
$version="$major.$minor.$patch"
$latestRun = gh run list --repo bobbyfch/idlix --workflow android-apk.yml --branch main --limit 1 --json number --jq '.[0].number // 0'
if ($LASTEXITCODE -ne 0 -or $latestRun -notmatch '^[0-9]+$') { throw "Could not read the latest Android Actions run number" }
$code=1200000+(([int]$latestRun+1)*1000)+$patch
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
$signerMatch=[regex]::Match($proof,'(?m)^Signer #1 certificate SHA-256 digest:[ ]*([A-Fa-f0-9:]+)[ ]*$')
if (!$signerMatch.Success) { throw "Could not read the APK signing certificate fingerprint" }
$apkFingerprint=($signerMatch.Groups[1].Value -replace '[^A-Fa-f0-9]','').ToUpperInvariant()

$env:IDLIX_STORE_PASSWORD=$storePassword
try {
    $keyInfo=(& keytool -list -v -keystore $keystorePath -storetype JKS -alias $keyAlias -storepass:env IDLIX_STORE_PASSWORD 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) { throw "Could not read the certificate from the configured release key" }
} finally {
    Remove-Item Env:IDLIX_STORE_PASSWORD -ErrorAction SilentlyContinue
}
$keyMatch=[regex]::Match($keyInfo,'(?m)^[ ]*SHA256:[ ]*([A-Fa-f0-9:]+)[ ]*$')
if (!$keyMatch.Success) { throw "Could not read the configured release key fingerprint" }
$keyFingerprint=($keyMatch.Groups[1].Value -replace '[^A-Fa-f0-9]','').ToUpperInvariant()
if ($apkFingerprint -ne $keyFingerprint) {
    throw "APK signer does not match the key configured in local.properties"
}

gh api "repos/bobbyfch/idlix/git/ref/tags/v$version" 2>$null | Out-Null
if ($LASTEXITCODE -eq 0) { throw "Version tag already reserved, refusing duplicate" }
Copy-Item $apk "IDLIX-$version.apk"
$notes="Locally built and signature-verified Android TV / touch release." + [Environment]::NewLine + [Environment]::NewLine + "<!-- IDLIX_VERSION_CODE=$code -->"
gh release create "v$version" "IDLIX-$version.apk" -R bobbyfch/idlix --target (git rev-parse HEAD) --title "IDLIX $version" --notes $notes
if ($LASTEXITCODE -ne 0) { throw "GitHub release publication failed" }
Remove-Item "IDLIX-$version.apk"
Write-Host "Published v$version with versionCode $code"
