# G10 Android 12 setup patch

Tested 2026-10-13 with local version 21.2.1 (version code 213).
This records the pre-release test build; the release also includes the subsequent
display-name spacing and documentation updates.

## Device

- TCL Q51K, device codename G10, product model `Smart TV`.
- Android 12 / API 31, Realtek RTD2875P.
- Firmware: `TCL/G10_4K_US_NF/G10:12/STT2.230203.001/AS108:user/release-keys`.

## Changes and results

- Android 11/12 now run a local ADB shell probe instead of requesting the
  nonexistent `ACCESS_RESTRICTED_SETTINGS` app-op. Android 13+ retain the grant
  command and permission read-back verification.
- Installed via computer ADB with no previous overlay installation. This was
  not a Downloader/file-manager installation test.
- Step 2 displayed **Authorize local ADB**, requested approval for the app's own
  ADB key, and reported **Local ADB connected and verified** after the TV prompt
  was accepted. Accessibility was enabled through the TV settings UI.
- Initial overlay launch exposed a separate CPU-frequency crash: failed sysfs
  reads shortened one frequency list and caused an out-of-bounds access.
  The patch preserves a slot for every core and displays **Unavailable** when
  required readings are missing. Valid CPU readings keep the existing formatting.
- After updating the APK and restarting only the overlay accessibility service,
  setup, overlay display, and launcher toggling worked. Projectivy's existing
  accessibility service remained enabled.
- Peacock's **Preseason: Cavaliers vs. Magic** reported active playback
  (`state=3`, speed 1.0) with the overlay running. The overlay process remained
  alive across the smoke test, with no new AndroidRuntime crash recorded.
- G10 exposed display resolution, a generic Realtek secure-decoder name, PCM
  output, and basic system information. Video resolution, bitrates, channels,
  HDR, and default CPU clock statistics were absent or unavailable. This is a
  setup/stability test, not validation of full G10 playback-metric support.
- Emby was not configured on this TV, so no Emby sample test was performed.

## Regression coverage and limits

- All 58 unit tests passed, including API 30/31/32 legacy setup, API 33-36
  permission verification, rejected failed/partial responses, and missing,
  unreadable, malformed, and valid CPU frequency inputs.
- APK signature verified against the existing signing certificate.
- No physical Android 11 Shield test or Android 13+ device retest was performed
  for this patch. The permission fix does not establish Shield metric support.

Tested APK SHA-256 (before the subsequent display-name spacing update):

```text
7f88dd7e45e851a54bcdb3c2d00558767237b24369f3efbbe039a8babe742cb4
```
