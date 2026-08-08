package com.keeftalk.chat.ui.call

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.domain.model.CallState
import com.keeftalk.chat.ui.screens.CallScreen
import com.keeftalk.chat.ui.screens.CallViewModel
import com.keeftalk.chat.ui.theme.KeeftalkTheme

class IncomingCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showOnLockScreen()

        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: finish().run { return }

        setContent {
            val repository = AppModule.provideChatRepository(applicationContext)
            val callViewModel: CallViewModel = viewModel(key = callId) {
                CallViewModel(application, repository, callId, false)
            }

            KeeftalkTheme {
                CallScreen(
                    viewModel = callViewModel,
                    onDismiss = { finish() }
                )
            }
            
            // If the call is accepted, transition to CallActivity (or just stay here since CallScreen handles it)
            // But usually we want to keep them in separate activities if we follow the user's request.
            // However, CallScreen handles the transition from RINGING to ACTIVE_CALL internally.
        }
    }

    private fun showOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }

    companion object {
        private const val EXTRA_CALL_ID = "extra_call_id"

        fun start(context: Context, callId: String) {
            val intent = Intent(context, IncomingCallActivity::class.java).apply {
                putExtra(EXTRA_CALL_ID, callId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            }
            context.startActivity(intent)
        }
    }
}
