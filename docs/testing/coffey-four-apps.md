# Coffey streaming-app smoke tests, 2026-10-13

Device: onn 4K Plus (`coffey`), AMLS905X5M, Android TV 14,
ADB 10.0.0.28:5555. Upgraded package com.signde.androidvideooverlay from
21.0/code 210 to the unreleased 21.1/code 211 preview. Existing accessibility
enablement and preferences survived the update.

APK: `Android_VideoInfoOverlay_av-preview_2026-10-13-v3.apk`

SHA-256: `fdbc57473e98c3e7415f35a806ba5c91bc3b03192e5392fb465bd126ce0255e2`

## Results

| App / playback | Observed overlay | Limitations |
| --- | --- | --- |
| ESPN, NBA Today live | AVC 1920 x 1080 / 59.94 fps; 11.55 Mbps video; Dolby Digital Plus direct/offload, 256 kbps, **6 channels**; HDMI SDR | Stream HDR unavailable. Despite the listing's HDR badge, this session used AVC and SDR output. |
| Peacock, Below Deck S13 E1 (after preroll) | AVC rose from 512 x 288 to 1920 x 1080; decoded PCM output; **2 source channels**; HDMI SDR | Bitrates, source audio format, frame rate, and stream HDR unavailable. Source channels came from AAC codec input logs, not the output channel mask. |
| Prime Video, Carrie episode 1 | HEVC 1920 x 1080; Dolby Digital Plus direct/offload output; **HDR10+ output** | Bitrates, source channels, frame rate, and stream HDR unavailable. Raw audio decoder metadata was not substituted for source channels. |
| FOX One, FS1's First Things First live | AVC 1920 x 1080; AAC, decoded PCM, **2 source channels**; HDMI SDR; overlay hide/show worked during playback | Bitrates, frame rate, and stream HDR unavailable. Initial sign-in/entitlement error cleared after user assistance; full-screen playback then succeeded. |

## Checks and caveats

- The display remained at its existing 1920 x 1080 / 60 Hz mode. No display,
  audio, or HDR settings were changed. This was not a 4K playback validation.
- Prime's HDR10+ output matched `/sys/class/amhdmitx/amhdmitx0/hdmi_hdr_status`
  (`HDR10Plus-VSIF`). ESPN/Peacock reported SDR at that same interface.
- Overlay show/hide through ToggleActivity worked across the apps. This did
  not independently validate the user's physical remote mapping.
- After playback stopped, source codec/resolution/audio values cleared and
  HDMI status returned to SDR; Prime's HDR10+ status did not leak into FOX's
  sign-in screen.
- No AndroidRuntime crash or PortableMediaInfo warning was found in the
  inspected retained logs. Android did terminate the hidden overlay once for
  **LOW_MEMORY** while switching from Prime to FOX (14:24:41, 26 MB PSS / 64 MB
  RSS). The accessibility service restarted and rebound automatically, and
  toggling worked afterward. This is a recovery observation, not a crash-free
  process-lifetime claim or proof of its cause relative to earlier builds.
- Protected video is black in ADB screenshots. These captures validate the
  overlay and player UI, not visual rendering quality or audible output.
  FOX One's video was visible in the successful follow-up captures and advanced
  between captures; audio was not independently listened to.
- No old/new APK A/B playback comparison was performed. These are smoke-test
  observations, not a guarantee that every rendition/device behavior is unchanged.
- No code changes, commits, pushes, or releases were made during these tests.
