# Android Video Info Overlay

Experimental video information overlay for Android TV devices.
Compatibility with other devices is not guaranteed.

Based on Jamal's original application and the almirus source mirror.
Original attribution and MIT license are retained.

Package ID: `com.signde.androidvideooverlay`

[Download the latest experimental APK](https://github.com/signde/android-video-overlay/releases/latest/download/Android_VideoInfoOverlay.apk)
· [Release notes and older versions](https://github.com/signde/android-video-overlay/releases)

**Downloader code: `3517057`** · [aftv.news/3517057](https://aftv.news/3517057)

Enter the code in Downloader to download the latest APK. The code and short URL
point to the direct APK link above and stay the same for future releases.

![Android Video Info Overlay during Dolby surround sample playback](docs/images/overlay-dolby-sample.png)

## Verified devices

- onn 4K Pro v2, Android TV 14
- onn 4K Plus, Android TV 14
- onn 4K Pro (2024), v21.1 (user-reported)
- onn 4K Streaming Device (2026), Android TV 14
- RockTek GX1, Android TV 14 (user-reported)
- Google TV Streamer 4K (contributor-reported)

Available metrics vary by device and playback app. Unsupported readings show
as unavailable; HDMI HDR output is not yet available on the tested Realtek devices.

## Setup and use

See the [installation guide](INSTALL_GUIDE.md) for guided setup, Downloader
installation, and help if the Accessibility toggle immediately switches off.

1. Install the APK and enable **Developer options > USB debugging** on the TV.
2. Open **Android Video Info Overlay** and follow its guided setup to authorize
   local ADB and allow restricted settings where required. Approve the TV's debugging prompt
   only if you trust this app. No computer commands are needed on supported devices.
3. Use the setup screen to enable **Android Video Info Overlay** in Accessibility,
   return with Back, and select **Show overlay and finish**. Leave debugging enabled
   on a trusted network. The app needs local ADB on port 5555; no root or any other
   helper is needed. Wireless debugging pairing alone is not sufficient.
4. Press the remote's **Favorites / Bookmark** button to show or hide the overlay.
   You can also open **Android Video Info Overlay** from the apps list to toggle it.

For appearance and remote-button settings, open **Settings > Accessibility >
Android Video Info Overlay > Configuration**, or **Overlay configuration** at the
bottom of the setup screen. Revisit setup through **Configuration > Setup / permissions**.

## Changelog

### 21.2.1

- Fix Android 11/12 setup requesting an unsupported permission.
- Prevent crashes when CPU frequency readings are unavailable.
- Use “Android Video Info Overlay” consistently in the app and documentation.

### 21.2

- Add guided, on-device ADB setup and a new app icon.
- Improve Realtek video detection, stream HDR, and audio channel reporting.
- Add a permanent latest-APK link and Downloader code.

### 21.1

- Add Google TV Streamer support and improve device-specific metric fallbacks.

### 21.0

- Initial experimental release for onn devices, with configurable remote toggling.
