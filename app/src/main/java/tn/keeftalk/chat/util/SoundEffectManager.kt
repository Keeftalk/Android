package tn.keeftalk.chat.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import tn.keeftalk.chat.R

object SoundEffectManager {
    private const val TAG = "SoundEffectManager"

    /**
     * Plays a sound effect intended for inside-chat events (Message sending, receiving, seen).
     * Uses USAGE_MEDIA (Media volume stream).
     */
    fun playMediaSound(context: Context, resId: Int) {
        playSound(context, resId, AudioAttributes.USAGE_MEDIA, AudioAttributes.CONTENT_TYPE_SONIFICATION)
    }

    /**
     * Plays a sound effect intended for notifications, bells, nudges, and ringtones.
     * Uses USAGE_NOTIFICATION or USAGE_NOTIFICATION_RINGTONE (Notification/Ring volume stream).
     */
    fun playNotificationSound(context: Context, resId: Int, isRing: Boolean = false) {
        val usage = if (isRing) AudioAttributes.USAGE_NOTIFICATION_RINGTONE else AudioAttributes.USAGE_NOTIFICATION
        playSound(context, resId, usage, AudioAttributes.CONTENT_TYPE_SONIFICATION)
    }

    private fun playSound(context: Context, resId: Int, usage: Int, contentType: Int) {
        try {
            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(contentType)
                        .build()
                )
                val afd = context.resources.openRawResourceFd(resId)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                setOnCompletionListener { mp ->
                    mp.release()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    mp.release()
                    true
                }
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play sound effect resId=$resId", e)
        }
    }

    // Convenience constants for raw sound resources
    val RES_RINGBACK_TONE = R.raw.ringback_tone
    val RES_INCOMING_RING = R.raw.get_called_ring_ringtone
    val RES_MSG_SENDING = R.raw.message_sending_inside_chat
    val RES_MSG_RECEIVING_CHAT = R.raw.message_receiving_inside_chat
    val RES_MSG_SEEN_CHAT = R.raw.message_seen_while_inside_chat
    val RES_ANY_NOTIFICATION = R.raw.any_other_notification_use_this
    val RES_PROFILE_BELL = R.raw.profile_view_nudged_bell_notification_sound
    val RES_MSG_RECEIVING_APP = R.raw.message_receiving_inside_app_and_not_inside_chat
    val RES_GOAL_DONE = R.raw.my_todo_my_habit_daily_reading_goal_done_sound_effect
}
