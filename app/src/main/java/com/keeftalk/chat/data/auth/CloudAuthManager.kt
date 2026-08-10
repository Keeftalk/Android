package com.keeftalk.chat.data.auth

import android.app.Activity
import android.app.PendingIntent
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope

class CloudAuthManager(private val context: android.content.Context) {

    companion object {
        private const val TAG = "CloudAuthManager"
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        const val PHOTOS_PICKER_SCOPE = "https://www.googleapis.com/auth/photospicker.mediaitems.readonly"
        const val DROPBOX_APP_KEY = "94qq780h7ci3de5"
    }

    fun startDropboxAuthFlow(activity: Activity) {
        com.dropbox.core.android.Auth.startOAuth2PKCE(
            activity, 
            DROPBOX_APP_KEY, 
            com.dropbox.core.DbxRequestConfig("Keeftalk"),
            listOf("files.metadata.read", "files.content.read")
        )
    }

    fun getDropboxCredential(): com.dropbox.core.oauth.DbxCredential? {
        return com.dropbox.core.android.Auth.getDbxCredential()
    }

    fun startGoogleDrivePickerFlow(
        activity: Activity,
        onResolutionRequired: (PendingIntent) -> Unit,
        onAlreadyAuthorized: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val requestedScopes = listOf(Scope(DRIVE_FILE_SCOPE))
        Log.d(TAG, "Starting Google Drive Picker Flow with scopes: ${requestedScopes.map { it.scopeUri }}")
        
        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            // Ensure the token is limited to the requested scopes and doesn't include previously granted scopes
            .setOptOutIncludingGrantedScopes(true)
            // Force CONSENT prompt to ensure Picker appears
            .setPrompt(AuthorizationRequest.Prompt.CONSENT)
            // Native Picker Trigger
            .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_OAUTH_TRIGGER, "true")
            // Optional: Multi-select support
            .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_ALLOW_MULTIPLE, "true") 
            .build()

        Identity.getAuthorizationClient(activity)
            .authorize(authorizationRequest)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    onResolutionRequired(result.pendingIntent!!)
                } else {
                    onAlreadyAuthorized(result.accessToken ?: "")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Drive Auth Request Failed", e)
                onError(e)
            }
    }

    fun startGooglePhotosAuthFlow(
        activity: Activity,
        onResolutionRequired: (PendingIntent) -> Unit,
        onAlreadyAuthorized: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val requestedScopes = listOf(Scope(PHOTOS_PICKER_SCOPE))
        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()

        Identity.getAuthorizationClient(activity)
            .authorize(authorizationRequest)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    onResolutionRequired(result.pendingIntent!!)
                } else {
                    onAlreadyAuthorized(result.accessToken ?: "")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Photos Auth Request Failed", e)
                onError(e)
            }
    }
}
