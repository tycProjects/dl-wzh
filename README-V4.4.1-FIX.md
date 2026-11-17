# WEB2APK V4.4.1 — Code Editor ZIP import compile fix

This is a minimal source patch over V4.4. The method `findReferencedAsset(...)` now declares `throws IOException`, matching `relativeEditorPath(...)`. Its call sites are inside `importEditorZip(...)`, which already declares `throws Exception`. No Gradle dependencies, SDK levels, Android manifest permissions, runtime template, signing configuration, or existing UI features were changed.

Build with `bash ./build-v4.sh`.
