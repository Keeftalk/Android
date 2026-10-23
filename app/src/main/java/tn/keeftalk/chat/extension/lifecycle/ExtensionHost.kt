package tn.keeftalk.chat.extension.lifecycle

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import tn.keeftalk.chat.extension.model.*
import tn.keeftalk.chat.extension.parser.ExtractedPackageSession
import tn.keeftalk.chat.extension.parser.KtxpPackageReader
import tn.keeftalk.chat.extension.persistence.ExtensionDatabase
import tn.keeftalk.chat.extension.persistence.ExtensionEntity
import tn.keeftalk.chat.extension.persistence.ExtensionStorageManager
import tn.keeftalk.chat.extension.registry.OfflineDictionaryEntry
import tn.keeftalk.chat.extension.security.PackageVerifier
import java.io.File

class ExtensionHost(private val context: Context) {
    private val TAG = "ExtensionHost"
    private val storage = InstallationStorage(context)
    private val database = ExtensionDatabase.getInstance(context)
    private val storageManager = ExtensionStorageManager(context)
    val registry = ExtensionRegistry()

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    init {
        try {
            val enabledEntities = database.extensionDao().getEnabledExtensionsSync()
            for (entity in enabledEntities) {
                val domain = entity.toDomain()
                val installDir = File(domain.installedPath)
                if (installDir.exists()) {
                    registerCapabilitiesForExtension(domain, installDir)
                }
            }
            Log.i(TAG, "Synchronously loaded ${enabledEntities.size} enabled extensions into registries on init.")
        } catch (e: Exception) {
            Log.e(TAG, "Error in synchronous extension load on init", e)
        }
    }

    suspend fun stageAndReviewPackage(packageUri: Uri): Triple<ExtractedPackageSession, ExtensionManifest?, SecurityReviewReport> = withContext(Dispatchers.IO) {
        val session = KtxpPackageReader.readAndStagePackage(context, packageUri)
        val (manifest, report) = PackageVerifier.verifyPackageSession(session, isNetworkAvailable = false)
        Triple(session, manifest, report)
    }

    suspend fun confirmAndInstallStagedPackage(
        session: ExtractedPackageSession,
        manifest: ExtensionManifest,
        report: SecurityReviewReport
    ): Result<InstalledExtension> = withContext(Dispatchers.IO) {
        if (!report.isEligibleForInstallation) {
            session.stagingDir.deleteRecursively()
            return@withContext Result.failure(IllegalStateException("Package review failed security or compatibility checks and is not eligible for installation."))
        }

        try {
            val installedDir = storage.installFromStaging(session.stagingDir, manifest.id, manifest.version)
            val domain = InstalledExtension(
                id = manifest.id,
                type = manifest.type,
                name = manifest.name,
                version = manifest.version,
                description = manifest.description,
                publisherName = manifest.publisher.name,
                isPublisherTrusted = report.isPublisherTrusted,
                installedPath = installedDir.absolutePath,
                installedTimeMillis = System.currentTimeMillis(),
                isEnabled = true,
                sizeBytes = session.totalExpandedBytes,
                entry = manifest.entry,
                manifestJson = session.manifestJson
            )

            try {
                database.extensionDao().insertOrUpdate(ExtensionEntity.fromDomain(domain))
                registerCapabilitiesForExtension(domain, installedDir)
                Log.i(TAG, "Extension '${manifest.id}' installed and registered successfully.")
                Result.success(domain)
            } catch (dbException: Exception) {
                Log.e(TAG, "Database insertion failed for '${manifest.id}'. Rolling back storage creation.", dbException)
                installedDir.deleteRecursively()
                throw dbException
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to confirm and install staged package: ${e.message}", e)
            session.stagingDir.deleteRecursively()
            Result.failure(e)
        }
    }

    suspend fun toggleExtensionEnabled(extensionId: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        database.extensionDao().setEnabled(extensionId, isEnabled)
        val entity = database.extensionDao().getExtensionById(extensionId) ?: return@withContext
        val domain = entity.toDomain()
        val installDir = File(domain.installedPath)

        if (isEnabled) {
            registerCapabilitiesForExtension(domain, installDir)
        } else {
            unregisterCapabilitiesForExtension(domain)
        }
    }

    suspend fun uninstallExtension(extensionId: String) = withContext(Dispatchers.IO) {
        val entity = database.extensionDao().getExtensionById(extensionId)
        if (entity != null) {
            unregisterCapabilitiesForExtension(entity.toDomain())
        }
        database.extensionDao().deleteById(extensionId)
        storage.uninstallExtension(extensionId)
        storageManager.clearPreferences(extensionId)
        Log.i(TAG, "Uninstalled extension '$extensionId'")
    }

    suspend fun loadAllInstalledExtensions() = withContext(Dispatchers.IO) {
        try {
            val enabledEntities = database.extensionDao().getEnabledExtensions()
            for (entity in enabledEntities) {
                val domain = entity.toDomain()
                val installDir = File(domain.installedPath)
                if (installDir.exists()) {
                    registerCapabilitiesForExtension(domain, installDir)
                } else {
                    Log.w(TAG, "Installed directory for extension '${domain.id}' is missing. Skipping registration.")
                }
            }
            Log.i(TAG, "Loaded ${enabledEntities.size} enabled extensions into registries.")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading installed extensions into registries", e)
        }
    }

    fun getAllInstalledExtensionsFlow(): Flow<List<InstalledExtension>> {
        return database.extensionDao().getAllExtensionsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun registerCapabilitiesForExtension(ext: InstalledExtension, installDir: File) {
        val entryFile = File(installDir, ext.entry)
        if (!entryFile.exists()) {
            Log.w(TAG, "Entry file '${ext.entry}' not found for extension '${ext.id}'")
            return
        }

        val parsedManifest = try {
            json.decodeFromString<ExtensionManifest>(ext.manifestJson)
        } catch (_: Exception) {
            null
        }

        try {
            val entryText = entryFile.readText()
            when (ext.type) {
                ExtensionType.THEME -> {
                    try {
                        val themePack = json.decodeFromString<ThemePack>(entryText)
                        registry.themeRegistry.registerTheme(themePack)
                        registry.themeRegistry.setActiveTheme(themePack.id)
                    } catch (_: Exception) {
                        try {
                            val themePacks = json.decodeFromString<List<ThemePack>>(entryText)
                            themePacks.forEach { pack ->
                                registry.themeRegistry.registerTheme(pack)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse theme pack for '${ext.id}': ${e.message}")
                        }
                    }
                }
                ExtensionType.EMOJI_PACK -> {
                    val emojiPack = json.decodeFromString<EmojiPack>(entryText)
                    val updatedItems = emojiPack.items.map { item ->
                        item.copy(relativePath = File(installDir, item.relativePath).absolutePath)
                    }
                    val updatedPack = emojiPack.copy(items = updatedItems)
                    registry.emojiPackRegistry.registerEmojiPack(updatedPack)
                }
                ExtensionType.ICON_PACK -> {
                    val iconPack = json.decodeFromString<IconPack>(entryText)
                    registry.iconPackRegistry.registerIconPack(iconPack)
                    registry.iconPackRegistry.setActiveIconPack(iconPack.id)
                }
                ExtensionType.SOUND_PACK -> {
                    val soundPack = json.decodeFromString<SoundPack>(entryText)
                    registry.soundPackRegistry.registerSoundPack(soundPack)
                    registry.soundPackRegistry.setActiveSoundPack(soundPack.id)
                }
                ExtensionType.DICTIONARY -> {
                    val srcLang = parsedManifest?.dictionaryLanguages?.getOrNull(0) ?: "en"
                    val tgtLang = parsedManifest?.dictionaryLanguages?.getOrNull(1) ?: "en"
                    val dictEntry = OfflineDictionaryEntry(
                        id = ext.id,
                        title = ext.name,
                        sourceLang = srcLang,
                        targetLang = tgtLang,
                        dbFilePath = entryFile.absolutePath
                    )
                    registry.dictionaryRegistry.registerDictionary(dictEntry)
                }
                ExtensionType.FEED_CARD -> {
                    val card = json.decodeFromString<DeclarativeCard>(entryText)
                    registry.feedCardRegistry.registerCard(card)
                }
                ExtensionType.FILE_HANDLER -> {
                    val supportedExts = if (parsedManifest != null && parsedManifest.supportedFileExtensions.isNotEmpty()) {
                        parsedManifest.supportedFileExtensions
                    } else {
                        listOf("kt", "java", "py", "js", "cpp", "json", "xml", "html", "css", "ts", "rs", "go", "c", "h", "md", "txt", "log")
                    }
                    val handler = tn.keeftalk.chat.extension.registry.CustomFileHandler(
                        extensionId = ext.id,
                        name = ext.name,
                        supportedExtensions = supportedExts
                    )
                    registry.fileHandlerRegistry.registerHandler(handler)
                }
                ExtensionType.FULL_SCREEN_APP, ExtensionType.SETTINGS_PAGE, ExtensionType.TOOL_WIDGET -> {
                    val screen = json.decodeFromString<DeclarativeScreen>(entryText).copy(extensionId = ext.id)
                    registry.declarativeScreenRegistry.registerScreen(screen)
                }
                else -> {
                    Log.i(TAG, "Capability registration for type '${ext.type}' acknowledged.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register capabilities for extension '${ext.id}': ${e.message}", e)
        }
    }

    private fun unregisterCapabilitiesForExtension(ext: InstalledExtension) {
        when (ext.type) {
            ExtensionType.THEME -> registry.themeRegistry.unregisterTheme(ext.id)
            ExtensionType.EMOJI_PACK -> registry.emojiPackRegistry.unregisterEmojiPack(ext.id)
            ExtensionType.ICON_PACK -> registry.iconPackRegistry.unregisterIconPack(ext.id)
            ExtensionType.SOUND_PACK -> registry.soundPackRegistry.unregisterSoundPack(ext.id)
            ExtensionType.DICTIONARY -> registry.dictionaryRegistry.unregisterDictionary(ext.id)
            ExtensionType.FEED_CARD -> registry.feedCardRegistry.unregisterCard(ext.id)
            ExtensionType.FILE_HANDLER -> registry.fileHandlerRegistry.unregisterHandler(ext.id)
            ExtensionType.FULL_SCREEN_APP, ExtensionType.SETTINGS_PAGE, ExtensionType.TOOL_WIDGET -> {
                registry.declarativeScreenRegistry.unregisterAllForExtension(ext.id)
            }
            else -> {}
        }
    }

    fun cleanupStaging() {
        try {
            val stagingDir = File(context.filesDir, "extensions/staging")
            if (stagingDir.exists()) {
                stagingDir.deleteRecursively()
                stagingDir.mkdirs()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up staging directory", e)
        }
    }
}
