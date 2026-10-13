package com.signde.androidvideooverlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupPermissionResultTest {
    @Test fun acceptsVerifiedGrant() {
        assertTrue(SetupPermissionResult.isAllowed("ACCESS_RESTRICTED_SETTINGS: allow\r\n__OVERLAY_SETUP_EXIT__0\r\n"))
        assertTrue(SetupPermissionResult.isAllowed("  ACCESS_RESTRICTED_SETTINGS: allow; time=+1s\n__OVERLAY_SETUP_EXIT__0\n"))
    }

    @Test fun rejectsErrorsAndIncompleteResponses() {
        listOf("", "__OVERLAY_SETUP_EXIT__0", "ACCESS_RESTRICTED_SETTINGS: allow",
            "ACCESS_RESTRICTED_SETTINGS: allow\n__OVERLAY_SETUP_EXIT__1",
            "ACCESS_RESTRICTED_SETTINGS: deny\n__OVERLAY_SETUP_EXIT__0",
            "ACCESS_RESTRICTED_SETTINGS: default\n__OVERLAY_SETUP_EXIT__0",
            "Error: unknown operation\n__OVERLAY_SETUP_EXIT__0",
            "ACCESS_RESTRICTED_SETTINGS: allowed\n__OVERLAY_SETUP_EXIT__0"
        ).forEach { assertFalse(it, SetupPermissionResult.isAllowed(it)) }
    }
}
