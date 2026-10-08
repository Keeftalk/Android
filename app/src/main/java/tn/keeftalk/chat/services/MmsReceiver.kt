package tn.keeftalk.chat.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import tn.keeftalk.chat.di.AppModule

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // MMS WAP PUSH received, trigger a full sync to fetch the new MMS
        AppModule.provideTelephonySyncManager(context).syncThreads()
    }
}
