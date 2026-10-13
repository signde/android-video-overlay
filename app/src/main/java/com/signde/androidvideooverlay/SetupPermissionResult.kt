package com.signde.androidvideooverlay

/** Require both successful command completion and read-back, not just an empty shell response. */
object SetupPermissionResult {
    fun requiresRestrictedSettings(sdkInt: Int): Boolean = sdkInt >= 33

    fun command(packageName: String, sdkInt: Int): String {
        require(packageName == "com.signde.androidvideooverlay")
        // Android 11/12 have no restricted-settings app-op. Still execute a
        // shell probe so skipping that app-op never skips ADB authorization.
        val operation = if (requiresRestrictedSettings(sdkInt))
            "appops set $packageName ACCESS_RESTRICTED_SETTINGS allow && " +
                "appops get $packageName ACCESS_RESTRICTED_SETTINGS"
        else "echo __OVERLAY_ADB_READY__"
        return "$operation; echo __OVERLAY_SETUP_EXIT__\$?"
    }

    fun isSuccessful(output: String, sdkInt: Int): Boolean =
        if (requiresRestrictedSettings(sdkInt)) isAllowed(output)
        else output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList() ==
            listOf("__OVERLAY_ADB_READY__", "__OVERLAY_SETUP_EXIT__0")

    fun isAllowed(output: String): Boolean =
        output.lineSequence().any { it.trim() == "__OVERLAY_SETUP_EXIT__0" } &&
            Regex("(?m)^\\s*ACCESS_RESTRICTED_SETTINGS: allow(?:[;\\s].*)?$").containsMatchIn(output)
}
