package com.signde.androidvideooverlay

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** A launchable, remote-mapper-friendly toggle that immediately returns to playback. */
class ToggleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val service = MainActivity.activeService
        if (service != null) {
            service.toggleOverlay()
        } else {
            startActivity(Intent(this, SetupActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        finish()
        overridePendingTransition(0, 0)
    }
}
