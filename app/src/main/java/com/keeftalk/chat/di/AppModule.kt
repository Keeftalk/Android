package com.keeftalk.chat.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import com.keeftalk.chat.data.auth.DataStoreSessionManager
import com.keeftalk.chat.data.local.KeeftalkDatabase
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.BuildConfig
import com.keeftalk.chat.data.repository.AuthRepositoryImpl
import com.keeftalk.chat.data.repository.ChatRepositoryImpl
import com.keeftalk.chat.domain.repository.AuthRepository
import com.keeftalk.chat.domain.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import androidx.sqlite.db.SupportSQLiteDatabase
import com.keeftalk.chat.util.PerformanceProfiler
import android.os.SystemClock
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import androidx.core.content.edit
import kotlinx.coroutines.asCoroutineDispatcher
import net.sqlcipher.database.SupportFactory
import com.keeftalk.chat.security.crypto.DatabaseKeyManager
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
    private var cryptoManager: com.keeftalk.chat.security.crypto.CryptoManager? = null

    @Volatile
    private var decryptionManager: com.keeftalk.chat.security.crypto.MessageDecryptionManager? = null

    @Volatile
    private var translationManager: com.keeftalk.chat.util.TranslationManager? = null

    @Volatile
    private var authRepository: AuthRepository? = null

    @Volatile
    private var privacyRepository: com.keeftalk.chat.domain.repository.PrivacyRepository? = null

    @Volatile
    private var countryService: com.keeftalk.chat.domain.service.CountryService? = null

    @Volatile
    private var phoneNumberService: com.keeftalk.chat.domain.service.PhoneNumberService? = null

    @Volatile
    private var legalRepository: com.keeftalk.chat.domain.repository.LegalRepository? = null

    @Volatile
    private var securityRepository: com.keeftalk.chat.domain.repository.SecurityRepository? = null

    @Volatile
    private var chatSettingsRepository: com.keeftalk.chat.domain.repository.ChatSettingsRepository? = null

    @Volatile
    private var appCustomizationRepository: com.keeftalk.chat.domain.repository.AppCustomizationRepository? = null

    @Volatile
    private var mediaRepository: com.keeftalk.chat.domain.repository.MediaRepository? = null

    @Volatile
    private var mediaExportPipeline: com.keeftalk.chat.data.service.MediaExportPipeline? = null

    @Volatile
    private var noteRepository: com.keeftalk.chat.domain.repository.NoteRepository? = null

    @Volatile
    private var vaultRepository: com.keeftalk.chat.domain.repository.VaultRepository? = null

    @Volatile
    private var calendarRepository: com.keeftalk.chat.domain.repository.CalendarRepository? = null

    @Volatile
    private var callLogManager: com.keeftalk.chat.data.local.CallLogManager? = null

    @Volatile
    private var phoneContactManager: com.keeftalk.chat.data.local.PhoneContactManager? = null

    @Volatile
    private var smsRepository: com.keeftalk.chat.domain.repository.SmsRepository? = null

    @Volatile
    private var walletRepository: com.keeftalk.chat.feature.wallet.repository.WalletRepository? = null

    @Volatile
    private var emailRepository: com.keeftalk.chat.feature.email.repository.EmailRepository? = null

    @Volatile
    private var emailAuthManager: com.keeftalk.chat.feature.email.auth.EmailAuthManager? = null

    @Volatile
    @set:android.annotation.SuppressLint("StaticFieldLeak")
    private var telephonySyncManager: com.keeftalk.chat.data.sync.TelephonySyncManager? = null

    @Volatile
    private var backgroundSyncManager: com.keeftalk.chat.data.sync.BackgroundSyncManager? = null

    @Volatile
    private var fileRepository: com.keeftalk.chat.domain.repository.FileRepository? = null

    @Volatile
    private var fileReferenceManager: com.keeftalk.chat.util.FileReferenceManager? = null

    @Volatile
    private var fileUploadManager: com.keeftalk.chat.util.FileUploadManager? = null

    @Volatile
    private var fileDownloadManager: com.keeftalk.chat.util.FileDownloadManager? = null

    @Volatile
    private var supabaseClient: SupabaseClient? = null
    private var supabaseInitDeferred = kotlinx.coroutines.CompletableDeferred<SupabaseClient>()

    @Volatile
    private var conversationKeyManager: com.keeftalk.chat.security.crypto.ConversationKeyManager? = null

    @Volatile
    private var mediaEncryptionManager: com.keeftalk.chat.security.crypto.MediaEncryptionManager? = null

    @Volatile
    private var secureBackupManager: com.keeftalk.chat.security.crypto.SecureBackupManager? = null

    private val databaseDeferred = kotlinx.coroutines.CompletableDeferred<KeeftalkDatabase>()

    private const val SUPABASE_URL = "https://lhlylcyuvfpauwvuxped.supabase.co/"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxobHlsY3l1dmZwYXV3dnV4cGVkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIyOTUzNDMsImV4cCI6MjA5Nzg3MTM0M30.vu5Zga2iX37NlDV9v0IgQp_PjDNWJmmqqhrvLJ9Un9E"
    private const val SUPABASE_SERVICE_ROLE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxobHlsY3l1dmZwYXV3dnV4cGVkIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4MjI5NTM0MywiZXhwIjoyMDk3ODcxMzQzfQ.xeN_tjRgZ0PQC0P1udZSyPJtR71q5uv2kywgKv7-fXg"

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun startSupabaseInit(context: Context) {
        synchronized(supabaseLock) {
            if (supabaseClient != null) return
            if (supabaseInitDeferred.isCompleted && supabaseInitDeferred.getCompletionExceptionOrNull() != null) {
                Log.w("AppModule", "Supabase init previously failed, resetting deferred for retry")
                supabaseInitDeferred = kotlinx.coroutines.CompletableDeferred()
            }
        }
        
        com.keeftalk.chat.util.KeeftalkExecutors.BOUNDED_IO.execute {
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
                    supabaseKey = SUPABASE_SERVICE_ROLE_KEY
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
        com.keeftalk.chat.util.KeeftalkExecutors.BOUNDED_IO.execute {
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
                            KeeftalkDatabase.MIGRATION_67_68,
                            KeeftalkDatabase.MIGRATION_68_69,
                            KeeftalkDatabase.MIGRATION_69_70,
                            KeeftalkDatabase.MIGRATION_70_71,
                            KeeftalkDatabase.MIGRATION_71_72,
                            KeeftalkDatabase.MIGRATION_72_73,
                            KeeftalkDatabase.MIGRATION_73_74
                        )
                        .openHelperFactory(factory)
                        .addCallback(object : RoomDatabase.Callback() {
                            override fun onOpen(db: SupportSQLiteDatabase) {
                                debugSupportDb = db
                            }
                        })
                        .fallbackToDestructiveMigration(true)
                        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                        .setQueryCallback({ sqlQuery, _ ->
                            if (BuildConfig.DEBUG) {
                                PerformanceProfiler.logEvent("SQLite Query", info = sqlQuery.take(100), category = PerformanceProfiler.Category.DATABASE)
                            }
                        }, com.keeftalk.chat.util.KeeftalkExecutors.BOUNDED_IO)
                        .build()
                }

                var newDb = buildDatabase()
                
                // CRITICAL: Verify if database is openable
                try {
                    newDb.openHelper.readableDatabase
                    Log.i("AppModule", "Database opened successfully.")
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    if (msg.contains("file is not a database") || msg.contains("corrupt")) {
                        Log.e("AppModule", "Database corruption detected! Attempting recovery by clearing database.", e)
                        newDb.close()
                        context.deleteDatabase(dbName)
                        // Re-build a fresh one
                        newDb = buildDatabase()
                    } else {
                        Log.e("AppModule", "Unexpected error during database open.", e)
                    }
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
        com.keeftalk.chat.util.KeeftalkExecutors.BOUNDED_IO.execute { provideDatabase(context) }
        return databaseDeferred.await()
    }

    fun provideMediaEncryptionManager(context: Context): com.keeftalk.chat.security.crypto.MediaEncryptionManager {
        return mediaEncryptionManager ?: synchronized(this) {
            mediaEncryptionManager ?: com.keeftalk.chat.security.crypto.MediaEncryptionManager(
                provideCryptoManager(context)
            ).also { mediaEncryptionManager = it }
        }
    }

    fun provideSecureBackupManager(context: Context): com.keeftalk.chat.security.crypto.SecureBackupManager {
        return secureBackupManager ?: synchronized(this) {
            secureBackupManager ?: com.keeftalk.chat.security.crypto.SecureBackupManager(
                context.applicationContext,
                provideConversationKeyManager(context)
            ).also { secureBackupManager = it }
        }
    }

    fun provideTranslationManager(): com.keeftalk.chat.util.TranslationManager {
        return translationManager ?: synchronized(translationLock) {
            translationManager ?: com.keeftalk.chat.util.TranslationManager().also { translationManager = it }
        }
    }

    fun provideConversationKeyManager(context: Context): com.keeftalk.chat.security.crypto.ConversationKeyManager {
        return conversationKeyManager ?: synchronized(this) {
            conversationKeyManager ?: com.keeftalk.chat.security.crypto.ConversationKeyManager(
                context.applicationContext,
                provideDatabase(context).conversationKeyDao()
            ).also { conversationKeyManager = it }
        }
    }

    fun provideCryptoManager(context: Context): com.keeftalk.chat.security.crypto.CryptoManager {
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

            return cryptoManager ?: com.keeftalk.chat.security.crypto.CryptoManager(
                provideConversationKeyManager(context),
                currentUserId
            ).also { cryptoManager = it }
        }
    }

    fun provideMessageDecryptionManager(context: Context): com.keeftalk.chat.security.crypto.MessageDecryptionManager {
        synchronized(this) {
            // Trigger check for user change
            provideCryptoManager(context)
            
            return decryptionManager ?: run {
                val db = provideDatabase(context)
                val crypto = provideCryptoManager(context)
                com.keeftalk.chat.security.crypto.MessageDecryptionManager(db.messageDao(), crypto)
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
                    com.keeftalk.chat.util.KeeftalkExecutors.BOUNDED_IO.asCoroutineDispatcher()
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
            try {
                com.keeftalk.chat.util.KeeftalkStore.shutdown()
                com.keeftalk.chat.util.AppDependencies.reset()
                decryptionManager?.shutdown()
                repository?.shutdown()
                authRepository?.shutdown()
                chatSettingsRepository?.shutdown()
                appCustomizationRepository?.shutdown()
                privacyRepository?.shutdown()
                securityRepository?.shutdown()
                calendarRepository?.shutdown()
                noteRepository?.shutdown()
                vaultRepository?.shutdown()
                emailRepository?.shutdown()
                callLogManager?.shutdown()
                backgroundSyncManager?.shutdown()
                telephonySyncManager?.stopSync()
            } catch (e: Exception) {
                Log.e("AppModule", "Error during component shutdown", e)
            }
        }

        // 2. Clear all databases
        synchronized(databaseLock) {
            database?.close()
            database = null
        }
        context.databaseList().forEach { dbName ->
            Log.d("AppModule", "Deleting database: $dbName")
            context.deleteDatabase(dbName)
        }

        // 3. Clear all shared preferences
        try {
            val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                sharedPrefsDir.listFiles()?.forEach { file ->
                    val name = file.name.removeSuffix(".xml")
                    Log.d("AppModule", "Clearing shared preferences: $name")
                    context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) { clear() }
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("AppModule", "Error clearing shared preferences", e)
        }

        // 4. Clear Datastore and Files
        try {
            // Delete entire files directory content
            context.filesDir.listFiles()?.forEach { 
                Log.d("AppModule", "Deleting file/dir: ${it.name}")
                it.deleteRecursively() 
            }
            
            // Delete datastore if it's in a sibling directory (standard location)
            val dataStoreDir = File(context.filesDir.parentFile, "datastore")
            if (dataStoreDir.exists()) dataStoreDir.deleteRecursively()
            
            // Delete external files if they exist
            context.getExternalFilesDir(null)?.listFiles()?.forEach { it.deleteRecursively() }
        } catch (e: Exception) {
            Log.e("AppModule", "Error clearing files", e)
        }

        // 5. Clear Cache
        try {
            context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        } catch (e: Exception) {
            Log.e("AppModule", "Error clearing cache", e)
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

    fun provideCountryService(context: Context): com.keeftalk.chat.domain.service.CountryService {
        return countryService ?: synchronized(this) {
            countryService ?: com.keeftalk.chat.data.service.CountryServiceImpl(
                context.applicationContext,
                provideUserPreferencesRepository(context)
            ).also { countryService = it }
        }
    }

    fun providePhoneNumberService(context: Context): com.keeftalk.chat.domain.service.PhoneNumberService {
        return phoneNumberService ?: synchronized(this) {
            phoneNumberService ?: com.keeftalk.chat.data.service.PhoneNumberServiceImpl(
                context.applicationContext
            ).also { phoneNumberService = it }
        }
    }

    fun provideLegalRepository(context: Context): com.keeftalk.chat.domain.repository.LegalRepository {
        return legalRepository ?: synchronized(this) {
            legalRepository ?: com.keeftalk.chat.data.repository.LegalRepositoryImpl(
                context.applicationContext
            ).also { legalRepository = it }
        }
    }

    fun providePrivacyRepository(context: Context): com.keeftalk.chat.domain.repository.PrivacyRepository {
        return privacyRepository ?: synchronized(this) {
            privacyRepository ?: com.keeftalk.chat.data.repository.PrivacyRepositoryImpl(
                context.applicationContext,
                provideUserPreferencesRepository(context)
            ).also { privacyRepository = it }
        }
    }

    @Volatile
    private var userPreferencesRepository: com.keeftalk.chat.data.prefs.UserPreferencesRepository? = null

    fun provideUserPreferencesRepository(context: Context): com.keeftalk.chat.data.prefs.UserPreferencesRepository {
        return userPreferencesRepository ?: synchronized(this) {
            userPreferencesRepository ?: com.keeftalk.chat.data.prefs.UserPreferencesRepository(context.applicationContext).also { userPreferencesRepository = it }
        }
    }

    fun provideSecurityRepository(context: Context): com.keeftalk.chat.domain.repository.SecurityRepository {
        return securityRepository ?: synchronized(this) {
            securityRepository ?: com.keeftalk.chat.data.repository.SecurityRepositoryImpl(
                context.applicationContext,
                provideUserPreferencesRepository(context)
            ).also { securityRepository = it }
        }
    }

    fun provideChatSettingsRepository(context: Context): com.keeftalk.chat.domain.repository.ChatSettingsRepository {
        return chatSettingsRepository ?: synchronized(this) {
            chatSettingsRepository ?: com.keeftalk.chat.data.repository.ChatSettingsRepositoryImpl(
                context.applicationContext,
                provideUserPreferencesRepository(context)
            ).also { chatSettingsRepository = it }
        }
    }

    fun provideAppCustomizationRepository(context: Context): com.keeftalk.chat.domain.repository.AppCustomizationRepository {
        return appCustomizationRepository ?: synchronized(this) {
            appCustomizationRepository ?: com.keeftalk.chat.data.repository.AppCustomizationRepositoryImpl(
                context.applicationContext,
                provideUserPreferencesRepository(context)
            ).also { appCustomizationRepository = it }
        }
    }

    fun provideMediaRepository(context: Context): com.keeftalk.chat.domain.repository.MediaRepository {
        return mediaRepository ?: synchronized(this) {
            mediaRepository ?: com.keeftalk.chat.data.repository.MediaRepositoryImpl(
                context.applicationContext
            ).also { mediaRepository = it }
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun provideMediaExportPipeline(context: Context): com.keeftalk.chat.data.service.MediaExportPipeline {
        return mediaExportPipeline ?: synchronized(this) {
            mediaExportPipeline ?: com.keeftalk.chat.data.service.MediaExportPipeline(
                context.applicationContext,
                provideChatRepository(context)
            ).also { mediaExportPipeline = it }
        }
    }

    fun provideNoteRepository(context: Context): com.keeftalk.chat.domain.repository.NoteRepository {
        return noteRepository ?: synchronized(this) {
            noteRepository ?: run {
                val db = provideDatabase(context)
                val prefs = provideUserPreferencesRepository(context)
                com.keeftalk.chat.data.repository.NoteRepositoryImpl(
                    context.applicationContext,
                    db,
                    db.syncQueueDao(),
                    db.fileDao(),
                    provideFileUploadManager(context),
                    prefs
                )
            }.also { noteRepository = it }
        }
    }

    fun provideVaultRepository(context: Context): com.keeftalk.chat.domain.repository.VaultRepository {
        return vaultRepository ?: synchronized(this) {
            val db = provideDatabase(context)
            vaultRepository ?: com.keeftalk.chat.data.repository.VaultRepositoryImpl(
                context.applicationContext,
                db.vaultDao(),
                db.vaultSyncQueueDao(),
                db.fileDao(),
                provideFileUploadManager(context)
            ).also { vaultRepository = it }
        }
    }

    fun provideCalendarRepository(context: Context): com.keeftalk.chat.domain.repository.CalendarRepository {
        return calendarRepository ?: synchronized(this) {
            val db = provideDatabase(context)
            calendarRepository ?: com.keeftalk.chat.data.repository.CalendarRepositoryImpl(
                context.applicationContext,
                db.calendarDao(),
                db.calendarSyncQueueDao(),
                db.fileDao(),
                provideFileUploadManager(context)
            ).also { calendarRepository = it }
        }
    }

    fun provideCallLogManager(context: Context): com.keeftalk.chat.data.local.CallLogManager {
        return callLogManager ?: synchronized(this) {
            val db = provideDatabase(context)
            callLogManager ?: com.keeftalk.chat.data.local.CallLogManager(
                context.applicationContext,
                provideChatRepository(context),
                db.callLogDao()
            ).also { callLogManager = it }
        }
    }

    fun providePhoneContactManager(context: Context): com.keeftalk.chat.data.local.PhoneContactManager {
        return phoneContactManager ?: synchronized(this) {
            phoneContactManager ?: com.keeftalk.chat.data.local.PhoneContactManager(
                context.applicationContext
            ).also { phoneContactManager = it }
        }
    }

    fun provideSmsRepository(context: Context): com.keeftalk.chat.domain.repository.SmsRepository {
        return smsRepository ?: synchronized(this) {
            smsRepository ?: com.keeftalk.chat.data.repository.SmsRepositoryImpl(
                context.applicationContext,
                provideDatabase(context).smsDao()
            ).also { smsRepository = it }
        }
    }

    fun provideWalletRepository(): com.keeftalk.chat.feature.wallet.repository.WalletRepository {
        return walletRepository ?: synchronized(this) {
            walletRepository ?: com.keeftalk.chat.feature.wallet.repository.WalletRepositoryImpl().also { walletRepository = it }
        }
    }

    fun provideEmailRepository(context: Context): com.keeftalk.chat.feature.email.repository.EmailRepository {
        return emailRepository ?: synchronized(this) {
            emailRepository ?: run {
                val db = provideDatabase(context)
                val mailDao = db.mailDao()
                val secureStore = com.keeftalk.chat.feature.email.data.local.EmailSecureStore(context.applicationContext)
                val gmailSyncer = com.keeftalk.chat.feature.email.data.sync.providers.GmailSyncer(context.applicationContext, mailDao, secureStore)
                val outlookSyncer = com.keeftalk.chat.feature.email.data.sync.providers.OutlookSyncer(mailDao, secureStore)
                val imapSyncer = com.keeftalk.chat.feature.email.data.sync.providers.ImapSyncer(mailDao, secureStore)
                val syncManager = com.keeftalk.chat.feature.email.data.sync.MailSyncManager(
                    context.applicationContext,
                    mailDao,
                    gmailSyncer,
                    outlookSyncer,
                    imapSyncer
                )
                com.keeftalk.chat.feature.email.repository.EmailRepositoryImpl(context.applicationContext, mailDao, syncManager, secureStore)
            }.also { emailRepository = it }
        }
    }

    fun provideEmailAuthManager(context: Context): com.keeftalk.chat.feature.email.auth.EmailAuthManager {
        return emailAuthManager ?: synchronized(this) {
            emailAuthManager ?: com.keeftalk.chat.feature.email.auth.EmailAuthManager(context.applicationContext).also { emailAuthManager = it }
        }
    }

    fun provideTelephonySyncManager(context: Context): com.keeftalk.chat.data.sync.TelephonySyncManager {
        return telephonySyncManager ?: synchronized(this) {
            telephonySyncManager ?: com.keeftalk.chat.data.sync.TelephonySyncManager(
                context.applicationContext,
                provideDatabase(context).smsDao(),
                provideChatRepository(context)
            ).also { telephonySyncManager = it }
        }
    }

    fun provideBackgroundSyncManager(context: Context): com.keeftalk.chat.data.sync.BackgroundSyncManager {
        return backgroundSyncManager ?: synchronized(this) {
            backgroundSyncManager ?: com.keeftalk.chat.data.sync.BackgroundSyncManager(
                context.applicationContext,
                provideChatRepository(context),
                provideCalendarRepository(context)
            ).also { backgroundSyncManager = it }
        }
    }

    fun provideFileRepository(context: Context): com.keeftalk.chat.domain.repository.FileRepository {
        return fileRepository ?: synchronized(this) {
            fileRepository ?: run {
                val db = provideDatabase(context)
                com.keeftalk.chat.data.repository.FileRepositoryImpl(
                    context.applicationContext,
                    db.fileDao()
                )
            }.also { fileRepository = it }
        }
    }

    fun provideFileReferenceManager(context: Context): com.keeftalk.chat.util.FileReferenceManager {
        return fileReferenceManager ?: synchronized(this) {
            fileReferenceManager ?: com.keeftalk.chat.util.FileReferenceManager(
                provideFileRepository(context),
                { provideSupabaseClientAsync(context) }
            ).also { fileReferenceManager = it }
        }
    }

    fun provideFileUploadManager(context: Context): com.keeftalk.chat.util.FileUploadManager {
        return fileUploadManager ?: synchronized(this) {
            fileUploadManager ?: com.keeftalk.chat.util.FileUploadManager(
                context.applicationContext,
                provideFileRepository(context),
                provideCryptoManager(context),
                { provideSupabaseClientAsync(context) }
            ).also { fileUploadManager = it }
        }
    }

    fun provideFileDownloadManager(context: Context): com.keeftalk.chat.util.FileDownloadManager {
        return fileDownloadManager ?: synchronized(this) {
            fileDownloadManager ?: com.keeftalk.chat.util.FileDownloadManager(
                context.applicationContext,
                provideCryptoManager(context),
                { provideSupabaseClientAsync(context) }
            ).also { fileDownloadManager = it }
        }
    }
}
