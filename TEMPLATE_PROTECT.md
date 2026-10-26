# Content hiding for Quick build ("Encrypt app content")

This used to describe a `.wc`/`dexopt` scheme that was never implemented anywhere -- not in the
template, not in the server. The server's `generateWebViewWrapperProject` (see `server.js`, the
`runtimeConfig` branch) already has a *different*, real, wired-up scheme: `SealedSite.java`. This
file now documents that one, which `lib/quick/quick_protect.dart` and `lib/quick/quick_builder.dart`
match exactly.

## How it actually works (server side, already shipped)

When a build has `runtimeConfig` on (true whenever `quickTemplate=1`, i.e. every build of
`assets/quick/template.apk`), the server generates three classes and wires them into
`MainActivity`'s `shouldInterceptRequest` and its start-URL picker:

- **`QuickConfig.java`** reads `assets/wzh_quick.json` once at launch: `remoteUrl` (website-link
  builds) and `cacheMode`. Nothing about encryption lives here.
- **`SealedSite.java`** looks for files stored as plain top-level zip entries named
  `m/<16-byte hash of the file's relative path, hex>` -- *outside* `assets/`, so `AssetManager`
  never lists them and a casual unzip finds no `index.html`, no file names, no extensions.
  - Key: `SHA-256(SALT_HEX_bytes ++ 0x01 ++ SHA-256(this app's own signing certificate DER))`,
    derived fresh every launch from whatever certificate the running APK is actually signed
    with. Nothing about the key is stored in the APK -- re-sign or edit the APK and it derives a
    different key and can't decrypt its own sealed files.
  - Each file: `AES/GCM/NoPadding`, random 12-byte nonce, **no associated data** -- stored as
    `nonce(12) ++ ciphertext+tag`.
  - `SALT_HEX` is a fixed 32-byte constant baked into `SealedSite.java` (see `server.js`).
- The start URL for local-file builds becomes
  `SealedSite.present(this, entryHtmlRel) ? EMBED_HOST + entryHtmlRel : file:///android_asset/entryHtmlRel`,
  and `shouldInterceptRequest` serves `SealedSite.get(...)` for any request under `EMBED_HOST`.

## Flutter side (`lib/quick/quick_protect.dart`, `lib/quick/quick_builder.dart`)

Matches the above byte for byte: no config file involved, nothing per-app to generate except the
AES key (derived the same way, from `cert` -- the app's own signing certificate, already in hand
as part of every Quick build). Site files get written as `entries[prot.assetName(rel)]` (an
`m/<hash>` top-level zip entry) instead of `entries['assets/$rel']` when "Encrypt app content" is
on, and the template's own placeholder `assets/index.html` is dropped so nothing of it lingers.

`quick_builder.dart` only attempts this when the bundled `assets/quick/template.apk`'s
`classes.dex` actually contains `"wzh_quick.json"` (a literal from `QuickConfig.java`) -- i.e. it
was built by a version of `server.js` that has `runtimeConfig` wired up at all. An older template
falls back to plain, unencrypted `assets/<path>` files, same as if the toggle were off, since
there's nothing in that template that could read a sealed copy.

## Known gap: "Block screenshots" / "Block rooted or tampered"

These two Options toggles (`blockCapture`, `blockRoot`) have **no implementation anywhere** --
not in `SealedSite.java`, not anywhere else in `server.js`'s generated wrapper. No `FLAG_SECURE`,
no root/debugger check, no re-sign check tied to these toggles exists in the current template.
`BuildRequest.blockCapture` / `.blockRoot` are still collected from the Options page and carried
through to `_Job` in `quick_builder.dart`, but nothing reads them anymore -- there is nothing on
the template side for them to configure. They're a no-op until FLAG_SECURE / a root-or-debugger
check is actually added to the server's wrapper generator and a new Quick template is built from
it. Flagging this explicitly so it isn't mistaken for a working feature; the Options page toggles
should probably say so (or be removed) until then.
