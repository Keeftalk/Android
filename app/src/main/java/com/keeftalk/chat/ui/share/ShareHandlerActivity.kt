package com.keeftalk.chat.ui.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.ui.theme.KeeftalkTheme

class ShareHandlerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val isVaultFlow = intent.component?.className?.contains("VaultShareTarget") == true
        
        setContent {
            KeeftalkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ShareViewModel = viewModel(
                        factory = ShareViewModel.Factory(
                            application = application,
                            chatRepository = AppModule.provideChatRepository(this),
                            vaultRepository = AppModule.provideVaultRepository(this)
                        )
                    )
                    
                    LaunchedEffect(intent) {
                        viewModel.handleIntent(intent, isVaultFlow)
                    }

                    LaunchedEffect(Unit) {
                        viewModel.navigationEvent.collect { event ->
                            when (event) {
                                is ShareNavigationEvent.OpenEditor -> {
                                    val mainIntent = Intent(this@ShareHandlerActivity, com.keeftalk.chat.MainActivity::class.java).apply {
                                        action = "SHARE_TO_CHAT"
                                        putExtra("chatId", event.chatId)
                                        putParcelableArrayListExtra("mediaItems", ArrayList(event.mediaItems))
                                        putParcelableArrayListExtra("documentModels", ArrayList(event.documentModels))
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    }
                                    startActivity(mainIntent)
                                    finish()
                                }
                            }
                        }
                    }
                    
                    ShareScreen(
                        viewModel = viewModel,
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}
