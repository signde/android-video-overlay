# Peacock resolution fallback, 2026-10-13

## Scope and safeguards

- Enabled only for Realtek hardware and the YUV buffer format (`0x77`) observed
  on wayne. Other manufacturers never invoke the fallback's SurfaceFlinger read.
- Existing codec/track resolution wins unchanged. No new resolution source is
  substituted on Amlogic or MediaTek devices.
- Requires exactly one active video decoder, a freshly resolved player package,
  and one visible SurfaceView video buffer owned by that exact package.
- Ignores UI/background/RGB layers, display rectangles, cached buffers, hidden
  surfaces, zero/invalid dimensions, other apps and ambiguous multiple surfaces.
- Rechecks resource-manager state after the compositor read. Changed state is
  discarded; no previous buffer size is cached across playback stops or changes.
- Read/parse failure preserves existing AV fields. A new buffer size does not
  inherit old frame-rate or bitrate metadata.
- UI label is **Video Buffer**, not **Video Resolution**, when this fallback is
  used. This is a submitted buffer size, which can differ from encoded/cropped
  content dimensions; it is not the HDMI output resolution.

## Verification

- 45 unit tests pass, including 10 fallback-specific tests. APK assembly and
  `git diff --check` pass.
- Ran the compiled parser against the exact previously captured live Peacock
  SurfaceFlinger dump: **1920 x 1080**. No need to infer size from a 4K display.
- Installed the build on wayne. Accessibility reconnection briefly routed the
  launcher to setup, taking Peacock out of playback. Closed setup and resumed
  the same movie. Fresh codec metadata then populated the existing resolution
  path, verifying that it retains precedence.
- Other devices were not accessed or physically retested. Their exclusion is
  covered by unit tests, not a claim of new on-device validation.

The fallback remains deliberately conservative. It does not infer missing HDR,
audio channels, frame rate, or bitrate from video-buffer dimensions.
