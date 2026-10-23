package tn.keeftalk.chat.ui

sealed class LogoutState {
    data object Idle : LogoutState()
    data object Confirming : LogoutState()
    data object Deleting : LogoutState()
    data class Error(val message: String) : LogoutState()
}
