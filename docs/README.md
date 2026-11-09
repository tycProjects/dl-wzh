# HyperStatusBar Pro

Companion UI на Jetpack Compose для настройки предполагаемого backend статус-бара Xiaomi.
Текущий scaffold проверяет `su`, хранит настройки в Preferences DataStore, показывает
предпросмотр и передаёт JSON в Magisk config bridge.

**Важно:** слайдеры и preview работают в приложении. Они не меняют системные иконки без
совместимого RRO/LSPosed backend. MIUI/HyperOS не предоставляет стабильного публичного API
для произвольного перемещения и масштабирования элементов SystemUI.
