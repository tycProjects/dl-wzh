# Magisk config bridge

Шаблон создаёт каталог модуля и хранит `config.json`. Он **не изменяет SystemUI**.
Для реального изменения status bar требуется отдельный backend, проверенный на точной
версии MIUI/HyperOS, Android API и модели устройства. Не устанавливайте случайные
overlay-ресурсы в SystemUI: это может вызвать bootloop или падение SystemUI.
