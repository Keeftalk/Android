package com.keeftalk.chat.services

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import com.keeftalk.chat.MainActivity

class ComposeSmsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val data = intent.data
        var address = ""
        if (data != null && (data.scheme == "sms" || data.scheme == "smsto")) {
            address = data.schemeSpecificPart
        }

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("screen", "sms_detail")
            putExtra("address", address)
        }
        startActivity(mainIntent)
        finish()
    }
}
