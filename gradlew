#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -x "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" ]; then
  exec java -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
fi
echo "Gradle wrapper JAR is not bundled. Open this project in Android Studio and let Gradle sync/download the wrapper, or install Gradle 8.7 locally." >&2
exit 1
