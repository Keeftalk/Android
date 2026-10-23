package tn.keeftalk.chat.ui.call

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.ui.screens.CallScreen
import tn.keeftalk.chat.ui.screens.CallViewModel
import tn.keeftalk.chat.ui.theme.KeeftalkTheme

class IncomingCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showOnLockScreen()
        wakeScreen()

        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: finish().run { return }
        val action = intent.getStringExtra("action") ?: intent.action

        setContent {
            val repository = AppModule.provideChatRepository(applicationContext)
            val callViewModel: CallViewModel = viewModel(key = callId) {
                CallViewModel(application, repository, callId, false)
            }

            if (action == "answer") {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    callViewModel.acceptCall()
                }
            } else if (action == "decline" || action == "ACTION_CANCEL_CALL") {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    callViewModel.rejectCall()
                    finish()
                }
            }

            KeeftalkTheme {
                CallScreen(
                    viewModel = callViewModel,
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val action = intent.getStringExtra("action") ?: intent.action
        if (action == "ACTION_CANCEL_CALL" || action == "decline") {
            finish()
        }
    }

    private fun wakeScreen() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isInteractive) {
            @Suppress("DEPRECATION")
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Keeftalk:CallWakeLock"
            )
            wakeLock.acquire(10000L) // Wake for 10 seconds
        }
    }

    private fun showOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
        
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
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            }
            context.startActivity(intent)
        }
    }
}
