package tn.keeftalk.chat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Trace
import android.util.Log
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import tn.keeftalk.chat.util.PerformanceProfiler
import tn.keeftalk.chat.util.PerformanceLifecycleCallbacks
import tn.keeftalk.chat.util.MemoryTracker
import tn.keeftalk.chat.util.KeeftalkMediaFetcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.seconds

class KeeftalkApplication : Application(), ImageLoaderFactory {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun newImageLoader(): ImageLoader {
        PerformanceProfiler.startStage("Coil ImageLoader Init")
        val loader = ImageLoader.Builder(this)
            .components {
                add(coil.decode.VideoFrameDecoder.Factory())
                add(KeeftalkMediaFetcher.Factory(this@KeeftalkApplication))
            }
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .crossfade(100)
            .respectCacheHeaders(false)
            .build()
        PerformanceProfiler.endStage("Coil ImageLoader Init", category = PerformanceProfiler.Category.MEDIA)
        return loader
    }

    override fun onCreate() {
        Trace.beginSection("KeeftalkApplication.onCreate")
        PerformanceProfiler.init(this)
        registerActivityLifecycleCallbacks(PerformanceLifecycleCallbacks())
        PerformanceProfiler.startStage("Application.onCreate()")
        super.onCreate()
        
        MemoryTracker.logHeapSnapshot(this, "App Start")

        // CORE ARCHITECTURE: Execute Tier 1 IMMEDIATELY
        Trace.beginSection("Tier 1: Local Prep")
        PerformanceProfiler.startStage("Tier 1: Local Prep")
        
        // Critical only
        tn.keeftalk.chat.util.KeeftalkStore.init(this)
        tn.keeftalk.chat.security.crypto.KeyManager.init(this)
        org.maplibre.android.MapLibre.getInstance(this)
        
        // Ensure channels exist synchronously for FCM wake-up
        createNotificationChannels()

        PerformanceProfiler.endStage("Tier 1: Local Prep", category = PerformanceProfiler.Category.STORAGE)
        Trace.endSection()

        // TIER 2: Immediate Background (Local Data Hydration)
        tn.keeftalk.chat.util.StartupOrchestrator.enqueue(tn.keeftalk.chat.util.StartupOrchestrator.Tier.TIER_2_BACKGROUND) {
            try {
                Trace.beginSection("Tier 2: Background Hydration")
                PerformanceProfiler.startStage("Tier 2: DB Warm & Auth")

                // OPTIMIZATION: Load SQLCipher native libraries in background
                try {
                    net.sqlcipher.database.SQLiteDatabase.loadLibs(this@KeeftalkApplication)
                    Log.i("KeeftalkApplication", "SQLCipher native libraries initialized in background.")
                } catch (e: Throwable) {
                    Log.e("KeeftalkApplication", "Failed to load SQLCipher libs in background", e)
                }

                // Initialize Logger in background
                tn.keeftalk.chat.util.PersistentLogger.init(this@KeeftalkApplication)
                
                // Load FastPath KV in background
                tn.keeftalk.chat.util.FastPathInbox.loadFromKV()

                // Initialize Global Dependency Graph
                tn.keeftalk.chat.util.AppDependencies.init(this@KeeftalkApplication)

                // Refill FastPath from SQLite if KV was stale/empty
                tn.keeftalk.chat.util.FastPathInbox.loadFromSQL(this@KeeftalkApplication)

                val db = tn.keeftalk.chat.di.AppModule.provideDatabase(this@KeeftalkApplication)
                db.openHelper.writableDatabase // Forces DB open
                PerformanceProfiler.logEvent("SQLite Connection Established", category = PerformanceProfiler.Category.DATABASE)

                // Pre-warm Chat Repository Local Cache
                val repo = tn.keeftalk.chat.di.AppModule.provideChatRepository(this@KeeftalkApplication)
                repo.startBackgroundTasks()

                // Load Installed Extensions into Registries
                try {
                    tn.keeftalk.chat.di.AppModule.provideExtensionHost(this@KeeftalkApplication).loadAllInstalledExtensions()
                    Log.i("KeeftalkApplication", "ExtensionHost loaded installed extensions on startup.")
                } catch (e: Exception) {
                    Log.e("KeeftalkApplication", "Failed to load extensions on startup", e)
                }

                // Initialize JobManager
                val instantiator = tn.keeftalk.chat.util.job.KeeftalkJobInstantiator(this@KeeftalkApplication)
                val jobController = tn.keeftalk.chat.util.job.JobController.init(this@KeeftalkApplication, instantiator)
                jobController.start()

                // CRYPTO & AUTH INITIALIZATION
                val userId = tn.keeftalk.chat.di.AppModule.provideUserPreferencesRepository(this@KeeftalkApplication).getUserIdFast()
                if (userId != null) {
                    PerformanceProfiler.startStage("Crypto Session Restore")
                    try {
                        tn.keeftalk.chat.account.KeeftalkAccountSyncHelper.ensureAccountExists(this@KeeftalkApplication, userId)
                    } catch (_: Exception) {}
                    // Start session monitoring and restore AEK if needed
                    tn.keeftalk.chat.di.AppModule.provideAuthRepository(this@KeeftalkApplication)
                    // Pre-warm conversation keys
                    tn.keeftalk.chat.di.AppModule.provideCryptoManager(this@KeeftalkApplication)
                    PerformanceProfiler.endStage("Crypto Session Restore", category = PerformanceProfiler.Category.AUTH)
                }

                PerformanceProfiler.endStage("Tier 2: DB Warm & Auth", category = PerformanceProfiler.Category.BACKGROUND)
                Trace.endSection()
            } catch (e: Exception) {
                android.util.Log.e("KeeftalkApplication", "Tier 2 Failed", e)
            }
        }

        // TIER 3: Post-Render (Heavy & Network tasks)
        tn.keeftalk.chat.util.StartupOrchestrator.enqueue(tn.keeftalk.chat.util.StartupOrchestrator.Tier.TIER_3_POST_RENDER) {
            Trace.beginSection("Tier 3: Post-Render")
            PerformanceProfiler.startStage("Tier 3: UI Pre-inflation & Network")

            // Initialize Supabase after UI is ready
            tn.keeftalk.chat.di.AppModule.startSupabaseInit(this@KeeftalkApplication)

            // Start media cleanup worker
            tn.keeftalk.chat.data.service.FileCleanupWorker.schedule(this@KeeftalkApplication)

            // OPTIMIZATION: Stagger heavy sync tasks to avoid IO burst during initial scroll
            applicationScope.launch(Dispatchers.IO) {
                delay(3.seconds)
                // Start Background Sync Manager
                val syncManager = tn.keeftalk.chat.di.AppModule.provideBackgroundSyncManager(this@KeeftalkApplication)
                syncManager.startFullSync()

                delay(2.seconds)
                // Start Telephony Sync Manager
                val telephonySyncManager = tn.keeftalk.chat.di.AppModule.provideTelephonySyncManager(this@KeeftalkApplication)
                telephonySyncManager.startSync()
            }

            // EmojiCompat initialization
            val config = BundledEmojiCompatConfig(this@KeeftalkApplication, java.util.concurrent.Executors.newSingleThreadExecutor())
            EmojiCompat.init(config)

            // Warm the Recipient Cache (Top 20 contacts - throttled)
            try {
                val repo = tn.keeftalk.chat.di.AppModule.provideChatRepository(this@KeeftalkApplication)
                val contacts = repo.getContacts().first()
                tn.keeftalk.chat.util.LiveRecipientCache.warm(contacts.take(20))
            } catch (e: Exception) {
                PerformanceProfiler.logEvent("Recipient Cache Warm Failed", info = e.message, isError = true)
            }

            // Database Optimization (Delayed to avoid contention)
            applicationScope.launch(Dispatchers.IO) {
                delay(10.seconds)
                try {
                    val db = tn.keeftalk.chat.di.AppModule.provideDatabase(this@KeeftalkApplication)
                    db.openHelper.writableDatabase.execSQL("ANALYZE")
                    Log.i("KeeftalkApplication", "Background DB ANALYZE complete")
                } catch (e: Exception) {
                    PerformanceProfiler.logEvent("DB ANALYZE Failed", info = e.message, isError = true)
                }
            }

            tn.keeftalk.chat.ui.screens.ChatListFragment.warmViewPool(this@KeeftalkApplication)
            PerformanceProfiler.endStage("Tier 3: UI Pre-inflation & Network", category = PerformanceProfiler.Category.BACKGROUND)

            PerformanceProfiler.endSession("STARTUP")
            Trace.endSection()
        }

        PerformanceProfiler.endStage("Application.onCreate()", category = PerformanceProfiler.Category.APP)
        tn.keeftalk.chat.util.StartupOrchestrator.onApplicationCreate()
        Trace.endSection()
    }

    private fun createNotificationChannels() {
        PerformanceProfiler.startStage("Notification Channels Creation")
        tn.keeftalk.chat.util.notifications.NotificationChannelManager.createAllChannels(this)
        PerformanceProfiler.endStage("Notification Channels Creation", category = PerformanceProfiler.Category.ANDROID)
    }

}
