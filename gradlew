#!/bin/sh
# Minimal Gradle wrapper launcher.
# NOTE: gradle/wrapper/gradle-wrapper.jar is NOT included in this archive (it is a binary that
# must come from a Gradle installation). Generate it once with:  gradle wrapper --gradle-version 8.7
# or simply open the project in Android Studio and let it sync.
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$JAR" ]; then
  echo "gradle-wrapper.jar is missing." >&2
  echo "Run once:  gradle wrapper --gradle-version 8.7   (or open the project in Android Studio)." >&2
  exit 1
fi
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then JAVACMD="$JAVA_HOME/bin/java"; else JAVACMD=java; fi
exec "$JAVACMD" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
