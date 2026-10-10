# Android VideoInfoOverlay

Experimental video information overlay for Android TV devices.
Compatibility with other devices is not guaranteed.

Based on Jamal's original application and the almirus source mirror.
Original attribution and MIT license are retained.

Package ID: `com.signde.androidvideooverlay`

[Download the experimental APK](https://github.com/signde/android-video-overlay/releases/tag/v21.1)

![Android VideoInfoOverlay during Dolby surround sample playback](docs/images/overlay-dolby-sample.png)

## Verified devices

- onn 4K Pro v2, Android TV 14
- onn 4K Plus, Android TV 14
- Google TV Streamer 4K (contributor-reported)

## Setup and use

See the [installation guide](INSTALL_GUIDE.md) for ADB connection methods and help
if the Accessibility toggle immediately switches off.

1. Install the APK.
2. In Android TV **Settings > Accessibility**, enable **Android VideoInfoOverlay**.
   Press Back to fully close the setup screen.
3. Leave ADB debugging enabled and approve the app's local ADB connection when
   prompted. The app requires ADB on port 5555; no root or any other helper is needed.
4. Press the remote's **Favorites / Bookmark** button to show or hide the overlay.
   You can also open **Android VideoInfoOverlay** from the apps list to toggle it.

For appearance and remote-button settings, open **Settings > Accessibility >
Android VideoInfoOverlay > Configuration**.
