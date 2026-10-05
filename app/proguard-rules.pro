# SmoothCamera keeps its own code conventional; CameraX/AndroidX ship their own rules.
# Keep the application entry point and model classes stable for reflection-safe future extensions.
-keep class com.example.smoothcamera.** { *; }
