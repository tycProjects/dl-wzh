# Update 1.7.0 — Heart that follows the player, repeat-one button, leaner window and heap

## Analysis (meminfo + gfxinfo of 1.6.3)
- Graphics buffers: four 720x960 buffers (about 11 MB) come from the fixed drawing window (60% of the screen).
- Pipeline is already `Skia (Vulkan)` on the test device. The app only uses AGSL `RuntimeShader` and plain canvas
  drawing (no GLES, no SurfaceView/TextureView), so it behaves the same on Skia GL and Skia Vulkan.
- Frames: 0.51% janky, p99 20 ms. The ~25 frames over 150 ms are rare events (no timestamps in the dump).
- Heart: the island read only `METADATA_KEY_USER_RATING`. Many players (Spotify, YT Music, ...) expose no rating,
  so the island fell back to its own saved list, which never learns that the heart was turned off in the player.

## Changes
### Heart (sync with the player)
- `Controls.readLiked` reads fresh from the player: rating, then the player's custom actions
  ("Remove from Liked Songs" = liked, "Add to Liked Songs" = not liked, "Unlike", ...).
- New, event driven: the player's own media notification is read (read only, never cancelled). Players repost it
  when the heart changes, so the island learns about it with no polling.
- Opening the island always re-reads the heart: immediately, and again after 450 ms and 1.3 s.
- While expanded and the screen is on, a light 2.5 s check runs (was a full media rebuild every 1 s).
- A bare "Like" label is only trusted after that player was seen switching labels (some apps keep one label).
- After tapping the heart in the island, the player's old answer cannot flip it back for 1.8 s.
- Custom-action players are no longer toggled twice when the state already matches.

### Repeat-one button (music only, right of "next")
- Fills the empty slot at the end of the transport row.
- Sends repeat-one to the player through the compat session (`MediaSessionCompat` / Media3 players).
- If the player does not confirm the mode within 450 ms, the island restarts the track itself:
  one timer per track, 450 ms before the end, only while playing. Nothing runs when it is off.
- Small details: stagger entrance, jelly press and halo, icon turn on toggle, spring pop, three orbiting dots,
  ring ping, accent disc and dot while on, and a ring pulse each time the song loops back to the start.
- The state is read back from the player each time the island opens.

### RAM, heap, frames
- Drawing window 60% -> 48% of the screen height (`WindowTuning.HEIGHT_FRACTION`, shared with the stack cap).
  Buffers: about 11 MB -> about 8.8 MB. Longer WhatsApp stacks overlap and scroll as before.
- Expanded progress ticker no longer calls `playbackState` (binder + allocation) four times a second.
  It anchors once and then computes the position locally.
- Heart/repeat refresh no longer rebuilds the whole media object every second.
- `onTrimMemory`: older artwork copies are dropped under memory pressure (the current one is kept).
- Notification dedupe memory trimmed (300 fingerprints, 200 keys).
- Manifest: `hardwareAccelerated`, Vulkan `uses-feature` entries marked not required.

## Still to check on device
- Heart: turn it off inside the player, then open the island. Check the Diagnostics log for `LIKE_NOTIF`.
  A player that exposes neither rating, custom action nor a labelled notification button cannot be read.
- Repeat: turn it on and let a song end. Players that honour it loop by themselves; others are restarted by the island.
- Expanded media with lyrics and a WhatsApp stack still fit in the 48% window (raise HEIGHT_FRACTION if cut).
- Compare meminfo again (GL mtrack and graphics buffers) and the frames over 150 ms.

Not compiled or run on a device here.
