package tn.keeftalk.chat.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import tn.keeftalk.chat.data.auth.DataStoreSessionManager
import tn.keeftalk.chat.data.local.KeeftalkDatabase
import tn.keeftalk.chat.data.prefs.UserPreferencesRepository
import tn.keeftalk.chat.BuildConfig
import tn.keeftalk.chat.data.repository.AuthRepositoryImpl
import tn.keeftalk.chat.data.repository.ChatRepositoryImpl
import tn.keeftalk.chat.domain.repository.AuthRepository
import tn.keeftalk.chat.domain.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.serialization.kotlinx.json.json
import androidx.sqlite.db.SupportSQLiteDatabase
import tn.keeftalk.chat.util.PerformanceProfiler
import android.os.SystemClock
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import androidx.core.content.edit
import kotlinx.coroutines.asCoroutineDispatcher
import net.sqlcipher.database.SupportFactory
import tn.keeftalk.chat.security.crypto.DatabaseKeyManager
import java.io.File
import android.util.Base64

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object AppModule {
    private val databaseLock = Any()
    private val repositoryLock = Any()
    private val authRepositoryLock = Any()
    private val supabaseLock = Any()
    private val adminSupabaseLock = Any()
    private val translationLock = Any()

    @Volatile
    private var database: KeeftalkDatabase? = null
    
    @Volatile
    private var repository: ChatRepository? = null

    @Volatile
    private var cryptoManager: tn.keeftalk.chat.security.crypto.CryptoManager? = null

    @Volatile
    private var decryptionManager: tn.keeftalk.chat.security.crypto.MessageDecryptionManager? = null

    @Volatile
    private var translationManager: tn.keeftalk.chat.util.TranslationManager? = null

    @Volatile
    private var connectivityObserver: tn.keeftalk.chat.util.ConnectivityObserver? = null

    @Volatile
    private var authRepository: AuthRepository? = null

    @Volatile
    private var securityManager: tn.keeftalk.chat.security.crypto.SecurityManager? = null

    fun provideSecurityManager(context: Context): tn.keeftalk.chat.security.crypto.SecurityManager {
        return securityManager ?: synchronized(this) {
            securityManager ?: tn.keeftalk.chat.security.crypto.SecurityManager(
                context.applicationContext,
                tn.keeftalk.chat.security.crypto.KeyManager
            ).also { securityManager = it }
        }
    }

    @Volatile
    private var privacyRepository: tn.keeftalk.chat.domain.repository.PrivacyRepository? = null

    @Volatile
    private var countryService: tn.keeftalk.chat.domain.service.CountryService? = null

    @Volatile
    private var phoneNumberService: tn.keeftalk.chat.domain.service.PhoneNumberService? = null

    @Volatile
    private var legalRepository: tn.keeftalk.chat.domain.repository.LegalRepository? = null

    @Volatile
    private var securityRepository: tn.keeftalk.chat.domain.repository.SecurityRepository? = null

    @Volatile
    private var chatSettingsRepository: tn.keeftalk.chat.domain.repository.ChatSettingsRepository? = null

    @Volatile
    private var settingsRepository: tn.keeftalk.chat.domain.repository.SettingsRepository? = null

    @Volatile
    private var appCustomizationRepository: tn.keeftalk.chat.domain.repository.AppCustomizationRepository? = null

    @Volatile
    private var mediaRepository: tn.keeftalk.chat.domain.repository.MediaRepository? = null

    @Volatile
    private var mediaExportPipeline: tn.keeftalk.chat.data.service.MediaExportPipeline? = null

    @Volatile
    private var noteRepository: tn.keeftalk.chat.domain.repository.NoteRepository? = null

    @Volatile
    private var vaultRepository: tn.keeftalk.chat.domain.repository.VaultRepository? = null

    @Volatile
    private var bookRepository: tn.keeftalk.chat.domain.repository.BookRepository? = null

    @Volatile
    private var docRepository: tn.keeftalk.chat.domain.repository.DocRepository? = null

    @Volatile
    private var habitsRepository: tn.keeftalk.chat.domain.repository.HabitsRepository? = null

    @Volatile
    private var bookHabitIntegrationManager: tn.keeftalk.chat.domain.engine.BookHabitIntegrationManager? = null

    @Volatile
    private var vaultBackupManager: tn.keeftalk.chat.data.service.VaultBackupManager? = null

    @Volatile
    private var calendarRepository: tn.keeftalk.chat.domain.repository.CalendarRepository? = null

    @Volatile
    private var callLogManager: tn.keeftalk.chat.data.local.CallLogManager? = null

    @Volatile
    private var phoneContactManager: tn.keeftalk.chat.data.local.PhoneContactManager? = null

    @Volatile
    private var smsRepository: tn.keeftalk.chat.domain.repository.SmsRepository? = null

    @Volatile
    private var walletRepository: tn.keeftalk.chat.feature.wallet.repository.WalletRepository? = null
    private var financesRepository: tn.keeftalk.chat.feature.finances.repository.FinancesRepository? = null

    @Volatile
    private var emailRepository: tn.keeftalk.chat.feature.email.repository.EmailRepository? = null

    @Volatile
    private var emailAuthManager: tn.keeftalk.chat.feature.email.auth.EmailAuthManager? = null

    @Volatile
    private var cloudAuthManager: tn.keeftalk.chat.data.auth.CloudAuthManager? = null

    @Volatile
    @set:android.annotation.SuppressLint("StaticFieldLeak")
    private var telephonySyncManager: tn.keeftalk.chat.data.sync.TelephonySyncManager? = null

    @Volatile
    private var backgroundSyncManager: tn.keeftalk.chat.data.sync.BackgroundSyncManager? = null

    @Volatile
    private var parentalControlManager: tn.keeftalk.chat.util.ParentalControlManager? = null

    @Volatile
    private var notificationDispatcher: tn.keeftalk.chat.util.notifications.NotificationDispatcher? = null


    @Volatile
    private var fileRepository: tn.keeftalk.chat.domain.repository.FileRepository? = null

    @Volatile
    private var fileReferenceManager: tn.keeftalk.chat.util.FileReferenceManager? = null

    @Volatile
    private var fileUploadManager: tn.keeftalk.chat.util.FileUploadManager? = null

    @Volatile
    private var fileDownloadManager: tn.keeftalk.chat.util.FileDownloadManager? = null

    @Volatile
    private var relationshipRepository: tn.keeftalk.chat.domain.repository.RelationshipRepository? = null

    @Volatile
    private var cloudImportCoordinator: tn.keeftalk.chat.util.CloudImportCoordinator? = null

    @Volatile
    private var cloudSecureStore: tn.keeftalk.chat.data.local.CloudSecureStore? = null

    @Volatile
    private var googlePhotosService: tn.keeftalk.chat.data.cloud.GooglePhotosService? = null

    @Volatile
    private var googleDriveService: tn.keeftalk.chat.data.cloud.GoogleDriveService? = null

    val cloudImportResultFlow = kotlinx.coroutines.flow.MutableSharedFlow<List<String>>(extraBufferCapacity = 1)

    @Volatile
    private var dropboxService: tn.keeftalk.chat.data.cloud.DropboxService? = null

    @Volatile
    private var feedRepository: tn.keeftalk.chat.domain.repository.FeedRepository? = null

    @Volatile
    private var weatherRepository: tn.keeftalk.chat.domain.repository.WeatherRepository? = null

    @Volatile
    private var locationService: tn.keeftalk.chat.domain.service.LocationService? = null

    @Volatile
    private var billingManager: tn.keeftalk.chat.data.billing.BillingManager? = null

    @Volatile
    private var entitlementManager: tn.keeftalk.chat.domain.engine.EntitlementManager? = null

    @Volatile
    private var extensionHost: tn.keeftalk.chat.extension.lifecycle.ExtensionHost? = null

    @Volatile
    private var bugReportRepository: tn.keeftalk.chat.domain.repository.BugReportRepository? = null

    @Volatile
    private var twoFactorRepository: tn.keeftalk.chat.domain.repository.TwoFactorRepository? = null

    @Volatile
    private var supabaseClient: SupabaseClient? = null
    private var supabaseInitDeferred = kotlinx.coroutines.CompletableDeferred<SupabaseClient>()

    @Volatile
    private var conversationKeyManager: tn.keeftalk.chat.security.crypto.ConversationKeyManager? = null

    @Volatile
    private var mediaEncryptionManager: tn.keeftalk.chat.security.crypto.MediaEncryptionManager? = null

    @Volatile
    private var secureBackupManager: tn.keeftalk.chat.security.crypto.SecureBackupManager? = null

    private val databaseDeferred = kotlinx.coroutines.CompletableDeferred<KeeftalkDatabase>()

    private const val SUPABASE_URL = "https://lhlylcyuvfpauwvuxped.supabase.co/"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxobHlsY3l1dmZwYXV3dnV4cGVkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIyOTUzNDMsImV4cCI6MjA5Nzg3MTM0M30.vu5Zga2iX37NlDV9v0IgQp_PjDNWJmmqqhrvLJ9Un9E"

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun startSupabaseInit(context: Context) {
        synchronized(supabaseLock) {
            if (supabaseClient != null) return
            if (supabaseInitDeferred.isCompleted && supabaseInitDeferred.getCompletionExceptionOrNull() != null) {
                Log.w("AppModule", "Supabase init previously failed, resetting deferred for retry")
                supabaseInitDeferred = kotlinx.coroutines.CompletableDeferred()
            }
        }
        
        tn.keeftalk.chat.util.KeeftalkExecutors.BOUNDED_IO.execute {
            try {
                PerformanceProfiler.startStage("Supabase Client Creation")
                if (supabaseClient == null) {
                    val client = provideSupabaseClientInternal(context)
                    synchronized(supabaseLock) {
                        supabaseClient = client
                        supabaseInitDeferred.complete(client)
                    }
                }
                PerformanceProfiler.endStage("Supabase Client Creation", category = PerformanceProfiler.Category.NETWORK)
            } catch (e: Exception) {
                Log.e("AppModule", "Supabase Init Failed", e)
                synchronized(supabaseLock) {
                    if (!supabaseInitDeferred.isCompleted) {
                        supabaseInitDeferred.completeExceptionally(e)
                    }
                }
            }
        }
    }

    suspend fun provideSupabaseClientAsync(context: Context): SupabaseClient {
        val client = supabaseClient
        if (client != null) return client
        startSupabaseInit(context)
        return supabaseInitDeferred.await()
    }

    private fun provideSupabaseClientInternal(context: Context): SupabaseClient {
        val prefs = provideUserPreferencesRepository(context)
        val newClient = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY,
        ) {
            requestTimeout = 90.seconds
            httpEngine = OkHttp.create {
                config {
                    connectTimeout(60, TimeUnit.SECONDS)
                    readTimeout(60, TimeUnit.SECONDS)
                    writeTimeout(60, TimeUnit.SECONDS)
                    retryOnConnectionFailure(true)
                    
                    eventListenerFactory(object : okhttp3.EventListener.Factory {
                        override fun create(call: okhttp3.Call): okhttp3.EventListener {
                            return object : okhttp3.EventListener() {
                                var dnsStart = 0L
                                var connectStart = 0L
                                var requestStart = 0L
                                var tlsStart = 0L

                                override fun dnsStart(call: okhttp3.Call, domainName: String) {
                                    dnsStart = SystemClock.elapsedRealtimeNanos()
                                }
                                override fun dnsEnd(call: okhttp3.Call, domainName: String, inetAddressList: List<java.net.InetAddress>) {
                                    PerformanceProfiler.logEvent("DNS Resolved: $domainName", info = "${(SystemClock.elapsedRealtimeNanos() - dnsStart) / 1_000_000}ms", category = PerformanceProfiler.Category.NETWORK)
                                }
                                override fun secureConnectStart(call: okhttp3.Call) {
                                    tlsStart = SystemClock.elapsedRealtimeNanos()
                                }
                                override fun secureConnectEnd(call: okhttp3.Call, handshake: okhttp3.Handshake?) {
                                    PerformanceProfiler.logEvent("TLS Handshake", info = "${(SystemClock.elapsedRealtimeNanos() - tlsStart) / 1_000_000}ms", category = PerformanceProfiler.Category.NETWORK)
                                }
                                override fun connectStart(call: okhttp3.Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy) {
                                    connectStart = SystemClock.elapsedRealtimeNanos()
                                }
                                override fun connectEnd(call: okhttp3.Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy, protocol: okhttp3.Protocol?) {
                                    PerformanceProfiler.logEvent("TCP Connected: ${inetSocketAddress.hostName}", info = "${(SystemClock.elapsedRealtimeNanos() - connectStart) / 1_000_000}ms", category = PerformanceProfiler.Category.NETWORK)
                                }
                                override fun requestHeadersStart(call: okhttp3.Call) {
                                    requestStart = SystemClock.elapsedRealtimeNanos()
                                }
                                override fun responseHeadersEnd(call: okhttp3.Call, response: okhttp3.Response) {
                                    PerformanceProfiler.logEvent("HTTP Response: ${call.request().url.encodedPath}", info = "${(SystemClock.elapsedRealtimeNanos() - requestStart) / 1_000_000}ms | Code: ${response.code}", category = PerformanceProfiler.Category.NETWORK)
                                }
                            }
                        }
                    })

                    dispatcher(okhttp3.Dispatcher().apply {
                        maxRequests = 128
                        maxRequestsPerHost = 32
                    })
                }
            }
            install(Auth) {
                sessionManager = DataStoreSessionManager(prefs)
                scheme = "keeftalk"
                host = "reset-password"
            }
            install(Postgrest)
            install(Functions)
            install(Realtime) {
                heartbeatInterval = 20.seconds
                reconnectDelay = 5.seconds
            }
            install(Storage)
        }
        return newClient
    }

    @Volatile
    private var adminSupabaseClient: SupabaseClient? = null

    fun provideAdminSupabaseClient(): SupabaseClient {
        return adminSupabaseClient ?: synchronized(adminSupabaseLock) {
            adminSupabaseClient ?: run {
                PerformanceProfiler.startStage("Admin Supabase Initialization")
                val client = createSupabaseClient(
                    supabaseUrl = SUPABASE_URL,
                    supabaseKey = SUPABASE_ANON_KEY
                ) {
                    requestTimeout = 90.seconds
                    httpEngine = OkHttp.create {
                        config {
                            connectTimeout(60, TimeUnit.SECONDS)
                            readTimeout(60, TimeUnit.SECONDS)
                            writeTimeout(60, TimeUnit.SECONDS)
                            retryOnConnectionFailure(true)
                        }
                    }
                    install(Postgrest)
                    install(Functions)
                    install(Storage)
                }
                PerformanceProfiler.endStage("Admin Supabase Initialization")
                client
            }.also { adminSupabaseClient = it }
        }
    }

    @Volatile
    private var debugSupportDb: SupportSQLiteDatabase? = null

    fun explainQuery(sql: String) {
        val db = debugSupportDb ?: return
        tn.keeftalk.chat.util.KeeftalkExecutors.BOUNDED_IO.execute {
            try {
                db.query("EXPLAIN QUERY PLAN $sql").use { cursor ->
                    val sb = StringBuilder("EXPLAIN QUERY PLAN results:\n")
                    while (cursor.moveToNext()) {
                        sb.append("• ").append(cursor.getString(3)).append("\n")
                    }
                    PerformanceProfiler.logEvent("SQL Explain Output", info = sb.toString(), category = PerformanceProfiler.Category.DATABASE)
                }
            } catch (e: Exception) {
                Log.e("AppModule", "EXPLAIN failed", e)
            }
        }
    }

    fun provideDatabase(context: Context): KeeftalkDatabase {
        val db = database
        if (db != null) return db
        return synchronized(databaseLock) {
            database ?: run {
                PerformanceProfiler.startStage("Database Initialization")
                
                val dbName = "keeftalk-db-v2"
                
                // Initialize SQLCipher native libraries
                try {
                    net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
                } catch (e: Throwable) {
                    Log.e("AppModule", "Failed to load SQLCipher libs", e)
                }

                fun performDatabasePreFlightBackup() {
                    try {
                        val dbFile = context.getDatabasePath(dbName)
                        if (!dbFile.exists()) return

                        val backupDir = File(context.filesDir, "db_backups")
                        if (!backupDir.exists()) backupDir.mkdirs()

                        val backupDbFile = File(backupDir, "$dbName.bak")
                        val backupWalFile = File(backupDir, "$dbName-wal.bak")
                        val backupShmFile = File(backupDir, "$dbName-shm.bak")

                        val walFile = File(dbFile.path + "-wal")
                        val shmFile = File(dbFile.path + "-shm")

                        // Copy main DB file and WAL/SHM files first before any database open attempt
                        dbFile.copyTo(backupDbFile, overwrite = true)

                        if (walFile.exists()) {
                            walFile.copyTo(backupWalFile, overwrite = true)
                        }
                        if (shmFile.exists()) {
                            shmFile.copyTo(backupShmFile, overwrite = true)
                        }

                        // Try to perform WAL checkpoint if file can be opened using SQLCipher
                        try {
                            val passphrase = DatabaseKeyManager.getDatabasePassphrase(context)
                            val sqDb = net.sqlcipher.database.SQLiteDatabase.openDatabase(
                                dbFile.path,
                                passphrase,
                                null,
                                net.sqlcipher.database.SQLiteDatabase.OPEN_READWRITE
                            )
                            val cursor = sqDb.rawQuery("PRAGMA wal_checkpoint(FULL)", null)
                            cursor.use { if (it.moveToFirst()) { Log.i("AppModule", "WAL Checkpoint result: ${it.getInt(0)}") } }
                            sqDb.close()
                        } catch (e: Exception) {
                            Log.w("AppModule", "WAL checkpoint prior to backup skipped: ${e.message}")
                        }

                        // Verify backup can actually be opened and queried using SQLCipher
                        try {
                            val passphrase = DatabaseKeyManager.getDatabasePassphrase(context)
                            val verifiedSqDb = net.sqlcipher.database.SQLiteDatabase.openDatabase(
                                backupDbFile.path,
                                passphrase,
                                null,
                                net.sqlcipher.database.SQLiteDatabase.OPEN_READONLY
                            )
                            val verifyCursor = verifiedSqDb.rawQuery("SELECT count(*) FROM sqlite_master", null)
                            verifyCursor.use {
                                if (it.moveToFirst()) {
                                    Log.i("AppModule", "Database pre-flight backup verified successfully (${it.getInt(0)} tables found).")
                                }
                            }
                            verifiedSqDb.close()
                        } catch (e: Exception) {
                            Log.w("AppModule", "Backup verification skipped or failed: ${e.message}")
                        }
                    } catch (e: Exception) {
                        Log.e("AppModule", "Pre-flight database backup failed, preserving original files", e)
                    }
                }

                // Execute WAL-checkpointed pre-flight backup before Room open
                performDatabasePreFlightBackup()

                fun buildDatabase(): KeeftalkDatabase {
                    val passphrase = DatabaseKeyManager.getDatabasePassphrase(context)
                    val factory = SupportFactory(passphrase.toByteArray(Charsets.UTF_8))
                    
                    return Room.databaseBuilder(
                        context.applicationContext,
                        KeeftalkDatabase::class.java, dbName
                    ).addCallback(KeeftalkDatabase.CALLBACK)
                        .addMigrations(
                            KeeftalkDatabase.MIGRATION_62_63, 
                            KeeftalkDatabase.MIGRATION_63_64, 
                            KeeftalkDatabase.MIGRATION_64_65,
                            KeeftalkDatabase.MIGRATION_65_66,
                            KeeftalkDatabase.MIGRATION_66_67,
                            KeeftalkDatabase.MIGRATION_67_68,
                            KeeftalkDatabase.MIGRATION_68_69,
                            KeeftalkDatabase.MIGRATION_69_70,
                            KeeftalkDatabase.MIGRATION_70_71,
                            KeeftalkDatabase.MIGRATION_71_72,
                            KeeftalkDatabase.MIGRATION_72_73,
                            KeeftalkDatabase.MIGRATION_73_74,
                            KeeftalkDatabase.MIGRATION_74_82,
                            KeeftalkDatabase.MIGRATION_82_83,
                            KeeftalkDatabase.MIGRATION_83_84,
                            KeeftalkDatabase.MIGRATION_84_85,
                            KeeftalkDatabase.MIGRATION_85_86,
                            KeeftalkDatabase.MIGRATION_86_87,
                            KeeftalkDatabase.MIGRATION_87_88,
                            KeeftalkDatabase.MIGRATION_88_89,
                            KeeftalkDatabase.MIGRATION_89_90,
                            KeeftalkDatabase.MIGRATION_90_91,
                            KeeftalkDatabase.MIGRATION_91_92,
                            KeeftalkDatabase.MIGRATION_92_93,
                            KeeftalkDatabase.MIGRATION_93_94,
                            KeeftalkDatabase.MIGRATION_94_95,
                            KeeftalkDatabase.MIGRATION_95_96,
                            KeeftalkDatabase.MIGRATION_96_100,
                            KeeftalkDatabase.MIGRATION_100_101,
                            KeeftalkDatabase.MIGRATION_101_102,
                            KeeftalkDatabase.MIGRATION_102_103,
                            KeeftalkDatabase.MIGRATION_103_104,
                            KeeftalkDatabase.MIGRATION_104_105,
                            KeeftalkDatabase.MIGRATION_105_106,
                            KeeftalkDatabase.MIGRATION_106_107,
                            KeeftalkDatabase.MIGRATION_107_108,
                            KeeftalkDatabase.MIGRATION_108_109,
                            KeeftalkDatabase.MIGRATION_109_110,
                            KeeftalkDatabase.MIGRATION_110_111,
                            KeeftalkDatabase.MIGRATION_111_112,
                            KeeftalkDatabase.MIGRATION_112_113,
                            KeeftalkDatabase.MIGRATION_113_114,
                            KeeftalkDatabase.MIGRATION_114_115,
                            KeeftalkDatabase.MIGRATION_115_116,
                            KeeftalkDatabase.MIGRATION_116_117,
                            KeeftalkDatabase.MIGRATION_117_118,
                            KeeftalkDatabase.MIGRATION_118_119,
                            KeeftalkDatabase.MIGRATION_119_120
                        )
                        .openHelperFactory(factory)
                        .addCallback(object : RoomDatabase.Callback() {
                            override fun onOpen(db: SupportSQLiteDatabase) {
                                debugSupportDb = db
                            }
                        })
                        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                        .setQueryCallback({ sqlQuery, _ ->
                            if (BuildConfig.DEBUG) {
                                PerformanceProfiler.logEvent("SQLite Query", info = sqlQuery.take(100), category = PerformanceProfiler.Category.DATABASE)
                            }
                        }, tn.keeftalk.chat.util.KeeftalkExecutors.BOUNDED_IO)
                        .build()
                }

                val newDb = buildDatabase()
                
                // CRITICAL: Verify if database is openable WITHOUT destructive fallback
                try {
                    newDb.openHelper.readableDatabase
                    Log.i("AppModule", "Database opened successfully.")
                } catch (e: Exception) {
                    Log.e("AppModule", "Database error detected during open. ORIGINAL DATABASE AND VERIFIED BACKUP REMAIN PRESERVED ON DISK.", e)
                    // NEVER SILENTLY DELETE THE USER DATABASE OR CALL deleteDatabase()
                }

                database = newDb
                databaseDeferred.complete(newDb)
                PerformanceProfiler.endStage("Database Initialization", category = PerformanceProfiler.Category.DATABASE)
                newDb
            }
        }
    }

    suspend fun provideDatabaseAsync(context: Context): KeeftalkDatabase {
        val db = database
        if (db != null) return db
        // Trigger init if not started
        tn.keeftalk.chat.util.KeeftalkExecutors.BOUNDED_IO.execute { provideDatabase(context) }
        return databaseDeferred.await()
    }

    fun provideMediaEncryptionManager(context: Context): tn.keeftalk.chat.security.crypto.MediaEncryptionManager {
        return mediaEncryptionManager ?: synchronized(this) {
            mediaEncryptionManager ?: tn.keeftalk.chat.security.crypto.MediaEncryptionManager(
                provideCryptoManager(context)
            ).also { mediaEncryptionManager = it }
        }
    }

    fun provideSecureBackupManager(context: Context): tn.keeftalk.chat.security.crypto.SecureBackupManager {
        return secureBackupManager ?: synchronized(this) {
            secureBackupManager ?: tn.keeftalk.chat.security.crypto.SecureBackupManager(
                context.applicationContext,
                provideConversationKeyManager(context)
            ).also { secureBackupManager = it }
        }
    }

    fun provideTranslationManager(): tn.keeftalk.chat.util.TranslationManager {
        return translationManager ?: synchronized(translationLock) {
            translationManager ?: tn.keeftalk.chat.util.TranslationManager().also { translationManager = it }
        }
    }

    fun provideConnectivityObserver(context: Context): tn.keeftalk.chat.util.ConnectivityObserver {
        return connectivityObserver ?: synchronized(this) {
            connectivityObserver ?: tn.keeftalk.chat.util.NetworkConnectivityObserver(context.applicationContext).also { connectivityObserver = it }
        }
    }

    fun provideConversationKeyManager(context: Context): tn.keeftalk.chat.security.crypto.ConversationKeyManager {
        return conversationKeyManager ?: synchronized(this) {
            conversationKeyManager ?: tn.keeftalk.chat.security.crypto.ConversationKeyManager(
                context.applicationContext,
                provideDatabase(context).conversationKeyDao()
            ).also { conversationKeyManager = it }
        }
    }

    fun provideCryptoManager(context: Context): tn.keeftalk.chat.security.crypto.CryptoManager {
        val currentUserId = provideUserPreferencesRepository(context).getUserIdFast() ?: "anonymous"
        
        synchronized(this) {
            val cached = cryptoManager
            if (cached != null && cached.userId != currentUserId) {
                Log.i("AppModule", "User changed, resetting Crypto dependencies")
                cryptoManager = null
                decryptionManager?.shutdown()
                decryptionManager = null
                repository = null
                authRepository = null
            }

            return cryptoManager ?: tn.keeftalk.chat.security.crypto.CryptoManager(
                provideConversationKeyManager(context),
                currentUserId
            ).also { cryptoManager = it }
        }
    }

    fun provideMessageDecryptionManager(context: Context): tn.keeftalk.chat.security.crypto.MessageDecryptionManager {
        synchronized(this) {
            // Trigger check for user change
            provideCryptoManager(context)
            
            return decryptionManager ?: run {
                val db = provideDatabase(context)
                val crypto = provideCryptoManager(context)
                tn.keeftalk.chat.security.crypto.MessageDecryptionManager(db.messageDao(), crypto)
            }.also { decryptionManager = it }
        }
    }

    fun provideChatRepository(context: Context): ChatRepository {
        synchronized(repositoryLock) {
            // User change check is implicit in provideMessageDecryptionManager
            provideMessageDecryptionManager(context)
            
            val repo = repository
            if (repo != null) return repo
            
            return run {
                PerformanceProfiler.startStage("ChatRepository Creation")
                val db = provideDatabase(context)
                val repoImpl = ChatRepositoryImpl(
                    context.applicationContext,
                    db.userDao(),
                    db.chatDao(),
                    db.messageDao(),
                    db.profileDao(),
                    db.notificationDao(),
                    db.chatListCacheDao(),
                    db.receiptSyncQueueDao(),
                    db.callLogDao(),
                    db.fileDao(),
                    provideFileUploadManager(context),
                    provideUserPreferencesRepository(context),
                    provideSettingsRepository(context),
                    provideConnectivityObserver(context),
                    tn.keeftalk.chat.util.KeeftalkExecutors.BOUNDED_IO.asCoroutineDispatcher()
                )
                PerformanceProfiler.endStage("ChatRepository Creation", category = PerformanceProfiler.Category.DI)
                repoImpl
            }.also { repository = it }
        }
    }

    fun provideAuthRepository(context: Context): AuthRepository {
        synchronized(authRepositoryLock) {
            val repo = authRepository
            if (repo != null) return repo
            
            return run {
                PerformanceProfiler.startStage("AuthRepository Creation")
                val db = provideDatabase(context)
                val repoImpl = AuthRepositoryImpl(
                    context.applicationContext,
                    db.profileDao(),
                    db.userDao(),
                    provideUserPreferencesRepository(context)
                )
                PerformanceProfiler.endStage("AuthRepository Creation")
                repoImpl
            }.also { authRepository = it }
        }
    }

    fun clearAllData(context: Context) {
        PerformanceProfiler.logEvent("Clearing all local data")
        
        // 1. Shut down all active components to stop background operations
        synchronized(this) {
            tn.keeftalk.chat.util.KeeftalkStore.shutdown()
            tn.keeftalk.chat.util.AppDependencies.reset()
            decryptionManager?.shutdown()
            repository?.shutdown()
            authRepository?.shutdown()
            chatSettingsRepository?.shutdown()
            appCustomizationRepository?.shutdown()
            privacyRepository?.shutdown()
            relationshipRepository?.shutdown()
            securityRepository?.shutdown()
            calendarRepository?.shutdown()
            noteRepository?.shutdown()
            vaultRepository?.shutdown()
            emailRepository?.shutdown()
            feedRepository?.shutdown()
            callLogManager?.shutdown()
            backgroundSyncManager?.shutdown()
            telephonySyncManager?.stopSync()
        }

        // 2. Clear all databases
        synchronized(databaseLock) {
            database?.close()
            database = null
        }
        context.databaseList().forEach { dbName ->
            Log.d("AppModule", "Deleting database: $dbName")
            if (dbName.endsWith("-journal") || dbName.endsWith("-wal") || dbName.endsWith("-shm")) {
                try {
                    val dbFile = context.getDatabasePath(dbName)
                    if (dbFile.exists()) dbFile.delete()
                } catch (_: Exception) {}
            } else {
                context.deleteDatabase(dbName)
                try {
                    val dbFile = context.getDatabasePath(dbName)
                    if (dbFile.exists()) dbFile.delete()
                    val journalFile = File(dbFile.parentFile, "${dbFile.name}-journal")
                    if (journalFile.exists()) journalFile.delete()
                    val walFile = File(dbFile.parentFile, "${dbFile.name}-wal")
                    if (walFile.exists()) walFile.delete()
                    val shmFile = File(dbFile.parentFile, "${dbFile.name}-shm")
                    if (shmFile.exists()) shmFile.delete()
                } catch (e: Exception) {
                    Log.w("AppModule", "Warning deleting database files for $dbName: ${e.message}")
                }
            }
        }

        // 3. Clear all shared preferences
        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
            sharedPrefsDir.listFiles()?.forEach { file ->
                val name = file.name.removeSuffix(".xml")
                Log.d("AppModule", "Clearing shared preferences: $name")
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) { clear() }
                if (!file.delete()) {
                    Log.w("AppModule", "Could not delete shared preference file: ${file.name}")
                }
            }
        }

        // 4. Clear Datastore and Files
        context.filesDir.listFiles()?.forEach { 
            Log.d("AppModule", "Deleting file/dir: ${it.name}")
            if (!it.deleteRecursively()) {
                throw Exception("Failed to delete file/directory: ${it.name}")
            }
        }
        
        val dataStoreDir = File(context.filesDir.parentFile, "datastore")
        if (dataStoreDir.exists()) {
            if (!dataStoreDir.deleteRecursively()) {
                throw Exception("Failed to delete datastore directory")
            }
        }
        
        context.getExternalFilesDir(null)?.listFiles()?.forEach { 
            if (!it.deleteRecursively()) {
                Log.w("AppModule", "Could not delete external file: ${it.name}")
            }
        }

        // 5. Clear Cache
        context.cacheDir.listFiles()?.forEach { 
            if (!it.deleteRecursively()) {
                Log.w("AppModule", "Could not delete cache file: ${it.name}")
            }
        }
        context.externalCacheDir?.listFiles()?.forEach { 
            if (!it.deleteRecursively()) {
                Log.w("AppModule", "Could not delete external cache file: ${it.name}")
            }
        }

        // 6. Reset all singleton references
        synchronized(this) {
            repository = null
            authRepository = null
            translationManager = null
            cryptoManager = null
            decryptionManager = null
            privacyRepository = null
            countryService = null
            phoneNumberService = null
            legalRepository = null
            securityRepository = null
            chatSettingsRepository = null
            appCustomizationRepository = null
            mediaRepository = null
            relationshipRepository = null
            mediaExportPipeline = null
            noteRepository = null
            vaultRepository = null
            calendarRepository = null
            callLogManager = null
            phoneContactManager = null
            smsRepository = null
            walletRepository = null
            emailRepository = null
            emailAuthManager = null
            feedRepository = null
            locationService = null
            telephonySyncManager = null
            backgroundSyncManager = null
            conversationKeyManager = null
            mediaEncryptionManager = null
            secureBackupManager = null
        }
        
        synchronized(supabaseLock) { 
            supabaseClient = null 
            supabaseInitDeferred = kotlinx.coroutines.CompletableDeferred()
        }
        synchronized(adminSupabaseLock) { adminSupabaseClient = null }
        
        Log.i("AppModule", "All local data successfully cleared.")
    }

    fun provideCountryService(context: Context): tn.keeftalk.chat.domain.service.CountryService {
        return countryService ?: synchronized(this) {
            countryService ?: tn.keeftalk.chat.data.service.CountryServiceImpl(
                context.applicationContext,
                provideUserPreferencesRepository(context)
            ).also { countryService = it }
        }
    }

    fun providePhoneNumberService(context: Context): tn.keeftalk.chat.domain.service.PhoneNumberService {
        return phoneNumberService ?: synchronized(this) {
            phoneNumberService ?: tn.keeftalk.chat.data.service.PhoneNumberServiceImpl(
                context.applicationContext
            ).also { phoneNumberService = it }
        }
    }

    fun provideLegalRepository(context: Context): tn.keeftalk.chat.domain.repository.LegalRepository {
        return legalRepository ?: synchronized(this) {
            legalRepository ?: tn.keeftalk.chat.data.repository.LegalRepositoryImpl(
                context.applicationContext
            ).also { legalRepository = it }
        }
    }

    fun providePrivacyRepository(context: Context): tn.keeftalk.chat.domain.repository.PrivacyRepository {
        return privacyRepository ?: synchronized(this) {
            privacyRepository ?: tn.keeftalk.chat.data.repository.PrivacyRepositoryImpl(
                context.applicationContext,
                provideUserPreferencesRepository(context)
            ).also { privacyRepository = it }
        }
    }

    @Volatile
    private var userPreferencesRepository: tn.keeftalk.chat.data.prefs.UserPreferencesRepository? = null

    fun provideUserPreferencesRepository(context: Context): tn.keeftalk.chat.data.prefs.UserPreferencesRepository {
        return userPreferencesRepository ?: synchronized(this) {
            userPreferencesRepository ?: tn.keeftalk.chat.data.prefs.UserPreferencesRepository(context.applicationContext).also { userPreferencesRepository = it }
        }
    }

    fun provideParentalControlManager(context: Context): tn.keeftalk.chat.util.ParentalControlManager {
        return parentalControlManager ?: synchronized(this) {
            parentalControlManager ?: tn.keeftalk.chat.util.ParentalControlManager(
                context.applicationContext,
                provideUserPreferencesRepository(context)
            ).also { parentalControlManager = it }
        }
    }

    fun provideSecurityRepository(context: Context): tn.keeftalk.chat.domain.repository.SecurityRepository {
        return securityRepository ?: synchronized(this) {
            securityRepository ?: tn.keeftalk.chat.data.repository.SecurityRepositoryImpl(
                context.applicationContext,
                provideUserPreferencesRepository(context)
            ).also { securityRepository = it }
        }
    }

    fun provideTwoFactorRepository(context: Context): tn.keeftalk.chat.domain.repository.TwoFactorRepository {
        return twoFactorRepository ?: synchronized(this) {
            twoFactorRepository ?: tn.keeftalk.chat.data.repository.TwoFactorRepositoryImpl(
                context.applicationContext
            ).also { twoFactorRepository = it }
        }
    }

    fun provideBugReportRepository(context: Context): tn.keeftalk.chat.domain.repository.BugReportRepository {
        return bugReportRepository ?: synchronized(this) {
            bugReportRepository ?: tn.keeftalk.chat.data.repository.BugReportRepositoryImpl(
                context.applicationContext
            ).also { bugReportRepository = it }
        }
    }

    fun provideChatSettingsRepository(context: Context): tn.keeftalk.chat.domain.repository.ChatSettingsRepository {
        return chatSettingsRepository ?: synchronized(this) {
            chatSettingsRepository ?: tn.keeftalk.chat.data.repository.ChatSettingsRepositoryImpl(
                context.applicationContext,
                provideUserPreferencesRepository(context)
            ).also { chatSettingsRepository = it }
        }
    }

    fun provideSettingsRepository(context: Context): tn.keeftalk.chat.domain.repository.SettingsRepository {
        return settingsRepository ?: synchronized(this) {
            settingsRepository ?: tn.keeftalk.chat.data.repository.SettingsRepositoryImpl(
                context.applicationContext,
                provideUserPreferencesRepository(context)
            ).also { settingsRepository = it }
        }
    }

    fun provideAppCustomizationRepository(context: Context): tn.keeftalk.chat.domain.repository.AppCustomizationRepository {
        return appCustomizationRepository ?: synchronized(this) {
            appCustomizationRepository ?: tn.keeftalk.chat.data.repository.AppCustomizationRepositoryImpl(
                context.applicationContext,
                provideUserPreferencesRepository(context)
            ).also { appCustomizationRepository = it }
        }
    }

    fun provideMediaRepository(context: Context): tn.keeftalk.chat.domain.repository.MediaRepository {
        return mediaRepository ?: synchronized(this) {
            mediaRepository ?: tn.keeftalk.chat.data.repository.MediaRepositoryImpl(
                context.applicationContext
            ).also { mediaRepository = it }
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun provideMediaExportPipeline(context: Context): tn.keeftalk.chat.data.service.MediaExportPipeline {
        return mediaExportPipeline ?: synchronized(this) {
            mediaExportPipeline ?: tn.keeftalk.chat.data.service.MediaExportPipeline(
                context.applicationContext,
                provideChatRepository(context)
            ).also { mediaExportPipeline = it }
        }
    }

    private var todoRepository: tn.keeftalk.chat.domain.repository.TodoRepository? = null

    fun provideTodoRepository(context: Context): tn.keeftalk.chat.domain.repository.TodoRepository {
        return todoRepository ?: synchronized(this) {
            todoRepository ?: run {
                val db = provideDatabase(context)
                tn.keeftalk.chat.data.repository.TodoRepositoryImpl(
                    context.applicationContext,
                    db.todoDao(),
                    tn.keeftalk.chat.data.remote.TodoRemoteDataSourceImpl(context.applicationContext)
                )
            }.also { todoRepository = it }
        }
    }

    fun provideNoteRepository(context: Context): tn.keeftalk.chat.domain.repository.NoteRepository {
        return noteRepository ?: synchronized(this) {
            noteRepository ?: run {
                val db = provideDatabase(context)
                val prefs = provideUserPreferencesRepository(context)
                tn.keeftalk.chat.data.repository.NoteRepositoryImpl(
                    context.applicationContext,
                    db,
                    db.syncQueueDao(),
                    db.fileDao(),
                    provideFileUploadManager(context),
                    prefs,
                    provideAuthRepository(context)
                )
            }.also { noteRepository = it }
        }
    }

    fun provideVaultRepository(context: Context): tn.keeftalk.chat.domain.repository.VaultRepository {
        return vaultRepository ?: synchronized(this) {
            val db = provideDatabase(context)
            vaultRepository ?: tn.keeftalk.chat.data.repository.VaultRepositoryImpl(
                context.applicationContext,
                db.vaultDao(),
                db.vaultSyncQueueDao(),
                db.fileDao(),
                provideFileUploadManager(context),
                provideAuthRepository(context),
                provideR2StorageClient(context)
            ).also { vaultRepository = it }
        }
    }

    private var secretsRepository: tn.keeftalk.chat.feature.secrets.repository.SecretsRepository? = null

    fun provideSecretsDao(context: Context): tn.keeftalk.chat.feature.secrets.data.local.dao.SecretsDao {
        return provideDatabase(context).secretsDao()
    }

    fun provideSecretsRepository(context: Context): tn.keeftalk.chat.feature.secrets.repository.SecretsRepository {
        return secretsRepository ?: synchronized(this) {
            secretsRepository ?: tn.keeftalk.chat.feature.secrets.repository.SecretsRepositoryImpl(
                provideSecretsDao(context)
            ).also { secretsRepository = it }
        }
    }

    fun provideBookRepository(context: Context): tn.keeftalk.chat.domain.repository.BookRepository {
        return bookRepository ?: synchronized(this) {
            val db = provideDatabase(context)
            bookRepository ?: tn.keeftalk.chat.data.repository.BookRepositoryImpl(
                db.bookDao(),
                context.applicationContext
            ).also { bookRepository = it }
        }
    }

    fun provideDocDao(context: Context): tn.keeftalk.chat.data.local.dao.DocDao {
        return provideDatabase(context).docDao()
    }

    fun provideDocRepository(context: Context): tn.keeftalk.chat.domain.repository.DocRepository {
        return docRepository ?: synchronized(this) {
            val db = provideDatabase(context)
            docRepository ?: tn.keeftalk.chat.data.repository.DocRepositoryImpl(
                db.docDao(),
                context.applicationContext
            ).also { docRepository = it }
        }
    }

    fun provideHabitsRepository(context: Context): tn.keeftalk.chat.domain.repository.HabitsRepository {
        return habitsRepository ?: synchronized(this) {
            val db = provideDatabase(context)
            habitsRepository ?: tn.keeftalk.chat.data.repository.HabitsRepositoryImpl(
                context = context.applicationContext,
                nativeHabitDao = db.nativeHabitDao(),
                remoteDataSource = tn.keeftalk.chat.data.remote.HabitsRemoteDataSourceNoOp()
            ).also { habitsRepository = it }
        }
    }

    fun provideBookHabitIntegrationManager(context: Context): tn.keeftalk.chat.domain.engine.BookHabitIntegrationManager {
        return bookHabitIntegrationManager ?: synchronized(this) {
            val habitsRepo = provideHabitsRepository(context)
            val bookRepo = provideBookRepository(context)
            bookHabitIntegrationManager ?: tn.keeftalk.chat.domain.engine.BookHabitIntegrationManager(
                habitsRepository = habitsRepo,
                bookRepository = bookRepo
            ).also { bookHabitIntegrationManager = it }
        }
    }

    private var readingXpRepository: tn.keeftalk.chat.data.repository.ReadingXpRepository? = null

    fun provideReadingXpRepository(context: Context): tn.keeftalk.chat.data.repository.ReadingXpRepository {
        return readingXpRepository ?: synchronized(this) {
            val db = provideDatabase(context)
            readingXpRepository ?: tn.keeftalk.chat.data.repository.ReadingXpRepository(
                db.xpDao()
            ).also { readingXpRepository = it }
        }
    }

    fun provideVaultBackupDao(context: Context): tn.keeftalk.chat.data.local.dao.VaultBackupDao {
        return provideDatabase(context).vaultBackupDao()
    }

    fun provideVaultBackupManager(context: Context): tn.keeftalk.chat.data.service.VaultBackupManager {
        return vaultBackupManager ?: synchronized(this) {
            val db = provideDatabase(context)
            vaultBackupManager ?: tn.keeftalk.chat.data.service.VaultBackupManager(
                context.applicationContext,
                db.vaultBackupDao(),
                provideUserPreferencesRepository(context),
                provideVaultRepository(context),
                provideFileRepository(context),
                provideAuthRepository(context),
                provideConnectivityObserver(context)
            ).also { vaultBackupManager = it }
        }
    }

    fun provideCalendarRepository(context: Context): tn.keeftalk.chat.domain.repository.CalendarRepository {
        return calendarRepository ?: synchronized(this) {
            val db = provideDatabase(context)
            calendarRepository ?: tn.keeftalk.chat.data.repository.CalendarRepositoryImpl(
                context.applicationContext,
                db.calendarDao(),
                db.calendarSyncQueueDao(),
                db.fileDao(),
                provideFileUploadManager(context)
            ).also { calendarRepository = it }
        }
    }

    fun provideCallLogManager(context: Context): tn.keeftalk.chat.data.local.CallLogManager {
        return callLogManager ?: synchronized(this) {
            val db = provideDatabase(context)
            callLogManager ?: tn.keeftalk.chat.data.local.CallLogManager(
                context.applicationContext,
                provideChatRepository(context),
                db.callLogDao()
            ).also { callLogManager = it }
        }
    }

    fun providePhoneContactManager(context: Context): tn.keeftalk.chat.data.local.PhoneContactManager {
        return phoneContactManager ?: synchronized(this) {
            phoneContactManager ?: tn.keeftalk.chat.data.local.PhoneContactManager(
                context.applicationContext
            ).also { phoneContactManager = it }
        }
    }

    fun provideSmsRepository(context: Context): tn.keeftalk.chat.domain.repository.SmsRepository {
        return smsRepository ?: synchronized(this) {
            smsRepository ?: tn.keeftalk.chat.data.repository.SmsRepositoryImpl(
                context.applicationContext,
                provideDatabase(context).smsDao()
            ).also { smsRepository = it }
        }
    }

    fun provideWalletRepository(context: Context): tn.keeftalk.chat.feature.wallet.repository.WalletRepository {
        return walletRepository ?: synchronized(this) {
            walletRepository ?: tn.keeftalk.chat.feature.wallet.repository.WalletRepositoryImpl(
                provideDatabase(context).financesDao()
            ).also { walletRepository = it }
        }
    }

    fun provideFinancesRepository(context: Context): tn.keeftalk.chat.feature.finances.repository.FinancesRepository {
        return financesRepository ?: synchronized(this) {
            financesRepository ?: tn.keeftalk.chat.feature.finances.repository.FinancesRepositoryImpl(
                provideDatabase(context).financesDao()
            ).also { financesRepository = it }
        }
    }

    fun provideEmailRepository(context: Context): tn.keeftalk.chat.feature.email.repository.EmailRepository {
        return emailRepository ?: synchronized(this) {
            emailRepository ?: run {
                val db = provideDatabase(context)
                val mailDao = db.mailDao()
                val secureStore = tn.keeftalk.chat.feature.email.data.local.EmailSecureStore(context.applicationContext)
                val gmailSyncer = tn.keeftalk.chat.feature.email.data.sync.providers.GmailSyncer(context.applicationContext, mailDao, secureStore)
                val outlookSyncer = tn.keeftalk.chat.feature.email.data.sync.providers.OutlookSyncer(mailDao, secureStore)
                val imapSyncer = tn.keeftalk.chat.feature.email.data.sync.providers.ImapSyncer(mailDao, secureStore)
                val syncManager = tn.keeftalk.chat.feature.email.data.sync.MailSyncManager(
                    context.applicationContext,
                    mailDao,
                    gmailSyncer,
                    outlookSyncer,
                    imapSyncer
                )
                tn.keeftalk.chat.feature.email.repository.EmailRepositoryImpl(context.applicationContext, mailDao, syncManager, secureStore)
            }.also { emailRepository = it }
        }
    }

    fun provideEmailAuthManager(context: Context): tn.keeftalk.chat.feature.email.auth.EmailAuthManager {
        return emailAuthManager ?: synchronized(this) {
            emailAuthManager ?: tn.keeftalk.chat.feature.email.auth.EmailAuthManager(context.applicationContext).also { emailAuthManager = it }
        }
    }

    fun provideCloudAuthManager(context: Context): tn.keeftalk.chat.data.auth.CloudAuthManager {
        return cloudAuthManager ?: synchronized(this) {
            cloudAuthManager ?: tn.keeftalk.chat.data.auth.CloudAuthManager(context.applicationContext).also { cloudAuthManager = it }
        }
    }

    fun provideTelephonySyncManager(context: Context): tn.keeftalk.chat.data.sync.TelephonySyncManager {
        return telephonySyncManager ?: synchronized(this) {
            telephonySyncManager ?: tn.keeftalk.chat.data.sync.TelephonySyncManager(
                context.applicationContext,
                provideDatabase(context).smsDao(),
                provideChatRepository(context)
            ).also { telephonySyncManager = it }
        }
    }

    fun provideBackgroundSyncManager(context: Context): tn.keeftalk.chat.data.sync.BackgroundSyncManager {
        return backgroundSyncManager ?: synchronized(this) {
            backgroundSyncManager ?: tn.keeftalk.chat.data.sync.BackgroundSyncManager(
                context.applicationContext,
                provideChatRepository(context),
                provideCalendarRepository(context),
                provideVaultRepository(context)
            ).also { backgroundSyncManager = it }
        }
    }

    fun provideFileRepository(context: Context): tn.keeftalk.chat.domain.repository.FileRepository {
        return fileRepository ?: synchronized(this) {
            fileRepository ?: run {
                val db = provideDatabase(context)
                tn.keeftalk.chat.data.repository.FileRepositoryImpl(
                    context.applicationContext,
                    db.fileDao()
                )
            }.also { fileRepository = it }
        }
    }

    @Volatile
    private var r2StorageClient: tn.keeftalk.chat.util.R2StorageClient? = null

    fun provideR2StorageClient(context: Context): tn.keeftalk.chat.util.R2StorageClient {
        return r2StorageClient ?: synchronized(this) {
            r2StorageClient ?: tn.keeftalk.chat.util.R2StorageClient(
                { provideSupabaseClientAsync(context) }
            ).also { r2StorageClient = it }
        }
    }

    fun provideFileReferenceManager(context: Context): tn.keeftalk.chat.util.FileReferenceManager {
        return fileReferenceManager ?: synchronized(this) {
            fileReferenceManager ?: tn.keeftalk.chat.util.FileReferenceManager(
                provideFileRepository(context),
                { provideSupabaseClientAsync(context) },
                provideR2StorageClient(context)
            ).also { fileReferenceManager = it }
        }
    }

    fun provideFileUploadManager(context: Context): tn.keeftalk.chat.util.FileUploadManager {
        return fileUploadManager ?: synchronized(this) {
            fileUploadManager ?: tn.keeftalk.chat.util.FileUploadManager(
                context.applicationContext,
                provideFileRepository(context),
                provideAuthRepository(context),
                provideSecurityManager(context),
                { provideSupabaseClientAsync(context) },
                provideR2StorageClient(context)
            ).also { fileUploadManager = it }
        }
    }

    fun provideFileDownloadManager(context: Context): tn.keeftalk.chat.util.FileDownloadManager {
        return fileDownloadManager ?: synchronized(this) {
            fileDownloadManager ?: tn.keeftalk.chat.util.FileDownloadManager(
                context.applicationContext,
                provideCryptoManager(context),
                provideAuthRepository(context),
                { provideSupabaseClientAsync(context) },
                provideR2StorageClient(context)
            ).also { fileDownloadManager = it }
        }
    }

    fun provideRelationshipRepository(context: Context): tn.keeftalk.chat.domain.repository.RelationshipRepository {
        return relationshipRepository ?: synchronized(this) {
            relationshipRepository ?: tn.keeftalk.chat.data.repository.RelationshipRepositoryImpl(
                context.applicationContext,
                provideChatRepository(context),
                providePrivacyRepository(context),
                provideDatabase(context).relationshipDao()
            ).also { relationshipRepository = it }
        }
    }

    fun provideCloudImportCoordinator(context: Context): tn.keeftalk.chat.util.CloudImportCoordinator {
        return cloudImportCoordinator ?: synchronized(this) {
            cloudImportCoordinator ?: tn.keeftalk.chat.util.CloudImportCoordinator(
                context.applicationContext,
                provideVaultRepository(context)
            ).also { cloudImportCoordinator = it }
        }
    }

    fun provideCloudSecureStore(context: Context): tn.keeftalk.chat.data.local.CloudSecureStore {
        return cloudSecureStore ?: synchronized(this) {
            cloudSecureStore ?: tn.keeftalk.chat.data.local.CloudSecureStore(context.applicationContext).also { cloudSecureStore = it }
        }
    }

    fun provideGooglePhotosService(context: Context): tn.keeftalk.chat.data.cloud.GooglePhotosService {
        return googlePhotosService ?: synchronized(this) {
            googlePhotosService ?: run {
                val client = io.ktor.client.HttpClient(io.ktor.client.engine.okhttp.OkHttp) {
                    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                        json(kotlinx.serialization.json.Json { ignoreUnknownKeys = true })
                    }
                }
                tn.keeftalk.chat.data.cloud.GooglePhotosService(client)
            }.also { googlePhotosService = it }
        }
    }

    fun provideGoogleDriveService(context: Context): tn.keeftalk.chat.data.cloud.GoogleDriveService {
        return googleDriveService ?: synchronized(this) {
            googleDriveService ?: run {
                val client = io.ktor.client.HttpClient(io.ktor.client.engine.okhttp.OkHttp) {
                    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                        json(kotlinx.serialization.json.Json { ignoreUnknownKeys = true })
                    }
                }
                tn.keeftalk.chat.data.cloud.GoogleDriveService(client)
            }.also { googleDriveService = it }
        }
    }

    fun provideDropboxService(): tn.keeftalk.chat.data.cloud.DropboxService {
        return dropboxService ?: synchronized(this) {
            dropboxService ?: tn.keeftalk.chat.data.cloud.DropboxService().also { dropboxService = it }
        }
    }

    fun provideFeedRepository(context: Context): tn.keeftalk.chat.domain.repository.FeedRepository {
        return feedRepository ?: synchronized(this) {
            feedRepository ?: run {
                val db = provideDatabase(context)
                tn.keeftalk.chat.data.repository.FeedRepositoryImpl(
                    context.applicationContext,
                    db.feedDao(),
                    okhttp3.OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build(),
                    provideLocationService(context),
                    provideUserPreferencesRepository(context)
                )
            }.also { feedRepository = it }
        }
    }

    fun provideWeatherRepository(context: Context): tn.keeftalk.chat.domain.repository.WeatherRepository {
        return weatherRepository ?: synchronized(this) {
            weatherRepository ?: tn.keeftalk.chat.data.repository.WeatherRepositoryImpl(
                provideFeedRepository(context),
                context.applicationContext
            ).also { weatherRepository = it }
        }
    }

    fun provideLocationService(context: Context): tn.keeftalk.chat.domain.service.LocationService {
        return locationService ?: synchronized(this) {
            locationService ?: tn.keeftalk.chat.data.service.LocationServiceImpl(context.applicationContext).also { locationService = it }
        }
    }

    fun provideBillingManager(context: Context): tn.keeftalk.chat.data.billing.BillingManager {
        return billingManager ?: synchronized(this) {
            billingManager ?: tn.keeftalk.chat.data.billing.BillingManager(context.applicationContext).also { billingManager = it }
        }
    }

    fun provideEntitlementManager(context: Context): tn.keeftalk.chat.domain.engine.EntitlementManager {
        return entitlementManager ?: synchronized(this) {
            entitlementManager ?: tn.keeftalk.chat.domain.engine.EntitlementManager(context.applicationContext).also { entitlementManager = it }
        }
    }

    fun provideExtensionHost(context: Context): tn.keeftalk.chat.extension.lifecycle.ExtensionHost {
        return extensionHost ?: synchronized(this) {
            extensionHost ?: tn.keeftalk.chat.extension.lifecycle.ExtensionHost(
                context.applicationContext
            ).also { extensionHost = it }
        }
    }

    fun provideNotificationDispatcher(context: Context): tn.keeftalk.chat.util.notifications.NotificationDispatcher {
        return notificationDispatcher ?: synchronized(this) {
            notificationDispatcher ?: tn.keeftalk.chat.util.notifications.NotificationDispatcher(
                context.applicationContext,
                provideUserPreferencesRepository(context),
                provideChatRepository(context)
            ).also { notificationDispatcher = it }
        }
    }
}

