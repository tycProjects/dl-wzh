# ZewMod — Reverse Engineering Notes
**Rebuilt from:** `NikuMods_Aimbot_Rage_Proxy.apk`

---

## APK Structure Analysis

| Field | Value |
|---|---|
| Package | `com.nikumods.app` |
| App Name | NikuMods |
| Version | 1.0.0 |
| Build System | Gradle 8.6.1 |
| Kotlin Version | 1.9.24 |
| Target JDK | 17 |
| Signer | Signflinger (Android toolchain) |
| Architecture | arm64-v8a |

## Files Extracted

```
AndroidManifest.xml         ← binary (tidak bisa baca tanpa apktool)
classes.dex                 ← Dalvik bytecode (heavily obfuscated → Lo/a, Lo/b, ...)
lib/arm64-v8a/
  libnikumods_native.so     ← 5KB native bridge (JNI, XOR-encrypted strings)
assets/
  Assembly-CSharp-patch.bytes ← 13KB Unity IL2CPP hot-patch (ILFix format)
  dexopt/baseline.prof       ← ART baseline profile
  fa-solid-900.ttf           ← Font Awesome Solid font
  localConfig.json           ← { "testCodePatch": true, "resetGuest": true }
```

## Native Library Analysis (`libnikumods_native.so`)

**Size:** 5.0 KB — minimalis, hanya 3 JNI exports

**JNI Exports:**
```
Java_com_faceboook_appmanager_data_NativeGate_nativePatchUrl
Java_com_faceboook_appmanager_data_NativeGate_nativeConfigUrl
Java_com_faceboook_appmanager_data_NativeGate_nativeUiStrings
```

> Note: "faceboook" (3 o) — intentional typo untuk evade package name detection

**String Obfuscation:**
XOR keys ditemukan di rodata: `"Bz$W8"`, `"c(?CK4X"`, `"4% X~;"`, `"xv(,2K&"`, `"iq80"` (rolling)

**Dependencies:** libc.so, liblog.so, libm.so, libdl.so

**Compiler:** clang 17.0.2 (Android r487747d) + LTO + PGO + BOLT

## Unity Patch Analysis (`Assembly-CSharp-patch.bytes`)

**Format:** ILFix binary (dIFix.ILFixInterfaceBridge)

**Target Game:** Call of War (COW)
- Assembly-CSharp **Version=0.86.0.518**

**Patched Classes:**
| Class | Purpose |
|---|---|
| `COW.GamePlay.Player` | Player entity — hook HP, position, LockedAimingCollider |
| `COW.GameFacade` | Game manager — access game state |
| `COW.GamePlay.AttackableEntity` | Enemy entity — hook for ESP detection |
| `COW.GameSettingData` | Game settings — override FOV, sensitivity |

**Hooked Methods:**
```
get_RootTransform / GetHeadTF     → ESP head tracking
CurrentLocalPlayer / IsLocalPlayer → self-identification
SetAimRotation / LookRotation      → aimbot rotation
get_HeadCollider                   → aimbot lock target
get_LockedAimingCollider           → bone lock for aimbot
WorldToScreenPoint                 → 2D projection for ESP box
IsVisible / SetActive              → visibility check for ESP
GetNickName                        → name ESP
get_CurHP                          → HP bar ESP
GetLeftAnkleTF / GetRightAnkleTF   ─┐
GetLeftToeTF / GetRightToeTF       ─┤  skeleton ESP bone positions
get_SpineBoneTransform             ─┤
get_ShoulderBoneTransform          ─┘
get_HipsBoneTransform              ─┘
```

**UDP Proxy (network classes found):**
```
System.Net.Sockets.UdpClient
System.Net.IPAddress ("127.0.0.1" literal)
System.Net.IPEndPoint
```

**Pipe-format tokens ditemukan di strings:**
```
|ENEMY| |HP| |TEAM| |SELF| |ROOT| |HEAD| |LOCAL|
|ROOT_SCREEN| |HEAD_SCREEN| |YUP|1
|WIDTH| |HEIGHT| |NAME| |FORWARD|
|LEFT_ANKLE_SCREEN| |RIGHT_ANKLE_SCREEN|
|LEFT_TOE_SCREEN| |RIGHT_TOE_SCREEN|
|SPINE_SCREEN| |SHOULDER_SCREEN| |HIPS_SCREEN|
```

**Shader materials:**
- `Hidden/Internal-Colored` → chams/ESP line material
- `Unlit/Color` → solid color overlay
- Props: `_Cull`, `_SrcBlend`, `_DstBlend`, `_ZWrite`, `_ZTest`

## Obfuscation Analysis

**DEX classes:** Semua di-obfuscate ke `Lo/<single-letter>` pattern (a-z, a0-z9, a00-z00...)
- Total: 700+ classes
- Package `o` = semua app classes
- `com.faceboook.appmanager.data.NativeGate` = satu-satunya unobfuscated class (JNI requirement)

**Shizuku integration:**
- `rikka.shizuku.*` — privilege escalation tanpa root
- `moe.shizuku.server.*` — server-side Shizuku protocol

## Build ZewMod dari Source

```bash
# Setup
git clone <this-repo>
cd zewmod_src

# NDK build (untuk native .so)
# Pastikan Android NDK terinstall
ndk-build APP_ABI=arm64-v8a

# Gradle build
./gradlew assembleRelease

# Sign APK
apksigner sign --ks your-keystore.jks \
    --out ZewMod-release-signed.apk \
    app/build/outputs/apk/release/app-release-unsigned.apk
```

## Dependencies (build.gradle)

```kotlin
implementation("rikka.shizuku:api:13.1.5")
implementation("rikka.shizuku:provider:13.1.5")
implementation("androidx.appcompat:appcompat:1.7.0")
implementation("com.google.android.material:material:1.12.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
```

---
*Reverse engineered by Zewwwwwwwwww — ZewMod v1.0*

---

## v2 — Thay đổi

### Bỏ Shizuku → Wireless ADB

**Cách setup (1 lần duy nhất):**
1. Vào **Settings → Developer Options → Wireless Debugging** → bật ON
2. Mở ZewMod app → tự động detect port và kết nối
3. Xong — không cần install Shizuku, không cần grant permission thủ công

**Cơ chế:**
```
App → Socket(127.0.0.1, 5555~5585) → ADB daemon → shell cp patch → game dir
```
Scan tự động từ port 5555 → 5585, tìm được port nào kết nối luôn.

### ESP v2 thay đổi
| Thứ | Trước | Sau |
|---|---|---|
| Box color | Xanh lá | **Xanh nước biển nhạt #7EC8E3** |
| Box style | Full rect | **Corner box (4 góc L-shape)** |
| Snapline | Không có | **Trắng, giữa màn → chân địch** |
| Counter | Không có | **Góc trên giữa, hiện số địch** |
| Smooth | Không | **Không — snap trực tiếp** |

### ESP không smooth vì:
`postInvalidate()` gọi trực tiếp không qua interpolation.
Data từ IL2CPP patch → ProxyRelay → `updateEnemies()` → `postInvalidate()` — không có lerp, không có delay buffer.
