@echo off
setlocal
set DIR=%~dp0
set VER=8.7
set DIST=%DIR%.gradle-bootstrap\gradle-%VER%
set ZIP=%DIR%.gradle-bootstrap\gradle-%VER%-bin.zip
if not exist "%DIST%\bin\gradle.bat" (
  if not exist "%DIR%.gradle-bootstrap" mkdir "%DIR%.gradle-bootstrap"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%VER%-bin.zip' -OutFile '%ZIP%'; Expand-Archive -Force '%ZIP%' '%DIR%.gradle-bootstrap'"
)
call "%DIST%\bin\gradle.bat" %*
