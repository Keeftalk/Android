package tn.keeftalk.chat.extension

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tn.keeftalk.chat.extension.persistence.ExtensionStorageManager

class ExtensionStorageManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testIsolatedStoragePreferences() {
        val context = FakeTestContext(tempFolder.root)
        val manager = ExtensionStorageManager(context)

        val extId1 = "com.keeftalk.ext.one"
        val extId2 = "com.keeftalk.ext.two"

        // Initially empty
        assertNull(manager.getPreference(extId1, "theme_mode"))

        // Set preferences for ext 1
        manager.setPreference(extId1, "theme_mode", "dark")
        manager.setPreference(extId1, "font_size", "16")

        assertEquals("dark", manager.getPreference(extId1, "theme_mode"))
        assertEquals("16", manager.getPreference(extId1, "font_size"))

        // Ext 2 should not see ext 1's preferences
        assertNull(manager.getPreference(extId2, "theme_mode"))

        // Clear preferences for ext 1
        manager.clearPreferences(extId1)
        assertNull(manager.getPreference(extId1, "theme_mode"))
        assertNull(manager.getPreference(extId1, "font_size"))
    }
}
