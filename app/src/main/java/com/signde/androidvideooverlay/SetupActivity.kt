package com.signde.androidvideooverlay

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.signde.androidvideooverlay.settings.SettingsActivity
import java.util.concurrent.Executors

/** TV-remote-friendly setup that works before the accessibility service can start. */
class SetupActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var client: SetupAdbClient? = null
    private lateinit var status: TextView
    private lateinit var serviceStatus: TextView
    private lateinit var authorize: Button
    private lateinit var showOverlay: Button
    private var busy = false
    private val refresh = object : Runnable {
        override fun run() {
            val ready = MainActivity.activeService != null
            serviceStatus.setText(if (ready) R.string.setup_service_ready else R.string.setup_service_waiting)
            showOverlay.isEnabled = ready && !busy
            handler.postDelayed(this, 500)
        }
    }
    private val timeout = Runnable {
        client?.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(40), dp(24), dp(40), dp(24))
        }
        val scroll = ScrollView(this).apply { addView(layout) }
        setContentView(scroll)
        fun label(text: Int, size: Float = 17f): TextView = TextView(this).apply {
            setText(text)
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(0, dp(6), 0, dp(6))
            layout.addView(this)
        }
        fun button(text: Int, action: () -> Unit): Button = Button(this).apply {
            setText(text)
            isAllCaps = false
            layout.addView(this, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))
            setOnClickListener { action() }
        }
        label(R.string.setup_title, 25f)
        label(R.string.setup_intro)
        button(R.string.setup_developer_button) { openSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) }
        authorize = button(R.string.setup_authorize_button) {
            AlertDialog.Builder(this)
                .setTitle(R.string.setup_consent_title)
                .setMessage(R.string.setup_consent_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.setup_consent_allow) { _, _ -> beginAuthorization() }
                .show()
        }
        status = label(R.string.setup_not_authorized)
        button(R.string.setup_accessibility_button) { openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) }
        serviceStatus = label(R.string.setup_service_waiting)
        showOverlay = button(R.string.setup_finish_button) {
            MainActivity.activeService?.let {
                it.toggleOverlay()
                finish()
            }
        }
        showOverlay.isEnabled = false
        button(R.string.setup_configuration_button) {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        authorize.requestFocus()
    }

    private fun beginAuthorization() {
        if (busy) return
        busy = true
        authorize.isEnabled = false
        showOverlay.isEnabled = false
        status.setText(R.string.setup_authorizing)
        val attempt = SetupAdbClient()
        client = attempt
        // Bound the entire attempt, including a stalled shell command, not just the handshake.
        handler.postDelayed(timeout, 85000)
        executor.execute {
            val result = runCatching {
                val output = attempt.allowRestrictedSettings(filesDir, packageName)
                check(SetupPermissionResult.isAllowed(output)) {
                    "Device did not confirm permission: ${output.trim().take(500)}"
                }
            }
            runOnUiThread {
                if (isDestroyed || isFinishing) return@runOnUiThread
                handler.removeCallbacks(timeout)
                client = null
                busy = false
                authorize.isEnabled = true
                result.fold(
                    onSuccess = {
                        Log.i("OverlaySetup", "Restricted settings allowed and verified through local ADB")
                        status.setText(R.string.setup_authorized)
                    },
                    onFailure = {
                        Log.w("OverlaySetup", "Local ADB setup failed", it)
                        status.text = getString(R.string.setup_failed, it.message ?: it.javaClass.simpleName)
                    }
                )
            }
        }
    }

    private fun openSettings(action: String) {
        runCatching { startActivity(Intent(action)) }.onFailure {
            status.setText(R.string.setup_settings_unavailable)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refresh)
        handler.post(refresh)
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        client?.close()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
