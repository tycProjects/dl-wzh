@echo off
set GRADLE_VERSION=8.2.1
set GRADLE_HOME=%USERPROFILE%\.gradle\manual-dists\gradle-%GRADLE_VERSION%\gradle-%GRADLE_VERSION%
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  echo Please install Gradle %GRADLE_VERSION% or run this project in an environment with curl and unzip support.
  exit /b 1
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
