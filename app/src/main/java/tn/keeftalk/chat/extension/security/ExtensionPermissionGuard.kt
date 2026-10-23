package tn.keeftalk.chat.extension.security

import android.util.Log
import tn.keeftalk.chat.extension.model.ExtensionManifest

class ExtensionPermissionGuard(private val manifest: ExtensionManifest?) {
    private val TAG = "ExtensionPermissionGuard"

    fun hasPermission(permission: String): Boolean {
        if (manifest == null) return false
        val cleanPermission = permission.trim().uppercase()
        val declared = manifest.permissions.map { it.trim().uppercase() }

        // Check exact match or wildcard
        val allowed = declared.contains(cleanPermission) || declared.contains("ALL") || declared.contains("INTERNET_ALL")
        if (!allowed) {
            Log.w(TAG, "Extension '${manifest.id}' attempted privileged operation requiring permission '$permission' without explicit grant in manifest.")
        }
        return allowed
    }

    fun requirePermission(permission: String) {
        if (!hasPermission(permission)) {
            throw SecurityException("Extension '${manifest?.id ?: "unknown"}' denied access to restricted capability requiring permission: '$permission'.")
        }
    }
}
