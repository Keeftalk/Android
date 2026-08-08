package com.keeftalk.chat.services

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log

import com.keeftalk.chat.ui.call.GsmCallActivity

private const val TAG = "KEEFTALK_INCALL"

class KeeftalkInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Call added: $call. State: ${call.state}")
        
        GsmCallManager.onCallAdded(call, this)
        
        val intent = Intent(this, GsmCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed: $call")
        GsmCallManager.onCallRemoved(call)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        GsmCallManager.updateAudioState(audioState)
    }
}

object GsmCallManager {
    private var currentCall: Call? = null
    private var inCallService: InCallService? = null
    private var listener: ((Call?) -> Unit)? = null
    private var audioStateListener: ((CallAudioState?) -> Unit)? = null

    fun onCallAdded(call: Call, service: InCallService) {
        currentCall = call
        inCallService = service
        listener?.invoke(call)
    }

    fun onCallRemoved(call: Call) {
        if (currentCall == call) {
            currentCall = null
            inCallService = null
            listener?.invoke(null)
        }
    }

    fun updateAudioState(state: CallAudioState) {
        audioStateListener?.invoke(state)
    }

    fun setMuted(muted: Boolean) {
        inCallService?.setMuted(muted)
    }

    fun setAudioRoute(route: Int) {
        inCallService?.setAudioRoute(route)
    }

    fun getCurrentCall() = currentCall
    fun getAudioState() = inCallService?.callAudioState

    fun setListener(l: (Call?) -> Unit) {
        listener = l
        l(currentCall)
    }

    fun setAudioStateListener(l: (CallAudioState?) -> Unit) {
        audioStateListener = l
        l(getAudioState())
    }
}
