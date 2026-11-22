PROMPT DROP — RAGE-STYLE OFFLINE DEMO

Это самостоятельное Android-приложение с интерфейсом в rage-стиле и
демонстрационной сценой с нарисованными ботами. Оно НЕ подключается к
Standoff 2, не внедряется в процесс, не читает/не меняет память игры,
не обходит античит и не гарантирует отсутствие блокировок.

Состав:
- Android app, ARM64 (arm64-v8a)
- Java UI + C++17 native library
- DemoView: фиктивные Box/Skeleton/Health/Distance/FOV визуализации
- Aim FOV и Smooth — только сохраняемые демонстрационные настройки
- SharedPreferences для локального конфига

ВАЖНО: ZIP — это исходный проект, а не APK. Переименование или «конвертация»
архива в APK не работает. APK создаётся компиляцией проекта.

Сборка:
- Android Studio / совместимая сборочная среда
- JDK 17
- Android SDK Platform 35
- Android NDK
- CMake 3.22.1
- Gradle, совместимый с Android Gradle Plugin 8.7.3

В Android-среде с настроенным SDK:
    sh build.sh

Результат:
    app/build/outputs/apk/debug/app-debug.apk

MT Manager может редактировать и запускать shell-скрипты, но сам по себе
не превращает ZIP с исходниками в APK.
