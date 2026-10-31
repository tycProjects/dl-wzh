@echo off
rem Minimal Gradle wrapper launcher (see gradlew for notes).
set APP_HOME=%~dp0
set JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if not exist "%JAR%" (
  echo gradle-wrapper.jar is missing.
  echo Run once:  gradle wrapper --gradle-version 8.7   ^(or open the project in Android Studio^).
  exit /b 1
)
if defined JAVA_HOME (set JAVACMD=%JAVA_HOME%\bin\java.exe) else (set JAVACMD=java.exe)
"%JAVACMD%" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
