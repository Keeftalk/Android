package com.keeftalk.chat.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.CallState
import com.keeftalk.chat.domain.repository.CallSession
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.util.CallManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.webrtc.Camera1Enumerator
import org.webrtc.PeerConnection
import org.webrtc.VideoCapturer

class CallViewModel(
    application: Application,
    private val repository: ChatRepository,
    private val callId: String,
    private val isCaller: Boolean
) : AndroidViewModel(application) {

    private val callManager = CallManager(application, repository)

    private val _callSession = MutableStateFlow<CallSession?>(null)
    val callSession: StateFlow<CallSession?> = _callSession

    private val _peerUser = MutableStateFlow<com.keeftalk.chat.domain.model.User?>(null)
    val peerUser: StateFlow<com.keeftalk.chat.domain.model.User?> = _peerUser

    val localVideoTrack = callManager.localVideoTrack
    val remoteVideoTrack = callManager.remoteVideoTrack

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled

    private var isWebRTCSetup = false
    private val _permissionsGranted = MutableStateFlow(false)

    init {
        observeCallSession()
        observeConnectionState()
        if (isCaller) {
            startCallFlow()
        }
    }

    fun toggleMic() {
        val newState = !_isMuted.value
        _isMuted.value = newState
        callManager.toggleAudio(!newState)
    }

    fun toggleVideo() {
        val newState = !_isVideoEnabled.value
        _isVideoEnabled.value = newState
        callManager.toggleVideo(newState)
    }

    fun onPermissionsGranted() {
        _permissionsGranted.value = true
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            callManager.connectionState.collect { state ->
                when (state) {
                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        if (_callSession.value?.state == CallState.CONNECTING) {
                            repository.updateCallState(callId, CallState.ACTIVE_CALL)
                        }
                    }
                    PeerConnection.PeerConnectionState.FAILED, PeerConnection.PeerConnectionState.DISCONNECTED -> {
                        if (_callSession.value?.state == CallState.ACTIVE_CALL) {
                            repository.updateCallState(callId, CallState.RECONNECTING)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeCallSession() {
        viewModelScope.launch {
            repository.getCallSession(callId).collect { session ->
                _callSession.value = session
                if (session != null && _peerUser.value == null) {
                    val peerId = if (isCaller) session.receiverId else session.callerId
                    repository.getContact(peerId).take(1).collect { user ->
                        _peerUser.value = user
                    }
                }
                if (session?.state == CallState.ENDED) {
                    cleanup()
                } else if (session?.state == CallState.OUTGOING_RINGING || session?.state == CallState.INCOMING_RINGING || session?.state == CallState.CONNECTING || session?.state == CallState.ACTIVE_CALL) {
                    viewModelScope.launch {
                        _permissionsGranted.first { it }
                        setupWebRTC()
                    }
                }
            }
        }
    }

    private fun startCallFlow() {
        // State is already set to OUTGOING_RINGING by repository.startCall
    }

    private fun setupWebRTC() {
        if (isWebRTCSetup) return
        isWebRTCSetup = true
        
        // Use a background scope for heavy initialization to keep UI responsive
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (_callSession.value?.type == "VIDEO") {
                    val capturer = createVideoCapturer()
                    if (capturer != null) {
                        withContext(Dispatchers.Main) {
                            callManager.startLocalVideo(capturer)
                        }
                    }
                }
                callManager.setupCall(callId, isCaller)
            } catch (e: Exception) {
                android.util.Log.e("CALL_VM", "WebRTC setup failed", e)
            }
        }
    }

    fun acceptCall() {
        viewModelScope.launch {
            callManager.accept()
            repository.acceptCall(callId)
        }
    }

    fun rejectCall() {
        viewModelScope.launch {
            repository.endCall(callId)
        }
    }

    fun endCall() {
        viewModelScope.launch {
            repository.endCall(callId)
        }
    }

    private fun cleanup() {
        callManager.stopCall()
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera1Enumerator(false)
        val deviceNames = enumerator.deviceNames
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
