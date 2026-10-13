package com.signde.androidvideooverlay.settings

import android.content.Intent
import android.os.Bundle
import com.signde.androidvideooverlay.MainActivity
import com.signde.androidvideooverlay.ToggleActivity
import com.signde.androidvideooverlay.SetupActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.signde.androidvideooverlay.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Older onn Favorites assignments may retain this former launcher
        // component. Preserve that assignment, but give it toggle behavior.
        if (intent.action == Intent.ACTION_MAIN &&
            (intent.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) ||
                intent.hasCategory(Intent.CATEGORY_LAUNCHER))) {
            startActivity(Intent(this, ToggleActivity::class.java))
            finish()
            return
        }
        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private var overlayOptionsPreference: Preference? = null
        private var backgroundOptionsPreference: Preference? = null
        private var distanceOptionsPreference: Preference? = null
        private var cornerOptionsPreference: Preference? = null
        private var textOptionsPreference: Preference? = null
        private var replacementOptionsPreference: Preference? = null
        private var hideOptionsPreference: Preference? = null
        private var aboutOptionsPreference: Preference? = null

        private fun openSetup() {
            startActivity(Intent(requireContext(), SetupActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.activity_settings, rootKey)

            findPreference<Preference>("toggle_overlay")?.setOnPreferenceClickListener {
                val service = MainActivity.activeService
                if (service == null) openSetup()
                else service.toggleOverlay()
                true
            }

            findPreference<Preference>("setup_permissions")?.setOnPreferenceClickListener {
                openSetup()
                true
            }

            overlayOptionsPreference = findPreference("overlay_options_activity")
            backgroundOptionsPreference = findPreference("background_options_activity")
            distanceOptionsPreference = findPreference("distance_options_activity")
            cornerOptionsPreference = findPreference("corner_options_activity")
            textOptionsPreference = findPreference("text_options_activity")
            replacementOptionsPreference = findPreference("replacement_options_activity")
            hideOptionsPreference = findPreference("hide_options_activity")
            aboutOptionsPreference = findPreference("about_options_activity")

            overlayOptionsPreference?.setOnPreferenceClickListener {
                val intent = Intent(activity, OverlaySettingsActivity::class.java)
                startActivity(intent)
                true
            }

            backgroundOptionsPreference?.setOnPreferenceClickListener {
                val intent = Intent(activity, BackgroundSettingsActivity::class.java)
                startActivity(intent)
                true
            }

            distanceOptionsPreference?.setOnPreferenceClickListener {
                val intent = Intent(activity, DistanceSettingsActivity::class.java)
                startActivity(intent)
                true
            }

            cornerOptionsPreference?.setOnPreferenceClickListener {
                val intent = Intent(activity, CornerSettingsActivity::class.java)
                startActivity(intent)
                true
            }

            textOptionsPreference?.setOnPreferenceClickListener {
                val intent = Intent(activity, TextSettingsActivity::class.java)
                startActivity(intent)
                true
            }

            replacementOptionsPreference?.setOnPreferenceClickListener {
                val intent = Intent(activity, ReplacementSettingsActivity::class.java)
                startActivity(intent)
                true
            }

            hideOptionsPreference?.setOnPreferenceClickListener {
                val intent = Intent(activity, HideSettingsActivity::class.java)
                startActivity(intent)
                true
            }

            aboutOptionsPreference?.setOnPreferenceClickListener {
                val intent = Intent(activity, AboutSettingsActivity::class.java)
                startActivity(intent)
                true
            }
        }
    }
}
