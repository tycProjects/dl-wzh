# Meloza — Android music player

Meloza plays the audio files already stored on your device. It has an
Android 16–oriented Material 3 interface, dynamic colors taken from album
art, a real MediaSession (lock screen, notifications, Bluetooth and
headset buttons), and a local library with search, playlists, favorites,
history, genres, an equalizer and a sleep timer.

Interface languages: **English**, **Русский**, **Українська**.

---

## 1. Build and run

Requirements: Android Studio Ladybug (or newer), JDK 17, Android SDK 36, a device or emulator running Android 8.0+ (API 26+).

1. Unzip `Meloza.zip`.
2. Open the `Meloza` folder in Android Studio and let Gradle sync.
3. Run the `app` configuration on a device.
4. On first launch, allow access to music when asked.

Command line:

```
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

---

## 2. Tutorial

### English

**First launch.** Meloza shows a four-step guide once. Tap *Next* to read it, or *Skip* to leave it. You can allow access to your audio files at any time; if you deny it, the app explains what is missing and offers a button to ask again.

**Home.** "Continue listening" shows recently played songs. "Daily mix" is 20 songs from your library that change once a day. "New in your library" lists the newest albums, "Popular albums" ranks them by how often you play them, and "Favorite artists" follows the songs you have liked. Tap any genre chip to open its songs.

**Search.** Type a song, artist or album name. Results are grouped into artists, albums and songs. Search only covers files on your device.

**Library.** Eight tabs: Songs, Albums, Artists, Genres, Playlists, Favorites, Recently added and History.

**Playlists.** In *Library → Playlists* tap **+** to create one. Open a playlist to rename it, delete it, add songs with **+** in the top bar, reorder songs with the up and down arrows, or remove a song with **−**.

**Player.** Tap the mini player at the bottom to open the full screen. Swipe left or right to move to the previous or next song in the queue. Use the bottom row to favorite a song, set the sleep timer, read lyrics status, or start a radio from the current song. The top-right icons open the equalizer and the queue. The slider under the title seeks, and the wave animates while music plays.

**Profile.** Tap the avatar to set your name. Profile shows plays, favorites, playlists and your most-played artists. Choose light, dark or system theme, and turn album-art colors on or off.

**Notifications and headphones.** Play, pause and skip work from the notification, the lock screen and Bluetooth headsets.

### Русский

**Первый запуск.** Meloza один раз показывает краткое руководство из четырёх шагов. Нажмите «Далее», чтобы листать, или «Пропустить», чтобы выйти. Доступ к аудиофайлам можно разрешить в любой момент: если вы откажете, приложение объяснит, чего не хватает, и предложит запросить доступ ещё раз.

**Главная.** «Продолжить слушать» показывает недавно прослушанные песни. «Микс дня» — 20 песен из вашей медиатеки, который меняется раз в сутки. «Новое в библиотеке» — самые свежие альбомы, «Популярные альбомы» сортируются по числу прослушиваний, «Любимые исполнители» — по песням из избранного. Нажмите на жанр, чтобы открыть его песни.

**Поиск.** Введите название песни, исполнителя или альбома. Результаты делятся на исполнителей, альбомы и песни. Поиск работает только по файлам на устройстве.

**Библиотека.** Восемь вкладок: Песни, Альбомы, Исполнители, Жанры, Плейлисты, Избранное, Недавно добавленные и История.

**Плейлисты.** В *Библиотека → Плейлисты* нажмите **+**, чтобы создать плейлист. Откройте плейлист, чтобы переименовать или удалить его, добавить песни кнопкой **+** в верхней панели, поменять порядок стрелками вверх и вниз или убрать песню кнопкой **−**.

**Плеер.** Нажмите мини-плеер внизу, чтобы открыть полноэкранный режим. Свайп влево или вправо переключает песни в очереди. В нижнем ряду — избранное, таймер сна, статус текста песни и радио от текущего трека. В правом верхнем углу — эквалайзер и очередь. Ползунок под названием перематывает трек, волна анимируется во время воспроизведения.

**Профиль.** Нажмите на аватар, чтобы задать имя. В профиле видно количество прослушиваний, избранного, плейлистов и самых частых исполнителей. Здесь же тема (светлая, тёмная или как в системе) и включение цветов по обложке.

**Уведомления и наушники.** Воспроизведение, пауза и переключение треков работают из уведомления, с экрана блокировки и с Bluetooth-наушников.

### Українська

**Перший запуск.** Meloza один раз показує короткий посібник із чотирьох кроків. Натисніть «Далі», щоб гортати, або «Пропустити», щоб вийти. Доступ до аудіофайлів можна дозволити будь-коли: якщо ви відмовите, застосунок пояснить, чого бракує, і запропонує запитати доступ ще раз.

**Головна.** «Продовжити слухати» показує нещодавно прослухані пісні. «Мікс дня» — 20 пісень із вашої медіатеки, який змінюється щодоби. «Нове у бібліотеці» — найсвіжіші альбоми, «Популярні альбоми» сортуються за кількістю прослуховувань, «Улюблені виконавці» — за піснями з обраного. Натисніть жанр, щоб відкрити його пісні.

**Пошук.** Введіть назву пісні, виконавця або альбому. Результати поділено на виконавців, альбоми та пісні. Пошук працює лише з файлами на пристрої.

**Бібліотека.** Вісім вкладок: Пісні, Альбоми, Виконавці, Жанри, Плейлисти, Обране, Нещодавно додані та Історія.

**Плейлисти.** У *Бібліотека → Плейлисти* натисніть **+**, щоб створити плейлист. Відкрийте плейлист, щоб перейменувати чи видалити його, додати пісні кнопкою **+** у верхній панелі, змінити порядок стрілками вгору та вниз або прибрати пісню кнопкою **−**.

**Плеєр.** Натисніть міні-плеєр унизу, щоб відкрити повноекранний режим. Свайп ліворуч або праворуч перемикає пісні в черзі. У нижньому ряду — обране, таймер сну, статус тексту пісні та радіо від поточного треку. У верхньому правому куті — еквалайзер і черга. Повзунок під назвою перемотує трек, хвиля анімується під час відтворення.

**Профіль.** Натисніть аватар, щоб вказати ім’я. У профілі видно кількість прослуховувань, обраного, плейлистів і найчастіших виконавців. Тут же тема (світла, темна або як у системі) і перемикач кольорів за обкладинкою.

**Сповіщення та навушники.** Відтворення, пауза й перемикання треків працюють зі сповіщення, з екрана блокування та з Bluetooth-навушників.

---

## 3. Architecture

```
app/src/main/java/com/meloza/app/
├── MelozaApp.kt              Application: DB, repository, player controller, app scope
├── data/
│   ├── Track.kt              Track/Album/Artist/Genre models + MediaStore scanner
│   ├── MusicRepository.kt    Library state (loading / no permission / empty / error / ready),
│   │                         search, favorites, history, playlists
│   └── db/MelozaDatabase.kt  Room: favorites, play events, playlists, playlist order
├── playback/
│   ├── PlaybackService.kt    MediaSessionService + ExoPlayer + system equalizer
│   └── PlayerController.kt   UI-side MediaController, queue, sleep timer, EQ control
└── ui/
    ├── MainActivity.kt       Permission, navigation, bottom bar, mini player, artwork accent
    ├── theme/Theme.kt        Material 3 schemes; colors extracted from album art (Palette)
    ├── components/           Artwork, album cards, track rows, animated sound wave
    └── screens/              Home, Search, Library, Profile, Player, Track lists, Tutorial
```

Data flow: `LocalLibrary` (MediaStore) → `MusicRepository` (state flow) → screens.
Playback: UI → `PlayerController` → `MediaController` → `PlaybackService` → ExoPlayer.
Because playback runs in a MediaSessionService, the notification, lock screen and headset controls are provided by the system.

---

## 4. Feature status (honest list)

| Feature | Status |
|---|---|
| Local library: songs, albums, artists, genres | Working (MediaStore) |
| Search across local songs, artists, albums | Working |
| Playback, queue, shuffle, repeat one, seek, swipe between songs | Working |
| Notification, lock screen, Bluetooth, MediaSession | Working (MediaSession) |
| Gapless playback | Built into ExoPlayer for local files |
| Equalizer | Working on devices that expose the system equalizer; otherwise shown as unsupported |
| Sleep timer | Working |
| Playlists: create, rename, delete, add, remove, reorder | Working; reorder uses up/down buttons, not drag-and-drop yet |
| Favorites, history, most-played stats | Working (Room) |
| Dynamic colors from album art | Working |
| Light / dark theme, Material You on Android 12+ | Working |
| Radio from current song | Local only: same artist first, then shuffle of the library |
| Crossfade | **Not implemented.** Needs a custom audio mixing pipeline. |
| Lyrics | **Not connected.** Shows an honest "no source" message. |
| Online recommendations, online radio, online catalog | **Not connected.** See section 5. |
| Home-screen widget | **Not implemented** in this version. |
| Quality settings | Local files play at original quality; no transcoding options exist for local files. |
| Expressive shapes and motion | Material 3 shapes, springs and large rounded corners are used. Full `MaterialExpressiveTheme` can be swapped in once your Material3 version exposes it as stable. |

---

## 5. Connecting an online music source

Meloza does not include any online catalog, because one would need a licence and an API agreement. The app is designed so a legal source can be added as another repository:

1. Obtain access to a licensed API (for example a label or distributor API, or an official streaming SDK with a developer agreement).
2. Create a class like `OnlineCatalog` in `data/` that returns your model (tracks, albums, artists, stream URLs).
3. Add it to `MusicRepository` next to `LocalLibrary`.
4. Replace the "not connected" messages in `Profile` and `Home` with the real data.

Until step 1 is done, online features stay visibly unavailable. Do not add invented or placeholder content as "online" results.

---

## 6. Permissions

- `READ_MEDIA_AUDIO` (Android 13+) or `READ_EXTERNAL_STORAGE` (Android 8–12): read music on the device.
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: background playback.
- `POST_NOTIFICATIONS`: the playback notification.

No account, no network access and no analytics are used.

---

## 7. Known limitations of this version

- The project was written without running it on a device. Expect to fix small compile issues the first time you build (Compose and Media3 APIs change between minor versions). Versions are pinned in `app/build.gradle.kts`.
- Artwork colors use a simple palette extraction; they are good for most covers but are not tuned for every image.
- Equalizer and some audio effects depend on the device's audio hardware.
