package tn.keeftalk.chat.services

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.util.SoundEffectManager

private const val TAG = "KeeftalkSmsStatusReceiver"

class SmsStatusReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val messageId = intent.getLongExtra("message_id", -1L)
        val resultCode = resultCode

        if (action == "SMS_DELIVERED") {
            SoundEffectManager.playMediaSound(context, SoundEffectManager.RES_MSG_SEEN_CHAT)
        }

        scope.launch {
            try {
                if (messageId != -1L) {
                    val statusValue = if (action == "SMS_DELIVERED") {
                        Telephony.Sms.STATUS_COMPLETE
                    } else {
                        if (resultCode == Activity.RESULT_OK) Telephony.Sms.STATUS_NONE else Telephony.Sms.STATUS_FAILED
                    }

                    try {
                        val values = ContentValues().apply {
                            put(Telephony.Sms.STATUS, statusValue)
                        }
                        context.contentResolver.update(
                            Telephony.Sms.CONTENT_URI,
                            values,
                            "${Telephony.Sms._ID} = ?",
                            arrayOf(messageId.toString())
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating status in system provider for message $messageId", e)
                    }
                }

                AppModule.provideTelephonySyncManager(context).syncThreads()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling status broadcast", e)
            }
        }
    }
}
