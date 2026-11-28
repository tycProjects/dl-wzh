@echo off
setlocal
set GRADLE_VERSION=8.9
set BASE=%USERPROFILE%\.gradle\filemax-bootstrap
set DIST=%BASE%\gradle-%GRADLE_VERSION%
if not exist "%DIST%\bin\gradle.bat" (
  if not exist "%BASE%" mkdir "%BASE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$u='https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip'; $z='%BASE%\gradle-%GRADLE_VERSION%-bin.zip'; Invoke-WebRequest $u -OutFile $z; Expand-Archive -Force $z '%BASE%'"
)
call "%DIST%\bin\gradle.bat" -p "%~dp0" %*
endlocal
