@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

if not exist "gradlew.bat" (
  echo [ERROR] Gradle wrapper not found. Run this from the repository.
  exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java 21 is required. Set JAVA_HOME to a JDK 21 installation.
  exit /b 1
)

if /I "%~1"=="release" (
  echo [IDLIX TV] Testing and building a locally signed release...
  call gradlew.bat --no-daemon testDebugUnitTest assembleRelease
  if errorlevel 1 exit /b 1
  echo [OK] Check app\build\outputs\apk\release\
  exit /b 0
)

echo [IDLIX TV] Testing and building a local debug APK...
call gradlew.bat --no-daemon testDebugUnitTest assembleDebug
if errorlevel 1 exit /b 1
echo [OK] app\build\outputs\apk\debug\app-debug.apk

if /I not "%~1"=="run" exit /b 0

set "ADB=adb"
if defined ANDROID_HOME if exist "%ANDROID_HOME%\platform-tools\adb.exe" set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
if defined ANDROID_SDK_ROOT if exist "%ANDROID_SDK_ROOT%\platform-tools\adb.exe" set "ADB=%ANDROID_SDK_ROOT%\platform-tools\adb.exe"
"%ADB%" devices
if errorlevel 1 (
  echo [ERROR] adb was not found. Install Android SDK Platform Tools.
  exit /b 1
)
echo [IDLIX TV] Installing preview build on the connected Android TV...
"%ADB%" install -r "app\build\outputs\apk\debug\app-debug.apk"
if errorlevel 1 exit /b 1
"%ADB%" shell am start -n com.idlix.tv.debug/com.idlix.tv.MainActivity
if errorlevel 1 exit /b 1
echo [OK] App launched.
