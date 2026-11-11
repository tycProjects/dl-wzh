#!/bin/sh
# Gradle Wrapper: запускает GradleWrapperMain из gradle/wrapper/gradle-wrapper.jar
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
if [ -n "$JAVA_HOME" ]; then JAVACMD="$JAVA_HOME/bin/java"; else JAVACMD=java; fi
exec "$JAVACMD" -Xmx64m -Dfile.encoding=UTF-8 -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
