@echo off
setlocal
cd /d "%~dp0"
if not exist "scripts\create-signing-secret.ps1" (
  echo [ERROR] Run this file from the extracted IDLIX repository ZIP.
  pause
  exit /b 1
)
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\create-signing-secret.ps1"
if errorlevel 1 (
  echo.
  echo Setup belum selesai. Baca pesan error di atas.
  pause
  exit /b 1
)
echo.
echo Signing secret siap. Selanjutnya jalankan workflow Android di GitHub Actions.
pause
