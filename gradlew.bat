@echo off
setlocal
cd /d "%~dp0"
java -cp "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.BootstrapMain %*
exit /b %ERRORLEVEL%
