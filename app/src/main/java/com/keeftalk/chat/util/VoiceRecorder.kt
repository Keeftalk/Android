package com.keeftalk.chat.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class VoiceRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var isRecording = false

    fun start(fileName: String = "voice_record_${System.currentTimeMillis()}.m4a"): Boolean {
        if (isRecording) return false
        
        val outputDir = context.cacheDir
        val outputFile = File(outputDir, fileName)
        currentFile = outputFile

        return try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            mediaRecorder?.release()
            mediaRecorder = null
            false
        }
    }

    fun stop(): File? {
        if (!isRecording) return null
        
        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaRecorder = null
        isRecording = false
        return currentFile
    }

    fun cancel() {
        if (!isRecording && currentFile == null) return
        
        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaRecorder = null
        isRecording = false
        currentFile?.delete()
        currentFile = null
    }

    fun getMaxAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun isRecording() = isRecording
}
