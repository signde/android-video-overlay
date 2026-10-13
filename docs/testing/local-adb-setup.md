# Local ADB setup prototype

## Device test, 2026-10-12

- onn 4K Streaming Device, codename `wayne`, Android 14, Realtek RTD1325.
- No overlay installation at the start. Projectivy Accessibility was already enabled.
- Developer options / USB debugging were already enabled for the test connection.
- APK copied to Downloads, then installed through Cx File Explorer and Android's
  package installer. No `adb install` or computer-side permission grant was used
  for the fresh-install proof. The computer drove remote inputs and collected evidence.
- Play Protect's unknown-developer warning required a per-APK **Install anyway**
  confirmation, explicitly approved by the owner. Play Protect was not disabled.

## Observed results

1. Fresh install: `ACCESS_RESTRICTED_SETTINGS: deny`. Enabling Accessibility and
   accepting its confirmation returned the switch to off, reproducing the forum report.
2. Cancelling the app's setup consent left the operation denied.
3. Continuing setup produced Android's **Allow USB debugging?** prompt for the
   app-generated key, independently of the computer's authorization.
4. After selecting **Always allow** and **Allow**, the app ran its own local ADB
   command and read back `ACCESS_RESTRICTED_SETTINGS: allow`.
   The `OverlaySetup` log confirmed successful verification.
5. Enabling Accessibility through the TV settings now stayed on. Projectivy
   remained enabled alongside the overlay. No secure-settings list was overwritten.
6. Returning to setup showed the connected service. **Show overlay and finish**
   displayed the overlay without a second ADB prompt or a required force-stop.
7. A later deliberate force-stop removed the overlay from the enabled Accessibility
   list on this firmware, but preserved its restricted-settings grant and ADB key.
   Repeating setup reconnected without another authorization prompt; enabling the
   service again through the UI worked.
8. The Developer options shortcut worked. The launcher toggle could hide the overlay.
9. After the fresh-install proof, an ADB APK update deployed a small navigation
   change to reuse an existing setup screen. No permission grants were issued
   from the computer. The grant and both accessibility services survived the update.
10. Cx File Explorer's install-unknown-apps permission was restored to its original
    disabled state after testing. The prototype remains installed.

## Scope and limitations

- This proves the app can perform the permission step without a computer granting
  it. A computer was used to stage the APK and automate the test, not to provide
  the app's ADB identity or execute its permission grant.
- File-manager sideload was tested, not Downloader itself. First-time activation
  of Developer options was not tested because debugging was already enabled.
- The app still requires local legacy ADB on port 5555. Wireless TLS pairing alone
  is not supported by the existing playback client.
- This is not full media-metric compatibility certification for the Realtek stick.
  The default Bookmark key did not toggle during injected-key testing; investigate
  separately before claiming remote-button compatibility on this device.
- Setup text currently falls back to English. Unit tests and APK assembly pass;
  lint still reports missing translations and existing TV manifest/resource issues.
- No version bump or release has been made. The local installation guide now
  separates preview setup from the published APK's computer-assisted instructions;
  these documentation changes have not been pushed.
