package com.keeftalk.chat.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class HeadlessSmsSendService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
