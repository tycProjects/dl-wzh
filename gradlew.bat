@echo off
set DIRNAME=%~dp0
if "%JAVA_HOME%"=="" set JAVA_EXE=java.exe
if not "%JAVA_HOME%"=="" set JAVA_EXE=%JAVA_HOME%\bin\java.exe
"%JAVA_EXE%" -classpath "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" com.smoothcamera.wrapper.GradleWrapperMain %*
