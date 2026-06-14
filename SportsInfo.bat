@echo off
title SportsInfo
setlocal enabledelayedexpansion

REM SportsInfo launcher - double-click to start the server and open the app.
REM GEMINI_API_KEY is inherited from your user environment variable (setx).

set "JAVA_HOME=C:\Users\minju\java\jdk-21.0.11+10"
set "ROOT=%~dp0"

REM 1) Build the web UI once if it has not been built yet.
if not exist "%ROOT%backend\src\main\resources\static\index.html" (
  echo [SportsInfo] Building the web UI for the first time...
  pushd "%ROOT%frontend"
  call npm install
  call npm run build
  popd
)

REM 2) Start the backend in a minimized window (close it to stop the server).
echo [SportsInfo] Starting server...
pushd "%ROOT%backend"
start "SportsInfo Server" /min cmd /c "gradlew.bat bootRun"
popd

REM 3) Wait until the server responds.
echo [SportsInfo] Waiting for the server to come up...
powershell -NoProfile -Command "$end=(Get-Date).AddSeconds(150); while((Get-Date) -lt $end){ try{ Invoke-WebRequest 'http://localhost:8080/api/games' -UseBasicParsing -TimeoutSec 2 | Out-Null; exit 0 }catch{ Start-Sleep -Seconds 2 } }; exit 1"

REM 4) Open Chrome in app mode (fallback to default browser).
set "CHROME="
if exist "%ProgramFiles%\Google\Chrome\Application\chrome.exe" set "CHROME=%ProgramFiles%\Google\Chrome\Application\chrome.exe"
if exist "%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe" set "CHROME=%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe"
if defined CHROME (
  start "" "!CHROME!" --app=http://localhost:8080 --window-size=1000,800
) else (
  start "" http://localhost:8080
)

echo [SportsInfo] Opened. You can close this window.
ping -n 3 127.0.0.1 >nul
exit
