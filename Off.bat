@echo off
title SportsInfo - OFF
echo [OFF] Stopping the SportsInfo server (port 8080)...

powershell -NoProfile -Command "$c = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue; if ($c) { $c | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force } ; Write-Host '[OFF] Server stopped.' } else { Write-Host '[OFF] Server was not running.' }"

ping -n 3 127.0.0.1 >nul
exit
