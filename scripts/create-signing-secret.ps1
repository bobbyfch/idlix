param(
    [string]$Repository = "bobbyfch/idlix"
)

$ErrorActionPreference = "Stop"

if ($Repository -notmatch '^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$') {
    throw "Repository must use owner/name format."
}

$keytool = Get-Command keytool -ErrorAction Stop
$gh = Get-Command gh -ErrorAction Stop

& $gh.Source auth status --hostname github.com *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Sign in to GitHub CLI first by running: gh auth login"
}

$confirmation = Read-Host "A new signing identity requires uninstalling the old IDLIX app. Type CREATE-FRESH to continue"
if ($confirmation -cne "CREATE-FRESH") {
    throw "Cancelled. No key or GitHub secret was created."
}

$signingDirectory = Join-Path $env:USERPROFILE ".idlix"
New-Item -ItemType Directory -Path $signingDirectory -Force | Out-Null
$keyPath = Join-Path $signingDirectory "release-signing.jks"
$propertiesPath = Join-Path (Get-Location).Path "local.properties"
$forwardKeyPath = $keyPath.Replace("\", "/")

if (Test-Path -LiteralPath $keyPath) {
    throw "A signing key already exists at $keyPath. Do not overwrite it. Remove it only if you have not published a release and intend to discard that key."
}

$passwordBytes = New-Object byte[] 48
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($passwordBytes)
}
finally {
    $rng.Dispose()
}
$password = [Convert]::ToBase64String($passwordBytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")

$env:IDLIX_KEY_PASSWORD = $password
try {
    & $keytool.Source -genkeypair -noprompt -keystore $keyPath -storetype JKS -alias idlix -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=IDLIX Release" -storepass:env IDLIX_KEY_PASSWORD -keypass:env IDLIX_KEY_PASSWORD
    if ($LASTEXITCODE -ne 0) {
        throw "keytool could not create the signing key."
    }
}
finally {
    Remove-Item Env:IDLIX_KEY_PASSWORD -ErrorAction SilentlyContinue
}

$existingLines = @()
if (Test-Path -LiteralPath $propertiesPath) {
    $existingLines = [IO.File]::ReadAllLines($propertiesPath)
}
$keptLines = @($existingLines | Where-Object {
    $_ -notmatch '^\s*(storeFile|storePassword|keyAlias|keyPassword)\s*='
})
$newLines = @($keptLines) + @(
    "storeFile=$forwardKeyPath",
    "storePassword=$password",
    "keyAlias=idlix",
    "keyPassword=$password"
)
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllLines($propertiesPath, $newLines, $utf8NoBom)

$keystoreBytes = [IO.File]::ReadAllBytes($keyPath)
$bundleObject = @{
    alias = "idlix"
    password = $password
    keystore_b64 = [Convert]::ToBase64String($keystoreBytes)
}
$bundleJson = $bundleObject | ConvertTo-Json -Compress
$bundle = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($bundleJson))

$startInfo = New-Object Diagnostics.ProcessStartInfo
$startInfo.FileName = $gh.Source
$startInfo.Arguments = "secret set IDLIX_SIGNING_BUNDLE --repo $Repository"
$startInfo.UseShellExecute = $false
$startInfo.RedirectStandardInput = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true

$process = [Diagnostics.Process]::Start($startInfo)
$stdoutTask = $process.StandardOutput.ReadToEndAsync()
$stderrTask = $process.StandardError.ReadToEndAsync()
$process.StandardInput.Write($bundle)
$process.StandardInput.Close()
$process.WaitForExit()
$stdout = $stdoutTask.GetAwaiter().GetResult()
$stderr = $stderrTask.GetAwaiter().GetResult()

if ($process.ExitCode -ne 0) {
    if ($stderr) {
        Write-Error $stderr
    }
    throw "GitHub CLI could not save IDLIX_SIGNING_BUNDLE. The key remains at $keyPath; do not delete it if the secret upload might have succeeded."
}

Write-Host "Fresh IDLIX_SIGNING_BUNDLE was saved to $Repository."
Write-Host "Local release signing is ready through local.properties."
Write-Host "Keep this key safe: $keyPath"
