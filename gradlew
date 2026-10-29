#!/bin/sh
# Gradle Wrapper launcher. The wrapper JAR must be present in gradle/wrapper/.
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P) || exit
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$CLASSPATH" ]; then
  echo "Gradle wrapper JAR is missing. Open this project in Android Studio and sync, or generate wrapper files with: gradle wrapper --gradle-version 8.9" >&2
  exit 1
fi
exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
