# Ryomd Tube — Quality, 100-video feed, refresh, and UI update

Basis: user-supplied `RYOMD_NATIVE_YOUTUBE_PLAYER_V2_3_TUBE_WZH_SOURCE.zip`.

- Removed Google sign-in, account profile UI/actions, native OAuth bridge, OAuth dependency, and account-only playlist tab.
- Preserved on-device watch history, local channel subscriptions, public YouTube search/discovery, video/channel pages, link playback, and native player.
- Native catalog now follows NewPipe result-page cursors and targets up to 100 results per query, with duplicate protection.
- Home feed exposes a refresh button and pull-down-to-refresh gesture.
- Native player offers Auto and the progressive video heights actually available for the current video; quality changes re-resolve the stream and resume near the previous playhead.
- App launcher name is `Ryomd panel`; feature branding stays `Ryomd Tube`.
- UI/UX reference: official `nextlevelbuilder/ui-ux-pro-max-skill` repository.

Resolution selection depends on the stream formats exposed by the video. Videos served only through an adaptive HLS/DASH manifest may expose Auto without manual height choices. Actual playback quality and WZH build must be device-tested.
