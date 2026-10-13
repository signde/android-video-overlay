package com.signde.androidvideooverlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class SetupPermissionResultTest {
    private val packageName = "com.signde.androidvideooverlay"

    @Test fun android11And12ProbeAdbWithoutRestrictedSettings() {
        for (sdk in listOf(30, 31, 32)) {
            assertFalse(SetupPermissionResult.requiresRestrictedSettings(sdk))
            assertEquals("echo __OVERLAY_ADB_READY__; echo __OVERLAY_SETUP_EXIT__\$?",
                SetupPermissionResult.command(packageName, sdk))
            assertTrue(SetupPermissionResult.isSuccessful("__OVERLAY_ADB_READY__\r\n__OVERLAY_SETUP_EXIT__0\r\n", sdk))
        }
    }

    @Test fun android13AndNewerStillRequireVerifiedPermission() {
        for (sdk in listOf(33, 34, 35, 36)) {
            assertTrue(SetupPermissionResult.requiresRestrictedSettings(sdk))
            assertEquals("appops set $packageName ACCESS_RESTRICTED_SETTINGS allow && " +
                "appops get $packageName ACCESS_RESTRICTED_SETTINGS; echo __OVERLAY_SETUP_EXIT__\$?",
                SetupPermissionResult.command(packageName, sdk))
            assertTrue(SetupPermissionResult.isSuccessful("ACCESS_RESTRICTED_SETTINGS: allow\n__OVERLAY_SETUP_EXIT__0", sdk))
            assertFalse(SetupPermissionResult.isSuccessful("__OVERLAY_ADB_READY__\n__OVERLAY_SETUP_EXIT__0", sdk))
            assertFalse(SetupPermissionResult.isSuccessful("Unknown operation string: ACCESS_RESTRICTED_SETTINGS\n__OVERLAY_SETUP_EXIT__255", sdk))
        }
    }

    @Test fun legacyDoesNotAcceptMissingFailedOrUnexpectedShellResponses() {
        for (sdk in listOf(30, 31, 32)) {
            listOf("", "__OVERLAY_ADB_READY__", "__OVERLAY_SETUP_EXIT__0",
                "__OVERLAY_ADB_READY__\n__OVERLAY_SETUP_EXIT__1",
                "ACCESS_RESTRICTED_SETTINGS: allow\n__OVERLAY_SETUP_EXIT__0",
                "Unknown operation string: ACCESS_RESTRICTED_SETTINGS\n__OVERLAY_SETUP_EXIT__255",
                "error\n__OVERLAY_ADB_READY__\n__OVERLAY_SETUP_EXIT__0"
            ).forEach { assertFalse(it, SetupPermissionResult.isSuccessful(it, sdk)) }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOtherPackages() {
        SetupPermissionResult.command("another.app; echo injected", 34)
    }

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
