package tn.keeftalk.chat.feature.mydin.service

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tn.keeftalk.chat.MainActivity
import tn.keeftalk.chat.R
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MyDinAudioService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): MyDinAudioService = this@MyDinAudioService
    }

    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Playback state flows for UI binding
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentSurahNumber = MutableStateFlow(1)
    val currentSurahNumber: StateFlow<Int> = _currentSurahNumber.asStateFlow()

    private val _currentSurahName = MutableStateFlow("الفاتحة")
    val currentSurahName: StateFlow<String> = _currentSurahName.asStateFlow()

    private val _currentAyahNumber = MutableStateFlow(1)
    val currentAyahNumber: StateFlow<Int> = _currentAyahNumber.asStateFlow()

    private val _totalAyahs = MutableStateFlow(7)
    val totalAyahs: StateFlow<Int> = _totalAyahs.asStateFlow()

    private val _currentReciterId = MutableStateFlow("Husary_128kbps")
    val currentReciterId: StateFlow<String> = _currentReciterId.asStateFlow()

    private val _currentReciterName = MutableStateFlow("الحصري")
    val currentReciterName: StateFlow<String> = _currentReciterName.asStateFlow()

    private var currentPlaybackMode = "AUTOPLAY_NEXT_SURAH"

    var onAyahCompletedListener: ((surahNum: Int, nextAyahNum: Int) -> Unit)? = null

    companion object {
        const val CHANNEL_ID = "mydin_media_channel"
        const val NOTIFICATION_ID = 1002

        const val ACTION_PLAY_AYAH = "tn.keeftalk.chat.mydin.ACTION_PLAY_AYAH"
        const val ACTION_TOGGLE_PLAY = "tn.keeftalk.chat.mydin.ACTION_TOGGLE_PLAY"
        const val ACTION_PREV = "tn.keeftalk.chat.mydin.ACTION_PREV"
        const val ACTION_NEXT = "tn.keeftalk.chat.mydin.ACTION_NEXT"
        const val ACTION_STOP = "tn.keeftalk.chat.mydin.ACTION_STOP"

        const val EXTRA_SURAH_NUM = "extra_surah_num"
        const val EXTRA_SURAH_NAME = "extra_surah_name"
        const val EXTRA_AYAH_NUM = "extra_ayah_num"
        const val EXTRA_TOTAL_AYAHS = "extra_total_ayahs"
        const val EXTRA_RECITER_ID = "extra_reciter_id"
        const val EXTRA_RECITER_NAME = "extra_reciter_name"
        const val EXTRA_AUDIO_URL = "extra_audio_url"
        const val EXTRA_LOCAL_PATH = "extra_local_path"
        const val EXTRA_PLAYBACK_MODE = "extra_playback_mode"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        when (intent?.action) {
            ACTION_PLAY_AYAH -> {
                val surahNum = intent.getIntExtra(EXTRA_SURAH_NUM, 1)
                val surahName = intent.getStringExtra(EXTRA_SURAH_NAME) ?: "الفاتحة"
                val ayahNum = intent.getIntExtra(EXTRA_AYAH_NUM, 1)
                val totalAyahsCount = intent.getIntExtra(EXTRA_TOTAL_AYAHS, 7)
                val reciterId = intent.getStringExtra(EXTRA_RECITER_ID) ?: "Husary_128kbps"
                val reciterName = intent.getStringExtra(EXTRA_RECITER_NAME) ?: "الحصري"
                val url = intent.getStringExtra(EXTRA_AUDIO_URL)
                val localPath = intent.getStringExtra(EXTRA_LOCAL_PATH)
                val pbMode = intent.getStringExtra(EXTRA_PLAYBACK_MODE) ?: "AUTOPLAY_NEXT_SURAH"

                currentPlaybackMode = pbMode

                playAyahTrack(
                    surahNum = surahNum,
                    surahName = surahName,
                    ayahNum = ayahNum,
                    totalAyahsCount = totalAyahsCount,
                    reciterId = reciterId,
                    reciterName = reciterName,
                    url = url,
                    localPath = localPath
                )
            }
            ACTION_TOGGLE_PLAY -> togglePlayPause()
            ACTION_PREV -> playPreviousAyah()
            ACTION_NEXT -> playNextAyah()
            ACTION_STOP -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    fun playAyahTrack(
        surahNum: Int,
        surahName: String,
        ayahNum: Int,
        totalAyahsCount: Int,
        reciterId: String,
        reciterName: String,
        url: String?,
        localPath: String?
    ) {
        _currentSurahNumber.value = surahNum
        _currentSurahName.value = surahName
        _currentAyahNumber.value = ayahNum
        _totalAyahs.value = totalAyahsCount
        _currentReciterId.value = reciterId
        _currentReciterName.value = reciterName

        stopMediaPlayerOnly()

        _isLoading.value = true
        _isPlaying.value = false
        updateNotification()

        serviceScope.launch {
            val fileToPlay = resolveAndAutoDownloadTrack(surahNum, ayahNum, reciterId, localPath, url)

            withContext(Dispatchers.Main) {
                if (fileToPlay == null) {
                    _isLoading.value = false
                    _isPlaying.value = false
                    updateNotification()
                    return@withContext
                }

                try {
                    val mp = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )

                        setDataSource(fileToPlay)

                        setOnPreparedListener {
                            _isLoading.value = false
                            _isPlaying.value = true
                            it.start()
                            updateNotification()
                        }

                        setOnCompletionListener {
                            _isPlaying.value = false
                            val currentAyah = _currentAyahNumber.value
                            val total = _totalAyahs.value

                            when (currentPlaybackMode) {
                                "REPEAT_AYAH" -> {
                                    playAyahTrack(
                                        surahNum = _currentSurahNumber.value,
                                        surahName = _currentSurahName.value,
                                        ayahNum = currentAyah,
                                        totalAyahsCount = total,
                                        reciterId = _currentReciterId.value,
                                        reciterName = _currentReciterName.value,
                                        url = null,
                                        localPath = null
                                    )
                                }
                                else -> {
                                    if (currentAyah < total) {
                                        val nextAyah = currentAyah + 1
                                        _currentAyahNumber.value = nextAyah
                                        onAyahCompletedListener?.invoke(_currentSurahNumber.value, nextAyah)
                                        playAyahTrack(
                                            surahNum = _currentSurahNumber.value,
                                            surahName = _currentSurahName.value,
                                            ayahNum = nextAyah,
                                            totalAyahsCount = total,
                                            reciterId = _currentReciterId.value,
                                            reciterName = _currentReciterName.value,
                                            url = null,
                                            localPath = null
                                        )
                                    } else if (currentPlaybackMode == "REPEAT_SURAH") {
                                        _currentAyahNumber.value = 1
                                        onAyahCompletedListener?.invoke(_currentSurahNumber.value, 1)
                                        playAyahTrack(
                                            surahNum = _currentSurahNumber.value,
                                            surahName = _currentSurahName.value,
                                            ayahNum = 1,
                                            totalAyahsCount = total,
                                            reciterId = _currentReciterId.value,
                                            reciterName = _currentReciterName.value,
                                            url = null,
                                            localPath = null
                                        )
                                    } else {
                                        updateNotification()
                                    }
                                }
                            }
                        }

                        setOnErrorListener { _, _, _ ->
                            _isLoading.value = false
                            _isPlaying.value = false
                            updateNotification()
                            true
                        }
                    }

                    mediaPlayer = mp
                    mp.prepareAsync()
                } catch (_: Exception) {
                    _isLoading.value = false
                    _isPlaying.value = false
                    updateNotification()
                }
            }
        }
    }

    private suspend fun resolveAndAutoDownloadTrack(
        surahNum: Int,
        ayahNum: Int,
        reciterId: String,
        localPath: String?,
        remoteUrl: String?
    ): String? = withContext(Dispatchers.IO) {
        if (!localPath.isNullOrBlank()) {
            val f = File(localPath)
            if (f.exists() && f.length() > 100) return@withContext localPath
        }

        val externalDir = Environment.getExternalStorageDirectory()
        val defaultLocalFile = File(externalDir, "keeftalk/.MyQuran/audio/$reciterId/${surahNum}_$ayahNum.mp3")
        if (defaultLocalFile.exists() && defaultLocalFile.length() > 100) {
            return@withContext defaultLocalFile.absolutePath
        }

        val fallbackFile = File(getExternalFilesDir(null) ?: filesDir, "keeftalk/.MyQuran/audio/$reciterId/${surahNum}_$ayahNum.mp3")
        if (fallbackFile.exists() && fallbackFile.length() > 100) {
            return@withContext fallbackFile.absolutePath
        }

        val downloadUrl = remoteUrl ?: generateDefaultAudioUrl(surahNum, ayahNum, reciterId)
        try {
            val target = if (defaultLocalFile.parentFile?.exists() == true || defaultLocalFile.parentFile?.mkdirs() == true) defaultLocalFile else fallbackFile
            val parentDir = target.parentFile
            if (parentDir != null && !parentDir.exists()) parentDir.mkdirs()
            val noMedia = File(parentDir?.parentFile, ".nomedia")
            if (!noMedia.exists()) try { noMedia.createNewFile() } catch (_: Exception) {}

            val u = URL(downloadUrl)
            val conn = u.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode == 200) {
                val input = conn.inputStream
                val output = FileOutputStream(target)
                input.copyTo(output)
                output.flush()
                output.close()
                input.close()
                if (target.exists() && target.length() > 100) {
                    return@withContext target.absolutePath
                }
            }
        } catch (_: Exception) {}

        return@withContext downloadUrl
    }

    fun togglePlayPause() {
        val mp = mediaPlayer
        if (mp != null) {
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
            } else {
                mp.start()
                _isPlaying.value = true
            }
            updateNotification()
        }
    }

    fun playPreviousAyah() {
        val cur = _currentAyahNumber.value
        if (cur > 1) {
            val prev = cur - 1
            _currentAyahNumber.value = prev
            playAyahTrack(
                surahNum = _currentSurahNumber.value,
                surahName = _currentSurahName.value,
                ayahNum = prev,
                totalAyahsCount = _totalAyahs.value,
                reciterId = _currentReciterId.value,
                reciterName = _currentReciterName.value,
                url = null,
                localPath = null
            )
        }
    }

    fun playNextAyah() {
        val cur = _currentAyahNumber.value
        val total = _totalAyahs.value
        if (cur < total) {
            val next = cur + 1
            _currentAyahNumber.value = next
            playAyahTrack(
                surahNum = _currentSurahNumber.value,
                surahName = _currentSurahName.value,
                ayahNum = next,
                totalAyahsCount = total,
                reciterId = _currentReciterId.value,
                reciterName = _currentReciterName.value,
                url = null,
                localPath = null
            )
        }
    }

    fun stopPlayback() {
        stopMediaPlayerOnly()
        _isPlaying.value = false
        _isLoading.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopMediaPlayerOnly() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    private fun generateDefaultAudioUrl(surahNum: Int, ayahNum: Int, reciterId: String): String {
        val s = String.format(Locale.US, "%03d", surahNum)
        val a = String.format(Locale.US, "%03d", ayahNum)
        return "https://everyayah.com/data/$reciterId/$s$a.mp3"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تلاوة القرآن الكريم",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "مشغل تلاوة القرآن الكريم خلفية"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, MyDinAudioService::class.java).apply { action = ACTION_PREV }
        val prevPendingIntent = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val toggleIntent = Intent(this, MyDinAudioService::class.java).apply { action = ACTION_TOGGLE_PLAY }
        val togglePendingIntent = PendingIntent.getService(this, 2, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(this, MyDinAudioService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, MyDinAudioService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val title = "سورة ${_currentSurahName.value} · الآية ${_currentAyahNumber.value}"
        val text = "بصوت ${_currentReciterName.value} · Keeftalk"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText("تلاوة القرآن الكريم")
            .setContentIntent(contentPendingIntent)
            .setOngoing(_isPlaying.value)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "السابقة", prevPendingIntent)
            .addAction(
                if (_isPlaying.value) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (_isPlaying.value) "إيقاف مؤقت" else "تشغيل",
                togglePendingIntent
            )
            .addAction(android.R.drawable.ic_media_next, "التالية", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق", stopPendingIntent)

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopMediaPlayerOnly()
    }
}
