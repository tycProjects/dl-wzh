# GlassDex

Android app (Kotlin + Jetpack Compose) that unpacks and inspects **APK, XAPK and EXE/DLL** files, scores malware risk, and shows detailed app info in a rounded, transparent "liquid glass" UI. Login with Google, email, or guest.

## Build
1. Open this folder in **Android Studio** (Ladybug or newer), let Gradle sync (needs internet once).
2. Run on a phone/emulator (Android 8.0+), or **Build > Build APK(s)**.

Guest mode works with no extra setup.

## Enable Google + email login (optional)
1. Create a project at console.firebase.google.com and add an Android app with package `com.glassdex.analyzer` (add your SHA-1 from `./gradlew signingReport`).
2. Authentication > Sign-in method: enable **Google** and **Email/Password**.
3. Download `google-services.json` into `app/`.
4. Copy the **Web client ID** (Authentication > Google > Web SDK configuration) into `local.properties`:
   `GOOGLE_WEB_CLIENT_ID=1234567890-abc.apps.googleusercontent.com`
5. Sync and rebuild.

Without `google-services.json` the login buttons show a message and guest mode still works.

## VirusTotal (optional, recommended)
Settings > paste a free API key from virustotal.com. The app sends only the file's SHA-256 hash, never the file.

## What it does
| Input | Output |
|-------|--------|
| APK | Every file extracted, binary XML (manifest + `res/*.xml`) decoded to readable XML, per-class skeletons (class, fields, method signatures) for every DEX, strings dump, signature certificates, permissions, components, native libs |
| XAPK | Extracts the container, analyzes the base APK, lists split APKs |
| EXE / DLL | PE headers, sections with entropy, import table, packer detection, strings dump |

Results are saved under the app's private storage, browsable in-app and shareable as a ZIP.

## Limits (read this)
- **Not a full decompiler.** Class output is a skeleton, with names, fields and method signatures. It does not turn Dalvik bytecode into Java source. For that, run the extracted `.dex` files through `jadx` on a PC.
- **EXE files** are analyzed statically. Nothing is executed or disassembled.
- **Malware score is a heuristic**: permission combos, risky API use, packers, embedded payloads, plus the VirusTotal verdict when a key is set. It can miss malware and can flag harmless apps. It is not an antivirus.
- App name/icon fall back to the package name or the best PNG icon when the manifest label is a resource reference (resources.arsc is not decoded).
- History is stored on the device per account. It is not synced across phones.
- Not compiled or run in the environment that generated it. Expect to fix a small compile error or two on first build.
