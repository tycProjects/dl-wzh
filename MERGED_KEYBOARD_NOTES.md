# Merged NeonKey + Live Video Wallpaper

- Preserved the original LiveVideoWallpaper Android project.
- Added a native Android Input Method Service (`NeonKeyboardService`) so the keyboard can type into other apps, not just its HTML preview.
- Added Enable NeonKey and Select Keyboard controls to the main screen.
- Included the supplied NeonKey Studio HTML designer in `app/src/main/assets/neonkey_studio.html` without discarding its font, wallpaper, sizing, and design controls.

## Enable the keyboard
1. Install the built APK.
2. Open the app and tap **Enable NeonKey**; Android opens its keyboard settings.
3. Enable **NeonKey Keyboard**.
4. Tap **Select Keyboard** and choose it.
5. In any text field, the keyboard will appear.

The native IME is a functional baseline keyboard with letter entry, shift, backspace, emoji, space, number toggle, and enter/action handling. The HTML studio remains the supplied interactive design editor; its browser-only video preview is not automatically the Android IME background. Gallery video wallpaper remains handled by the original live-wallpaper module.
