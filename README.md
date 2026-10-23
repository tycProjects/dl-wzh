# ARAFAT AI — Your Personal AI Assistant
**Developer:** ARAFAT MOJMUNDAR  
**Platform:** Native Android (Kotlin + Jetpack Compose)  
**Package:** `com.arafat.ai`

---

## 🌟 Overview
ARAFAT AI is a futuristic personal AI assistant built natively for Android using Kotlin and Jetpack Compose. Inspired by advanced cybernetic interfaces with electric blue, cyan neon, glassmorphism, and animated HUD concentric visualizer rings.

### Key Capabilities:
- ⚡ **Multi-Model Intelligence:** Real integration with Google Gemini (`gemini-3.5-flash`, `gemini-3.1-pro-preview`) and xAI Grok (`grok-2-latest`, `grok-beta`) with an intelligent **Auto-Fallback Engine**.
- 🇧🇩 **Multilingual Fluency:** Full text and voice interaction in English, Bengali (বাংলা), and Hindi.
- 🎙️ **Voice Assistant:** Real-time speech recognition and Text-to-Speech (TTS) with animated rotating HUD visualizer rings reacting to Idle, Listening, Thinking, and Speaking states.
- 💻 **Coding Assistant:** Code generation, code explanation, debugging, project folder architecture, and master prompt generation for Kotlin, Python, JS, TypeScript, HTML/CSS, SQL, C++, and more.
- 📱 **Android App Launcher:** Query launchable applications via Android `PackageManager`, search installed apps, and execute voice shortcuts ("Open YouTube", "Open WhatsApp", "Open ChatGPT", "Open Chrome", "Open Settings").
- 🤖 **ChatGPT Suite:** One-tap launcher for ChatGPT (app or web) and a Super Prompt Generator for apps, websites, and games.
- 🖥️ **Screen Assistance:** Consent-based screen capture and UI element analysis.
- 🔒 **Privacy & Local Security:** Clear chat history, local snippet saving, masked API key inputs, and explicit confirmation before external calls or messages.

---

## 📦 Locating and Installing the APK

### 1. Build the Debug APK
From the project root:
```bash
gradle assembleDebug
```

### 2. Locate the Generated APK
Once compiled, your installable Android package is located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 3. How to Install on an Android Phone
1. Transfer `app-debug.apk` to your phone via USB cable, Google Drive, Bluetooth, or local download.
2. On your Android device, tap on `app-debug.apk` in your File Manager or Downloads.
3. If prompted: **"Install unknown apps"**, allow permission for your browser or file manager in **Settings > Security & Privacy > Install unknown apps**.
4. Tap **Install**, then tap **Open** to launch **ARAFAT AI**.

---

## 🔑 API Key Configuration
ARAFAT AI supports both zero-configuration runtime injection and in-app customizable keys:
1. **Gemini API Key:**
   - Configure `GEMINI_API_KEY` in `.env` (or via the **Secrets panel in AI Studio**).
   - Alternatively, open **ARAFAT AI > Settings > Gemini AI Configuration** and enter your key directly.
2. **Grok (xAI) API Key:**
   - Configure `XAI_API_KEY` in `.env`.
   - Or open **ARAFAT AI > Settings > Grok AI Configuration** and enter your bearer key.

---

## 🛡️ Release vs. Debug APK
- **Debug APK (`app-debug.apk`):** Signed automatically with Android's default `debug.keystore`. Suitable for local testing and developer side-loading.
- **Signed Release APK (`app-release.apk`):** Optimized with ProGuard/R8, zipalign, and signed with your secure private keystore for Google Play deployment.

### Generating a Release Keystore (For Production):
```bash
keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```
*Never commit `.jks` files or passwords to source control.*
