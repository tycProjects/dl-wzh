# NOMAN GO – native Android browser (v3.1)

A small, fast browser built on Android `WebView`. The home page, Settings, History,
Bookmarks and Downloads screens are one HTML file (`app/src/main/assets/index.html`)
shown inside the native app. Everything else (toolbar, tabs, menu, downloads, voice search)
is native Kotlin, so scrolling and animations stay on the GPU.

## New in 3.1

### Tabs and session restore
* Every normal tab, its URL, title, desktop/mobile mode, back/forward history and the active tab are saved
  (about a second after a page loads, and always when the app goes to the background). Leaving the app, swiping it
  away or a phone restart brings the exact same tabs back.
* Only the active tab loads at start. The other tabs wake up the first time you open them, so 20 tabs do not slow
  the start. If a tab's saved history cannot be restored (for example after a WebView update) it simply reloads its URL.
* Incognito tabs are never written to disk.
* Settings > Startup: **Where I left off** (new default) restores everything. **New tab page** restores everything and
  also opens a fresh home tab. **A specific page** restores everything and opens that page in a new tab.
  The old default was "New tab page" which threw the tabs away, so it is switched to "Where I left off" once.

### Tab switcher
The same 3D carousel as the home page: 180 x 290 cards that snap to the centre, tilt 36 degrees, shrink and fade with
distance, a moving shine on the preview, a close button that lifts the card away, and the same header
(incognito, tab count, menu, done) and "New tab" pill.

### Downloads
* Normal links go through the system DownloadManager with the right cookies, Referer, user agent, file name
  (also `filename*=` names) and file type.
* `blob:` links (files a page makes itself) are read inside the page and streamed into Downloads. `data:` links are
  decoded and saved. They show up in Settings > Downloads like any other file and open with the right app.
* Android 9 and older ask for storage permission once. A toast tells you when a download finishes or fails.
* Settings > You > Export backup now saves a real file in Downloads.
* Streams that use MediaSource (YouTube and similar) cannot be saved, the menu does not offer Download for them.

### Saving files of every kind (jpg, png, gif, webp, mp3, m4a, mp4, webm, pdf, zip, apk ...)
Ways that all end in the same real download:
* **A link that is a file** (pdf, zip, apk, a link the server marks as "attachment"): downloads by itself.
* **Long press an image**: Download image, Open image, Copy image link (a linked thumbnail also offers the link).
* **Long press a video or audio player**: Download video / audio, Open, Copy link.
* **Long press any link**: Open in new tab, Open in incognito tab, Copy link address, **Download link**
  (that is how you save a .mp3 / .mp4 / .jpg without opening it first).
* **A page that is itself an image, video or audio file**: the three-dot menu shows Download image / video / audio.
* `blob:` and `data:` files, and the backup file, are saved too.
Names come from the server (`filename=` and `filename*=` including non-Latin names), then the URL, and a missing
extension is added from the file type. Images, video and audio also appear in the gallery.
Not possible: live streams that use MediaSource / HLS (YouTube and similar), DRM video, and downloads that need a
POST request. The app says so instead of failing silently.

## Toolbar (same order as Chrome)

`Home` | `URL / search pill` | `Mic` | `Tabs (live count)` | `Three-dot menu`

* **Home** loads the NOMAN GO home page. If you are already there it scrolls to the top.
* **URL pill** shows a green lock (https), an orange info icon (http) or a search icon. While a
  page loads the icon spins and the pill gets an accent border, next to the progress bar.
* **Mic** opens the system voice search (no extra permission needed). It is hidden automatically
  on phones without a speech recogniser, and can be switched off in Settings > Search.
* **Tabs** opens the tab switcher. The number bounces when it changes.
* **Menu** icon row: Back, Forward, Bookmark (web pages), Refresh / Stop.
  List: New tab, New incognito tab, Share (web pages), **Desktop site**, Bookmarks, History,
  Downloads, Settings. Every icon pops in, then loops a small animation while the menu is open.

## Desktop site: always in the menu

* On a **web page**: switches that site between mobile and desktop. The WebView user agent is set
  before the page loads, the viewport is widened and the page reloads. It is remembered per site and
  listed in Settings > Desktop site, where each site can be removed.
* On the **home page and internal pages**: the same row reads "Desktop site (all sites)" and
  toggles the global setting. The home page switches to its desktop layout straight away and every
  site without its own choice follows it.
* A site-specific choice always wins over the global one.

## Build the APK without Android Studio

1. Create a GitHub repository and upload the **contents** of this folder to its root.
2. Open the *Actions* tab, choose **Build debug APK**, press **Run workflow**.
3. When it finishes, download the artifact `NomanGo-debug-apk` and install `app-debug.apk`.

Or open the folder in Android Studio (Hedgehog or newer, JDK 17) and press Run.

## Smoothness notes

* No bottom bar, no software layers, no alpha fades on the WebView itself.
* Theme colours are only re-applied when they actually change.
* Toolbar and menu animations use property animators (translation, scale, rotation, alpha) only.
* Background tabs are paused and hidden. Menu icon loops are cancelled when the menu closes.
* Settings > Performance > "Reduced animations" or "Battery saver" turns all of the above off.

## Known limits

* Incognito tabs have no separate cookie jar (WebView limitation), but history is not saved.
* The `Sec-CH-UA-Mobile` client hint is not changed. Sites that detect desktop only through it
  may still serve the mobile page. The user agent and viewport are changed, which is what
  almost every site uses.
* No find-in-page, tab thumbnails, autofill or sync yet.
* This project was written and statically checked (brackets, resources, ids, symbols), but the Kotlin code has not
  been compiled or run on a device in this session. The first GitHub Actions build is the real test.
