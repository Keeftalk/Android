package com.keeftalk.chat.feature.email.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.exception.MsalException

class EmailAuthManager(private val context: Context) {

    companion object {
        private const val GMAIL_WEB_CLIENT_ID = "263449157700-5jmpabi45c5m8qkgmb3vsjiu4smfulo1.apps.googleusercontent.com"
    }

    // --- Google OAuth ---

    fun getGoogleSignInIntent(extraScopes: List<String> = emptyList()): Intent {
        android.util.Log.d("EmailAuthManager", "Building GoogleSignInOptions with Web Client ID: $GMAIL_WEB_CLIENT_ID")
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestServerAuthCode(GMAIL_WEB_CLIENT_ID)
            .requestIdToken(GMAIL_WEB_CLIENT_ID)

        if (extraScopes.isEmpty()) {
            builder.requestScopes(Scope("https://www.googleapis.com/auth/gmail.readonly"))
            builder.requestScopes(Scope("https://www.googleapis.com/auth/gmail.send"))
            builder.requestScopes(Scope("https://www.googleapis.com/auth/gmail.modify"))
        } else {
            extraScopes.forEach { builder.requestScopes(Scope(it)) }
        }

        val gso = builder.build()
        val client = GoogleSignIn.getClient(context, gso)
        // Ensure we sign out first to force account picker for testing
        client.signOut()
        return client.signInIntent
    }

    // --- Microsoft OAuth ---

    fun loginMicrosoft(activity: Activity, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            com.keeftalk.chat.R.raw.msal_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    application.signIn(
                        activity,
                        null,
                        arrayOf("https://graph.microsoft.com/Mail.ReadWrite", "https://graph.microsoft.com/Mail.Send"),
                        object : com.microsoft.identity.client.AuthenticationCallback {
                            override fun onSuccess(authenticationResult: com.microsoft.identity.client.IAuthenticationResult) {
                                onSuccess(authenticationResult.accessToken)
                            }

                            override fun onError(exception: MsalException) {
                                onError(exception)
                            }

                            override fun onCancel() {
                                onError(Exception("Cancelled"))
                            }
                        }
                    )
                }

                override fun onError(exception: MsalException) {
                    onError(exception)
                }
            }
        )
    }
}
