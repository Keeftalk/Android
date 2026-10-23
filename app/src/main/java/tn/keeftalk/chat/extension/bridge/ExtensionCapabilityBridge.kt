package tn.keeftalk.chat.extension.bridge

import android.content.Context
import android.util.Log
import tn.keeftalk.chat.extension.model.ExtensionManifest
import tn.keeftalk.chat.extension.persistence.ExtensionStorageManager
import tn.keeftalk.chat.extension.security.ExtensionPermissionGuard

class ExtensionCapabilityBridge(
    private val context: Context,
    private val manifest: ExtensionManifest?
) {
    private val TAG = "ExtensionCapabilityBridge"
    private val permissionGuard = ExtensionPermissionGuard(manifest)
    private val storageManager = ExtensionStorageManager(context)

    fun requestStorageAccess(extensionId: String, key: String): String? {
        permissionGuard.requirePermission("STORAGE")
        return storageManager.getPreference(extensionId, key)
    }

    fun writeStorageAccess(extensionId: String, key: String, value: String) {
        permissionGuard.requirePermission("STORAGE")
        storageManager.setPreference(extensionId, key, value)
    }

    fun requestLocationAccess(): Boolean {
        return permissionGuard.hasPermission("LOCATION_COARSE") || permissionGuard.hasPermission("LOCATION_FINE")
    }

    fun requestNotificationAccess(): Boolean {
        return permissionGuard.hasPermission("NOTIFICATIONS")
    }
}
