param(
    [string]$Repository = "bobbyfch/idlix",
    [switch]$ReuseExisting,
    [switch]$ReplaceExistingSecret
)

$ErrorActionPreference = "Stop"

if ($Repository -notmatch '^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$') {
    throw "Repository must use owner/name format."
}

$keytool = Get-Command keytool -ErrorAction Stop
$gh = Get-Command gh -ErrorAction SilentlyContinue
$useGh = $false

if ($null -ne $gh) {
    & $gh.Source auth status --hostname github.com *> $null
    if ($LASTEXITCODE -eq 0) {
        $useGh = $true
    }
    else {
        Write-Warning "GitHub CLI is not logged in. The script will use the GitHub browser page instead."
    }
}
else {
    Write-Host "GitHub CLI was not found. The script will use the GitHub browser page instead."
}

$secretNames = @()
if ($useGh) {
    $secretNames = & $gh.Source secret list --repo $Repository --app actions
    if ($LASTEXITCODE -ne 0) {
        $useGh = $false
        $secretNames = @()
        Write-Warning "GitHub CLI cannot manage this repository's secrets. The script will use the GitHub browser page instead."
    }
}
$signingSecretExists = $useGh -and [bool]($secretNames | Where-Object { $_ -match '^\s*IDLIX_SIGNING_BUNDLE\s' })
if ($signingSecretExists -and -not ($ReplaceExistingSecret -or $ReuseExisting)) {
    throw "IDLIX_SIGNING_BUNDLE already exists. Use -ReplaceExistingSecret only if you intend to rotate the signing identity."
}

if ($ReplaceExistingSecret -and $signingSecretExists) {
    $confirmation = Read-Host "Replacing this secret changes the signing identity. Type ROTATE-SECRET to continue"
    if ($confirmation -cne "ROTATE-SECRET") {
        throw "Cancelled. Existing secret was not changed."
    }
}

if ($ReuseExisting) {
    $confirmation = Read-Host "Reuse the local key and password saved by this script. Type REUSE-FRESH to continue"
    if ($confirmation -cne "REUSE-FRESH") {
        throw "Cancelled. No GitHub secret was changed."
    }
}
else {
    $confirmation = Read-Host "A new signing identity requires uninstalling the old IDLIX app. Type CREATE-FRESH to continue"
    if ($confirmation -cne "CREATE-FRESH") {
        throw "Cancelled. No key or GitHub secret was created."
    }
}

$signingDirectory = Join-Path $env:USERPROFILE ".idlix"
New-Item -ItemType Directory -Path $signingDirectory -Force | Out-Null
$keyPath = Join-Path $signingDirectory "release-signing.jks"
$propertiesPath = Join-Path (Get-Location).Path "local.properties"
$forwardKeyPath = $keyPath.Replace("\", "/")
$existingLines = @()
if (Test-Path -LiteralPath $propertiesPath) {
    $existingLines = [IO.File]::ReadAllLines($propertiesPath)
}
$properties = @{}
foreach ($line in $existingLines) {
    if ($line -match '^\s*([^#\s][^=]*)\s*=\s*(.*?)\s*$') {
        $properties[$matches[1].Trim()] = $matches[2]
    }
}

if ($ReuseExisting) {
    if (-not (Test-Path -LiteralPath $keyPath)) {
        throw "No local key exists at $keyPath."
    }
    if ($properties["storeFile"] -ne $forwardKeyPath -or
        $properties["keyAlias"] -ne "idlix" -or
        $properties["storePassword"] -notmatch '^[A-Za-z0-9_-]{32,}$' -or
        $properties["keyPassword"] -ne $properties["storePassword"]) {
        throw "local.properties does not contain this script's saved signing configuration."
    }
    $password = $properties["storePassword"]
    $env:IDLIX_KEY_PASSWORD = $password
    try {
        & $keytool.Source -list -keystore $keyPath -storetype JKS -alias idlix -storepass:env IDLIX_KEY_PASSWORD *> $null
        if ($LASTEXITCODE -ne 0) {
            throw "The local signing key could not be opened with its saved password."
        }
    }
    finally {
        Remove-Item Env:IDLIX_KEY_PASSWORD -ErrorAction SilentlyContinue
    }
}
else {
    if (Test-Path -LiteralPath $keyPath) {
        throw "A signing key already exists at $keyPath. To retry uploading the same key, run this script with -ReuseExisting."
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
}

$keystoreBytes = [IO.File]::ReadAllBytes($keyPath)
$bundleObject = @{
    alias = "idlix"
    password = $password
    keystore_b64 = [Convert]::ToBase64String($keystoreBytes)
}
$bundleJson = $bundleObject | ConvertTo-Json -Compress
$bundle = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($bundleJson))

if ($useGh) {
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
        throw "GitHub CLI could not save IDLIX_SIGNING_BUNDLE. The key remains at $keyPath; retry with -ReuseExisting."
    }
}
else {
    $settingsUrl = "https://github.com/$Repository/settings/secrets/actions"
    Set-Clipboard -Value $bundle
    try {
        Start-Process $settingsUrl
        Write-Host "Secret sudah disalin ke clipboard dan halaman Actions secrets dibuka."
        Write-Host "Di GitHub, tambahkan atau perbarui secret bernama IDLIX_SIGNING_BUNDLE, tempel nilainya, lalu klik Add secret."
        $confirmation = Read-Host "Ketik SECRET-SAVED setelah secret tersimpan di GitHub"
        if ($confirmation -cne "SECRET-SAVED") {
            throw "Secret belum dikonfirmasi tersimpan. Key tetap aman di $keyPath; jalankan ulang dengan -ReuseExisting."
        }
    }
    finally {
        Set-Clipboard -Value ""
    }
}

Write-Host "Signing key disiapkan untuk $Repository."
Write-Host "Local release signing is ready through local.properties."
Write-Host "Keep this key safe: $keyPath"
