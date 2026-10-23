package tn.keeftalk.chat.account

import android.app.Service
import android.content.Intent
import android.os.IBinder

class KeeftalkAccountAuthenticatorService : Service() {

    private lateinit var authenticator: KeeftalkAbstractAccountAuthenticator

    override fun onCreate() {
        super.onCreate()
        authenticator = KeeftalkAbstractAccountAuthenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return authenticator.iBinder
    }
}
