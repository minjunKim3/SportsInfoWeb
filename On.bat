@echo off
title SportsInfo - ON
setlocal

REM Turn the SportsInfo server ON (http://localhost:8080).
REM GEMINI_API_KEY is inherited from your user environment variable (setx).

set "JAVA_HOME=C:\Users\minju\java\jdk-21.0.11+10"
set "ROOT=%~dp0"

REM Build the web UI once if it is missing.
if not exist "%ROOT%backend\src\main\resources\static\index.html" (
  echo [ON] Building the web UI for the first time...
  pushd "%ROOT%frontend"
  call npm install
  call npm run build
  popd
)

REM If already running, don't start twice.
powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }"
if %errorlevel%==0 (
  echo [ON] Server is already running at http://localhost:8080
  ping -n 3 127.0.0.1 >nul
  exit /b
)

echo [ON] Starting the server in a minimized window...
pushd "%ROOT%backend"
start "SportsInfo Server" /min cmd /c "gradlew.bat bootRun"
popd

echo [ON] Done. Open http://localhost:8080 in a few seconds.
echo      To stop the server later, run Off.bat
ping -n 3 127.0.0.1 >nul
exit
