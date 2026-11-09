# NOMAN GO – native Android browser (v3.6)

A small, fast browser built on Android `WebView`. The home page, Settings, History,
Bookmarks and Downloads screens are one HTML file (`app/src/main/assets/index.html`)
shown inside the native app. Everything else (toolbar, tabs, menu, downloads, voice search)
is native Kotlin, so scrolling and animations stay on the GPU.

## New in 3.6

### Tab manager: a vertical list
Tap the tab count to open it. Every open tab is one card in a scrollable vertical list, in the same black / red / white card
style as before, with the open tab marked by the red border and its moving shine:
* **Real preview**: the top of the page as you last saw it, taken when you open the manager, when you leave a tab, when the app
  goes to the background, and about a second after every page load, so it updates by itself. Each tab keeps its own picture
  (also after a restart). A tab that has not been seen yet shows the old skeleton instead of a made-up picture.
* **Title, address and favicon** of the page. If a site gives no icon, a coloured letter is shown.
* **Search** (field at the top): filters by title and by address while you type.
* **Pin / unpin** (pin button): pinned tabs move to the top and stay pinned after a restart. The back button never closes a
  pinned tab, and **Close all** keeps pinned tabs unless you tick "Also close N pinned tabs". Closing one pinned tab asks first.
* **Close** (x) closes one tab; the open page of every other tab, its scroll position and its history are untouched.
* **Close all** (trash button, next to the search) asks first. It switches to a surviving tab once and does not wake sleeping
  tabs on the way.
* Opening a link that is already open (from another app) switches to that tab instead of making a duplicate.
* The list recycles its cards, so a long list costs the same as a short one: only about a screen of cards exists, each card is
  filled when it scrolls into view, preview pictures and icons are read from disk only for cards that are on screen, and
  nothing is redrawn unless the tabs, the search or the open tab really changed. The shine is one animator on one card.
The browser-only `NOMAN-GO.html` keeps its own simple home-page tab view; it has no real tabs.

## New in 3.5

### The refresh icon can no longer spin forever
The little spinner in the address bar, the "Stop" entry in the menu, the loading bar and the pull-to-refresh circle all
follow one state per tab (`LoadGuard.kt`). A load now ends, and everything stops together, in every one of these cases:
* the page finishes, or the browser says progress is 100 % but forgets to say "finished" (checked after 1.5 s);
* the page fails: error page, or a bad certificate on the page itself (it now shows the error page too);
* you press Stop in the menu;
* the navigation is cancelled because it turned into a download, or was handed to another app (phone, mail, Play Store ...);
* nothing happens for 30 s (the load is stopped and a short message is shown), or the page is still trickling after 90 s
  (it stops looking busy, the page keeps loading);
* the tab is closed or replaced by a new load.
The home page and the tab switcher never show a spinner. In the HTML viewer the same rule applies with a 25 s time-out.

## New in 3.4

### Downloads page
* A 3D header card (it sways by itself and tilts toward your finger) with the number of files, the space they use, how many
  downloads are running, the live speed and a colour bar that shows what takes the space (videos, images, audio, documents,
  archives, apps).
* Type chips with counts (All, Images, Videos, Audio, Documents, Archives, Apps, Other), search, and sort (Newest, Oldest,
  A to Z, Biggest). Files are grouped by Today, Yesterday, This week, Earlier; running downloads stay on top.
* Every file has a 3D tile in its type colour. A running download shows a progress ring, the percentage, a progress bar, the live
  speed and the time left. Tap a finished file to open it; Share, Remove from list, Retry (failed) and Cancel (running) are buttons.
* Long press a file (or the tick button) to select several: Share, Remove from the list, or Delete the files from the phone
  (it asks first). Removing from the list can be undone for a few seconds.
* "Clear completed" and a button that opens the phone's own Downloads folder.
* Empty state with a turning 3D cube. All 3D motion follows Settings > Performance (3D effects, Reduced animations).

### Build notes
* Needs JDK 17 and Android SDK 34; Gradle 8.9 is installed by the workflow. No extra libraries are used.
* The Kotlin compiler runs inside the Gradle process with 4 GB (see `gradle.properties`) because `MainActivity.kt` is large.
* Release builds skip Android Lint (it cannot fail the build); the release APK is signed with the debug key so it installs.

## New in 3.3

### Tab previews
Every card in the tab switcher shows a real screenshot of that tab's page (the top of the page, as wide as the card),
inside the same preview area, with the same shine on top. A picture is taken when you open the tab switcher, when you
leave a tab, and when the app goes to the background, so each card shows the page as you last saw it. Pictures are kept
on disk per tab (in the cache folder) so they are still there after a restart, are deleted when the tab is closed, and
tabs without a picture yet keep the old skeleton look. Incognito tabs are never written to disk.

### Translate
Three-dot menu > **Translate** (web pages). Pick a language and the real text of the page is replaced by the translation,
including placeholders, button labels, image descriptions and the page title. Text that appears later (scrolling,
single page apps) is translated as it arrives. **Show original** puts every word back exactly as it was.
Code blocks, `translate="no"` / `notranslate` elements, numbers, web addresses and e-mail addresses are left alone.
The page language is detected with the same service, and a page that is already in the chosen language says so.
Text inside other sites' iframes cannot be reached and is not translated.

You need one of these (Settings > Translate has the same steps and a Test button):

* **Microsoft Translator** (2 million characters free every month)
  1. Create a free account at portal.azure.com.
  2. Create a resource "Translator", pricing tier **Free (F0)**.
  3. Open it > Keys and Endpoint. Copy **KEY 1** and the **Location/Region**.
  4. In the app: Settings > Translate > Microsoft, paste the key, type the region (for example `southeastasia`), Save, Test.
* **Google Cloud Translation** (500,000 characters free every month, billing must be switched on)
  1. console.cloud.google.com > create a project > enable billing.
  2. Enable the **Cloud Translation API**.
  3. APIs and Services > Credentials > Create credentials > **API key** (restrict it to the Cloud Translation API).
  4. In the app: Settings > Translate > Google Cloud, paste the key, Save, Test.
* **LibreTranslate** (free, you run the server): start a server (for example the Docker image `libretranslate/libretranslate`),
  then Settings > Translate > LibreTranslate and type its address. A key is only needed if your server asks for one.

No Google sign-in is used anywhere, only the API key. The key is saved only on the phone, in its own file that is left out
of Android backups and out of the app's own Backup export. Free amounts and prices are set by the providers and can change.
Pages are capped at about 150,000 characters per translation so a huge page cannot use up your monthly amount.

### Pull to refresh
Pull down at the top of a web page and release to reload it. A round indicator in the theme colour follows your finger,
ticks when you pass the point of no return and spins until the page has loaded. It only starts when the page really is
at the top: a box inside the page that can still scroll up keeps scrolling (pull.js checks this), pages that set
`overscroll-behavior` or `touch-action: none` (maps, games) are left alone, horizontal swipes and two-finger gestures never
trigger it, and the home page and the tab switcher are not affected. It can be switched off in Settings > Performance.

## New in 3.2

### Loading bar under the address bar
A 3 dp bar sits directly under the toolbar. It fills from the left with the real WebView progress while a page loads or
refreshes (menu, pull to refresh, typed address), its blue, cyan, white and purple keep flowing left to right, and when the
page is done it runs to the end and fades out. It never moves backwards, it finishes once per load (no flash when the
browser reports "finished" twice) and a new load that starts while the old bar is still fading begins again from the left.
It is one small custom view (`LoadBar.kt`), so it costs almost nothing. With "Reduced animations" the colours stay still
but the bar still shows the progress. The HTML viewer and the home search loader use the same colours.

### Remove a recent search
Every recent search in the address-bar list has a **x** button. It deletes that search for good, the row slides away
and the next older search takes its place. While you type, matching recent searches (up to three) are shown with the
same button. In the HTML home page the same x removes it from the Recent list and it stays removed after a restart
(your History page keeps the visit). Searching for the same words again brings it back.

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
See "Tab manager" in 3.6 below (it replaced the 3D carousel).

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
