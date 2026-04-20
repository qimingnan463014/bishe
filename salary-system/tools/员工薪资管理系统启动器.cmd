@echo off
setlocal
title Salary System Launcher
echo Starting salary-system...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "E:\bishe\salary-system\tools\start-salary-system.ps1"
if errorlevel 1 (
  echo.
  echo Launcher failed. Press any key to close this window.
  pause >nul
)
endlocal
