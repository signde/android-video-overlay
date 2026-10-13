package com.signde.androidvideooverlay

/** Require both successful command completion and read-back, not just an empty shell response. */
object SetupPermissionResult {
    fun isAllowed(output: String): Boolean =
        output.lineSequence().any { it.trim() == "__OVERLAY_SETUP_EXIT__0" } &&
            Regex("(?m)^\\s*ACCESS_RESTRICTED_SETTINGS: allow(?:[;\\s].*)?$").containsMatchIn(output)
}
