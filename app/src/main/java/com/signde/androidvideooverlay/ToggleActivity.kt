package com.signde.androidvideooverlay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

/** A launchable, remote-mapper-friendly toggle that immediately returns to playback. */
class ToggleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val service = MainActivity.activeService
        if (service != null) {
            service.toggleOverlay()
        } else {
            Toast.makeText(this, getString(R.string.enable_accessibility_first, getString(R.string.app_name)), Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        finish()
        overridePendingTransition(0, 0)
    }
}
