# Установка и сборка

1. Установите Android Studio и JDK 17.
2. Откройте папку проекта.
3. Дождитесь Gradle Sync.
4. Выберите Build > Make Project или выполните `./gradlew :app:assembleDebug`.
5. Установите `app/build/outputs/apk/debug/app-debug.apk`.

Требуется Android SDK Platform 35 и minSdk 33. Для SDK/AGP новее используйте
официальную матрицу совместимости Android Gradle Plugin.
