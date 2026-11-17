# DroidStore

Экосистема приложений: Android-клиент, DroidStore Studio (портал разработчиков), сервер и административная панель.

## Структура
- `app/` — Android-приложение (Kotlin, Jetpack Compose, Material 3 Expressive)
- `backend/` — API, авторизация, хранение APK, модерация
- `database/` — схема БД
- `admin-panel/` — административная панель
- `developer-portal/` — DroidStore Studio
- `docs/` — документация

Подробная инструкция: `docs/SETUP.md`.

## Статус
Backend с API, входом через Google, студиями, загрузкой APK, модерацией, скачиваниями, оценками и журналом аудита реализован. Android-экраны и developer portal ещё в разработке.
