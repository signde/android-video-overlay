# Publishing releases

The app remains experimental. Public releases use versioned tags (for example,
`v21.2`) and are marked **Latest**, not GitHub prereleases, so the permanent APK
link resolves. Use prereleases only for separate testing builds that should not
replace the Downloader target.

## Permanent Downloader target

```text
https://github.com/signde/android-video-overlay/releases/latest/download/Android_VideoInfoOverlay.apk
```

The existing Downloader code is **`3517057`**, with short URL
[aftv.news/3517057](https://aftv.news/3517057), pointing to the destination above.
Keep this code for future releases. Do not change its target to a version-specific
tag, release-page URL, or temporary asset-storage redirect.
GitHub's [latest asset link](https://docs.github.com/en/repositories/releasing-projects-on-github/linking-to-releases)
selects the identically named asset on the latest release. No moving `latest`
tag or duplicate release is necessary.

## Release checklist

1. Increment `versionCode` and `versionName` in `app/build.gradle`. Update the
   README and both languages of the installation guide if setup changes.
2. Run `testDebugUnitTest assembleDebug` with JDK 17 and the Android SDK. Review
   lint separately and smoke-test the resulting APK on a supported device.
3. Preserve the existing signing identity for in-place updates. Published builds
   currently use the debug build variant; do not silently switch to a different
   key. Verify the APK with `apksigner verify --verbose --print-certs`. Its signer
   SHA-256 must match:

   ```text
   bce0938102d4c22016401c6c661dc1787aa38352a2da54a073001af802026971
   ```

   Keep signing keys private and backed up outside Git. A fresh machine's default
   debug key will not match existing installations.
4. Make two byte-identical copies of the verified APK for release assets:
   `Android_VideoInfoOverlay_<version>.apk` and `Android_VideoInfoOverlay.apk`.
   Generate `SHA256SUMS.txt` covering both. Do not commit APKs or keys to Git.
5. Verify Git author/committer identity, commit, tag the exact tested source, and
   push the commit and version tag. Never move a published version tag.
6. Create a draft release for that tag, upload both APKs and the checksums, then
   publish it as **Latest**. Keep notes concise, retain the experimental warning,
   and distinguish user/contributor-reported devices from local testing.
7. Download the APK through the permanent URL and verify its checksum matches
   the versioned asset before announcing it. Confirm `/releases/latest` resolves
   to the intended tag. Keep older releases and their assets intact.

Downloader still needs permission to install unknown apps. Downloading an APK
does not grant the overlay Accessibility or ADB permissions; the app's guided
setup handles the user-approved steps on supported devices. File-manager
sideload setup has been tested; this is not a claim of an end-to-end Downloader
app test.
