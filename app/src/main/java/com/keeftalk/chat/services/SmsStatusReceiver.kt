package com.keeftalk.chat.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.keeftalk.chat.di.AppModule

class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Trigger a sync to update local Room status from system provider
        AppModule.provideTelephonySyncManager(context).syncThreads()
    }
}
