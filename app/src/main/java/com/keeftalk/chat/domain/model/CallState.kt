package com.keeftalk.chat.domain.model

enum class CallState {
    IDLE,
    OUTGOING_RINGING,
    INCOMING_RINGING,
    CONNECTING,
    ACTIVE_CALL,
    RECONNECTING,
    ENDED
}
