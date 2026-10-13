# Wayne AV detail testing, 2026-10-13

Device: onn 4K Streaming Device (`wayne`), Realtek RTD1325, Android TV 14.
Updated the existing installation over ADB without changing its permissions or
Accessibility configuration. No release, commit, or push was made.

## Changes and results

- Identify video codecs from the full decoder name. The observed
  `c2.realtek.video.hevc.main10.decoder` now displays **H.265 / HEVC**.
- Display **Stream HDR** separately from **HDR Output**. Unknown output is shown
  as **Unavailable**, not silently omitted or inferred from stream metadata.
- Scope Codec2 transfer parsing to the active video decoder's allocation/thread;
  do not reuse other decoder threads or unscoped history. Missing logs remain
  unknown. The field follows the existing HDR visibility preference.
- Display **Audio Channels** from selected source/input-format metadata or the
  sole active audio decoder's compressed input configuration. Never substitute
  HAL/output channels. Later format changes invalidate the configuration fallback.
  Counts are not assumed to mean a particular speaker layout. This field follows
  the existing audio-format visibility preference, as audio bitrate already does.
- Recognize Emby's `audio/true-hd` MIME name.
- Realtek color output comes only from `[RTKDRM] crnt-fmt:` in SurfaceFlinger,
  not EDID capabilities or the proposed `gpt-fmt` entry. Wayne reports YCbCr 4:2:0
  (10 Bit). This fallback runs only on Realtek hardware.

### On-device checks

- Emby Dolby Escape HEVC UHD HDR10 TrueHD 7.1 sample: HEVC, 3840 x 2160,
  Stream HDR10, Dolby TrueHD, Direct / offload, **8 channels**.
- ESPN home preview and live playback: AVC/SDR and working resolution/bitrate
  updates. Final build displayed AAC, Decoded PCM, **2 channels**, and Realtek
  color output. Source HDR from the earlier Emby sample did not leak into SDR.
- The ESPN item selected was advertised as 4K HDR, but the playback diagnostics
  observed during this session were AVC/SDR. This is **not** an ESPN HDR validation.
- Stopping/changing playback cleared unavailable media fields.
- No `PortableMediaInfo` warning or `AndroidRuntime` error was observed in the
  final targeted log check. 35 unit tests pass; APK assembly and diff checks pass.
- Screenshots during hardware video playback omit the video plane and sometimes
  show only overlay damage regions. They are not evidence that the TV is black.

## Remaining limitations

Live HDMI HDR output remains unverified/unavailable on this Realtek firmware.
Vendor sysfs interfaces were permission-restricted. SurfaceFlinger's current HDR
setting is `HDR_MODE_INPUT_FW_AUTO`, a policy, not a live HDR10/HLG/DV measurement.
No output status is inferred from that setting, 10-bit color, or stream HDR.
Some players do not expose source channels; unavailable is intentional in those
cases. Log history loss can also make stream details unavailable.

Codec2 transfer values were checked against
[AOSP C2Config.h](https://android.googlesource.com/platform/frameworks/av/+/044eaa62c79775870cbf08e323b152752edcfb9d/media/codec2/core/include/C2Config.h).
HLG and stale/ambiguous metadata are regression-tested, not validated by HLG
playback on this device. Other devices were not accessed in this session.

## ESPN channel parsing follow-up

A later live ESPN capture on wayne confirmed HEVC 3840 x 2160 at 59.94 fps,
HDR10 source metadata, and a selected six-channel E-AC3 track. ESPN's subsequent
input-format message reported 256 kbps but omitted channels, superseding the
otherwise useful track listing in our parser.

The parser now fills that missing field only from an unambiguous selection with
matching MIME and language. It rejects same-format alternatives, intervening
audio format changes, unsupported selections, and explicit unknown counts.
There is no persistent metadata cache or HAL-channel substitution. HDR output
detection is unchanged and remains unavailable on wayne.

All 50 unit tests pass. Replaying the actual captured logs through the updated
parser returns **6 channels**, **256 kbps**, **16.15 Mbps**, and **Stream HDR10**,
with HDR output unknown. The signed `av-preview_2026-10-13-v3` APK was built and
verified but not installed during this follow-up, to avoid disturbing playback.
