package com.keeftalk.chat.ui.call

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.ui.screens.CallScreen
import com.keeftalk.chat.ui.screens.CallViewModel
import com.keeftalk.chat.ui.theme.KeeftalkTheme
import com.keeftalk.chat.domain.model.CallState

class CallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: finish().run { return }
        val isCaller = intent.getBooleanExtra(EXTRA_IS_CALLER, false)

        setContent {
            val repository = AppModule.provideChatRepository(applicationContext)
            val callViewModel: CallViewModel = viewModel(key = callId) {
                CallViewModel(application, repository, callId, isCaller)
            }

            KeeftalkTheme {
                CallScreen(
                    viewModel = callViewModel,
                    onDismiss = { finish() }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_CALL_ID = "extra_call_id"
        private const val EXTRA_IS_CALLER = "extra_is_caller"

        fun start(context: Context, callId: String, isCaller: Boolean) {
            val intent = Intent(context, CallActivity::class.java).apply {
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_IS_CALLER, isCaller)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
