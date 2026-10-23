package tn.keeftalk.chat.extension

import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.extension.model.*
import tn.keeftalk.chat.extension.security.ExtensionPermissionGuard

class ExtensionPermissionGuardTest {

    @Test
    fun testPermissionEnforcement() {
        val manifest = ExtensionManifest(
            id = "com.test.ext",
            type = ExtensionType.TOOL_WIDGET,
            name = "Test Ext",
            version = "1.0.0",
            entry = "entry.json",
            permissions = listOf("LOCATION_COARSE", "INTERNET")
        )

        val guard = ExtensionPermissionGuard(manifest)

        assertTrue(guard.hasPermission("LOCATION_COARSE"))
        assertTrue(guard.hasPermission("internet"))
        assertFalse(guard.hasPermission("CAMERA"))

        // Test requirePermission
        guard.requirePermission("location_coarse")

        try {
            guard.requirePermission("RECORD_AUDIO")
            fail("Expected SecurityException for ungranted permission")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("RECORD_AUDIO"))
        }
    }
}
