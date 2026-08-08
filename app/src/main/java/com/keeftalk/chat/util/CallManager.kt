package com.keeftalk.chat.util

import android.content.Context
import android.util.Log
import com.keeftalk.chat.domain.repository.ChatRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.webrtc.*
import java.util.UUID

private const val TAG = "KEEFTALK_CALL"

class CallManager(
    private val context: Context,
    private val repository: ChatRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var eglBase: EglBase = EglBase.create()
    
    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack
    
    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack

    private val _connectionState = MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
    val connectionState: StateFlow<PeerConnection.PeerConnectionState> = _connectionState

    private val _isAccepted = MutableStateFlow(false)
    private var isRemoteDescriptionSet = false
    private val iceCandidateQueue = mutableListOf<IceCandidate>()
    private val processedSignals = mutableSetOf<String>()

    init {
        initWebRTC()
    }

    private fun initWebRTC() {
        Log.d(TAG, "[CALL] Initializing WebRTC")
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val factoryOptions = PeerConnectionFactory.Options()
        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(factoryOptions)
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .createPeerConnectionFactory()
    }

    fun startLocalVideo(videoCapturer: VideoCapturer): VideoTrack? {
        val factory = peerConnectionFactory ?: return null
        val source = factory.createVideoSource(false)
        try {
            videoCapturer.initialize(SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext), context, source.capturerObserver)
            videoCapturer.startCapture(1280, 720, 30)
        } catch (e: Exception) {
            Log.e(TAG, "[CALL] Failed to start camera", e)
        }
        val track = factory.createVideoTrack("LOCAL_VIDEO_TRACK", source)
        _localVideoTrack.value = track
        return track
    }

    private fun createAudioTrack(): AudioTrack? {
        val factory = peerConnectionFactory ?: return null
        val audioSource = factory.createAudioSource(MediaConstraints())
        return factory.createAudioTrack("ARDAMSa0", audioSource)
    }

    suspend fun setupCall(callId: String, isCaller: Boolean) {
        Log.d(TAG, "[CALL] Setting up call. isCaller: $isCaller, callId: $callId")
        if (isCaller) _isAccepted.value = true
        val factory = peerConnectionFactory ?: return
        
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80").setUsername("openrelayproject").setPassword("openrelayproject").createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443").setUsername("openrelayproject").setPassword("openrelayproject").createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp").setUsername("openrelayproject").setPassword("openrelayproject").createIceServer()
        )
        
        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                Log.d(TAG, "[CALL] ICE GENERATED: ${candidate.sdpMid} | ${candidate.sdp}")
                scope.launch {
                    val candidateJson = org.json.JSONObject().apply {
                        put("sdpMid", candidate.sdpMid)
                        put("sdpMLineIndex", candidate.sdpMLineIndex)
                        put("sdp", candidate.sdp)
                    }
                    repository.sendCallSignaling(callId, "ICE_CANDIDATE", candidateJson.toString())
                    Log.d(TAG, "[CALL] ICE SENT")
                }
            }
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {
                val track = receiver.track()
                Log.d(TAG, "[CALL] ON ADD TRACK: ${track?.kind()} | ID: ${track?.id()}")
                if (track is VideoTrack) {
                    _remoteVideoTrack.value = track
                }
            }
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                Log.d(TAG, "[CALL] PEER CONNECTION STATE CHANGE: $newState")
                _connectionState.value = newState
            }
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "[CALL] SIGNALING STATE CHANGE: $state")
            }
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "[CALL] ICE CONNECTION CHANGE: $state")
                if (state == PeerConnection.IceConnectionState.CONNECTED || state == PeerConnection.IceConnectionState.COMPLETED) {
                    Log.d(TAG, "[CALL] ICE CONNECTED / COMPLETED")
                    // Force state update to CONNECTED to trigger UI transition
                    _connectionState.value = PeerConnection.PeerConnectionState.CONNECTED
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(TAG, "[CALL] ICE RECEIVING CHANGE: $receiving")
            }
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "[CALL] ICE GATHERING CHANGE: $state")
            }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {
                Log.d(TAG, "[CALL] RENEGOTIATION NEEDED")
            }
            override fun onAddStream(p0: MediaStream?) {}
            override fun onTrack(transceiver: RtpTransceiver?) {}
        }

        peerConnection = factory.createPeerConnection(iceServers, observer)

        // Add Audio Track (REQUIRED for both Voice and Video)
        val audioTrack = createAudioTrack()
        if (audioTrack != null) {
            peerConnection?.addTrack(audioTrack, listOf("ARDAMS"))
            Log.d(TAG, "[CALL] Audio track added")
        }

        // Add Video Track if available
        _localVideoTrack.value?.let {
            peerConnection?.addTrack(it, listOf("ARDAMS"))
            Log.d(TAG, "[CALL] Local video track added")
        }

        if (isCaller) {
            Log.d(TAG, "[CALL] Creating OFFER")
            val constraints = MediaConstraints()
            peerConnection?.createOffer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription) {
                    Log.d(TAG, "[CALL] OFFER Created, setting local description")
                    peerConnection?.setLocalDescription(this, sdp)
                    scope.launch { 
                        repository.sendCallSignaling(callId, "OFFER", sdp.description) 
                        Log.d(TAG, "[CALL] OFFER Sent")
                    }
                }
                override fun onSetSuccess() {
                    Log.d(TAG, "[CALL] Local description set success (OFFER)")
                }
                override fun onCreateFailure(e: String?) {
                    Log.e(TAG, "[CALL] Create OFFER Failure: $e")
                }
                override fun onSetFailure(e: String?) {
                    Log.e(TAG, "[CALL] Set Local description Failure: $e")
                }
            }, constraints)
        }

        // Signaling listener
        Log.d(TAG, "[CALL] Subscribing to signaling channel")
        scope.launch {
            repository.getCallSignaling(callId).collect { signal ->
                if (processedSignals.contains(signal.id)) return@collect
                processedSignals.add(signal.id)

                Log.d(TAG, "[CALL] SIGNAL RECEIVED: ${signal.type}")
                when (signal.type) {
                    "OFFER" -> {
                        if (!isCaller) {
                            Log.d(TAG, "[CALL] OFFER RECEIVED, payload length: ${signal.payload.length}")
                            peerConnection?.setRemoteDescription(object : SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onSetSuccess() {
                                    Log.d(TAG, "[CALL] Remote description (OFFER) set success")
                                    isRemoteDescriptionSet = true
                                    drainIceCandidates()
                                    scope.launch {
                                        Log.d(TAG, "[CALL] Waiting for user to accept call...")
                                        _isAccepted.first { it }
                                        Log.d(TAG, "[CALL] User accepted, creating ANSWER")
                                        peerConnection?.createAnswer(object : SdpObserver {
                                            override fun onCreateSuccess(sdp: SessionDescription) {
                                                Log.d(TAG, "[CALL] ANSWER Created, setting local description")
                                                peerConnection?.setLocalDescription(this, sdp)
                                                scope.launch { 
                                                    repository.sendCallSignaling(callId, "ANSWER", sdp.description) 
                                                    Log.d(TAG, "[CALL] ANSWER Sent")
                                                }
                                            }
                                            override fun onSetSuccess() {
                                                Log.d(TAG, "[CALL] Local description set success (ANSWER)")
                                            }
                                            override fun onCreateFailure(e: String?) {
                                                Log.e(TAG, "[CALL] Create ANSWER Failure: $e")
                                            }
                                            override fun onSetFailure(e: String?) {
                                                Log.e(TAG, "[CALL] Set Local description Failure: $e")
                                            }
                                        }, MediaConstraints())
                                    }
                                }
                                override fun onCreateFailure(e: String?) {}
                                override fun onSetFailure(e: String?) {
                                    Log.e(TAG, "[CALL] Set Remote description Failure: $e")
                                }
                            }, SessionDescription(SessionDescription.Type.OFFER, signal.payload))
                        }
                    }
                    "ANSWER" -> {
                        if (isCaller) {
                            Log.d(TAG, "[CALL] ANSWER RECEIVED, payload length: ${signal.payload.length}")
                            peerConnection?.setRemoteDescription(object : SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onSetSuccess() {
                                    Log.d(TAG, "[CALL] Remote description (ANSWER) set success")
                                    isRemoteDescriptionSet = true
                                    drainIceCandidates()
                                }
                                override fun onCreateFailure(e: String?) {}
                                override fun onSetFailure(e: String?) {
                                    Log.e(TAG, "[CALL] Set Remote description Failure: $e")
                                }
                            }, SessionDescription(SessionDescription.Type.ANSWER, signal.payload))
                        }
                    }
                    "ICE_CANDIDATE" -> {
                        try {
                            val json = org.json.JSONObject(signal.payload)
                            val candidate = IceCandidate(
                                json.getString("sdpMid"),
                                json.getInt("sdpMLineIndex"),
                                json.getString("sdp")
                            )
                            if (isRemoteDescriptionSet) {
                                Log.d(TAG, "[CALL] ICE RECEIVED & ADDED: ${candidate.sdpMid}")
                                peerConnection?.addIceCandidate(candidate)
                            } else {
                                Log.d(TAG, "[CALL] ICE RECEIVED & QUEUED: ${candidate.sdpMid}")
                                iceCandidateQueue.add(candidate)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "[CALL] Error parsing ICE candidate", e)
                        }
                    }
                }
            }
        }
    }

    private fun drainIceCandidates() {
        Log.d(TAG, "[CALL] Draining ${iceCandidateQueue.size} queued ICE candidates")
        iceCandidateQueue.forEach {
            peerConnection?.addIceCandidate(it)
        }
        iceCandidateQueue.clear()
    }

    fun accept() {
        Log.d(TAG, "[CALL] User accepted call via UI")
        _isAccepted.value = true
    }

    fun toggleAudio(enabled: Boolean) {
        peerConnection?.senders?.forEach { sender ->
            val track = sender.track()
            if (track is AudioTrack) {
                track.setEnabled(enabled)
            }
        }
    }

    fun toggleVideo(enabled: Boolean) {
        peerConnection?.senders?.forEach { sender ->
            val track = sender.track()
            if (track is VideoTrack) {
                track.setEnabled(enabled)
            }
        }
    }

    fun stopCall() {
        Log.d(TAG, "[CALL] Stopping call and cleaning up")
        try {
            peerConnection?.close()
        } catch (e: Exception) {}
        peerConnection = null
        _localVideoTrack.value = null
        _remoteVideoTrack.value = null
        isRemoteDescriptionSet = false
        iceCandidateQueue.clear()
    }
}
