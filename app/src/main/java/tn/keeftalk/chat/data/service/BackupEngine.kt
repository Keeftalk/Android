package tn.keeftalk.chat.data.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import tn.keeftalk.chat.data.local.KeeftalkDatabase
import tn.keeftalk.chat.data.local.entities.*
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.security.crypto.BackupCryptoEngine
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupEngine(
    private val context: Context,
    private val database: KeeftalkDatabase,
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val backupDir: File
        get() = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }

    val currentBackupFile: File
        get() = File(backupDir, "current_backup.ktb")

    /**
     * Public ordinary storage directory: Storage/Keeftalk/backups/
     */
    val publicBackupDirectory: File
        get() {
            val rootStorage = Environment.getExternalStorageDirectory()
            return File(rootStorage, "Keeftalk/backups").apply {
                if (!exists()) {
                    mkdirs()
                }
            }
        }

    suspend fun performBackup(
        password: String,
        selectedCategories: Set<String>,
        accountId: String,
    ): Result<File> = withContext(Dispatchers.IO) {
        val stagingDir = File(context.cacheDir, "backup_staging_${System.currentTimeMillis()}")
        try {
            stagingDir.mkdirs()
            val recordCounts = mutableMapOf<String, Int>()

            // 1. Export Data Categories & Sub-entities comprehensively using Moshi
            if (selectedCategories.contains(BackupCategory.BOOKS.name)) {
                val books = database.bookDao().getAllBooks()
                val highlights = database.bookDao().getAllHighlightsFlow().first()
                val bookmarks = database.bookDao().getAllBookmarksFlow().first()
                val sessions = database.bookDao().getAllSessions()

                writeJson("books.json", stagingDir, books, BookEntity::class.java)
                writeJson("book_highlights.json", stagingDir, highlights, BookHighlightEntity::class.java)
                writeJson("book_bookmarks.json", stagingDir, bookmarks, BookBookmarkEntity::class.java)
                writeJson("book_sessions.json", stagingDir, sessions, BookReadingSessionEntity::class.java)
                recordCounts["BOOKS"] = books.size + highlights.size + bookmarks.size + sessions.size
            }
            if (selectedCategories.contains(BackupCategory.HABITS.name)) {
                val habits = database.nativeHabitDao().getAllHabits().first()
                val checkIns = database.nativeHabitDao().getAllCheckIns().first()
                val schedules = database.nativeHabitDao().getAllSchedules().first()

                writeJson("habits.json", stagingDir, habits, HabitEntity::class.java)
                writeJson("habit_checkins.json", stagingDir, checkIns, CheckInEntity::class.java)
                writeJson("habit_schedules.json", stagingDir, schedules, ScheduleVersionEntity::class.java)
                recordCounts["HABITS"] = habits.size + checkIns.size + schedules.size
            }
            if (selectedCategories.contains(BackupCategory.TODO.name)) {
                val tasks = database.todoDao().getAllTasks().first()
                val lists = database.todoDao().getAllLists().first()

                writeJson("todo_tasks.json", stagingDir, tasks, TodoTaskEntity::class.java)
                writeJson("todo_lists.json", stagingDir, lists, TodoListEntity::class.java)
                recordCounts["TODO"] = tasks.size + lists.size
            }
            if (selectedCategories.contains(BackupCategory.BROWSER.name)) {
                val history = database.browserHistoryDao().getBrowserHistory().first()
                writeJson("browser_history.json", stagingDir, history, BrowserHistoryEntity::class.java)
                recordCounts["BROWSER"] = history.size
            }
            if (selectedCategories.contains(BackupCategory.FEED.name)) {
                val articles = database.feedDao().getAllArticlesRaw().first()
                val sources = database.feedDao().getAllSources().first()
                writeJson("feed_articles.json", stagingDir, articles, FeedArticleEntity::class.java)
                writeJson("feed_sources.json", stagingDir, sources, FeedSourceEntity::class.java)
                recordCounts["FEED"] = articles.size + sources.size
            }
            if (selectedCategories.contains(BackupCategory.FINANCES.name)) {
                val transactions = database.financesDao().getTransactions().first()
                val budgets = database.financesDao().getBudgets().first()
                val goals = database.financesDao().getGoals().first()
                val assets = database.financesDao().getAssets().first()
                val debts = database.financesDao().getDebts().first()

                writeJson("finance_transactions.json", stagingDir, transactions, FinanceTransactionEntity::class.java)
                writeJson("finance_budgets.json", stagingDir, budgets, FinanceBudgetEntity::class.java)
                writeJson("finance_goals.json", stagingDir, goals, FinanceGoalEntity::class.java)
                writeJson("finance_assets.json", stagingDir, assets, FinanceAssetEntity::class.java)
                writeJson("finance_debts.json", stagingDir, debts, FinanceDebtEntity::class.java)
                recordCounts["FINANCES"] = transactions.size + budgets.size + goals.size + assets.size + debts.size
            }
            if (selectedCategories.contains(BackupCategory.CALENDAR.name)) {
                val calendarItems = database.calendarDao().getAllItems().first().map { it.item }
                val categories = database.calendarDao().getAllCategories().first()
                writeJson("calendar_items.json", stagingDir, calendarItems, CalendarItemEntity::class.java)
                writeJson("calendar_categories.json", stagingDir, categories, CalendarCategoryEntity::class.java)
                recordCounts["CALENDAR"] = calendarItems.size + categories.size
            }
            if (selectedCategories.contains(BackupCategory.NOTES.name)) {
                val notes = database.noteDao().getAllNotesOnce()
                writeJson("notes.json", stagingDir, notes, NoteEntity::class.java)
                recordCounts["NOTES"] = notes.size
            }
            if (selectedCategories.contains(BackupCategory.DOCS.name)) {
                val docs = database.docDao().getAllDocs()
                writeJson("docs.json", stagingDir, docs, DocEntity::class.java)
                recordCounts["DOCS"] = docs.size
            }
            if (selectedCategories.contains(BackupCategory.SECRETS.name)) {
                val vaultItems = database.vaultDao().getAllItems().first().map { it.item }
                val folders = database.vaultDao().getFolders(null).first()
                writeJson("vault_items.json", stagingDir, vaultItems, VaultItemEntity::class.java)
                writeJson("vault_folders.json", stagingDir, folders, VaultFolderEntity::class.java)
                recordCounts["SECRETS"] = vaultItems.size + folders.size
            }
            if (selectedCategories.contains(BackupCategory.CALLS.name)) {
                val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    val calls = database.callLogDao().getAllCallLogs().first()
                    writeJson("call_logs.json", stagingDir, calls, CallLogEntity::class.java)
                    recordCounts["CALLS"] = calls.size
                } else {
                    recordCounts["CALLS_PERMISSION_MISSING"] = 0
                }
            }
            if (selectedCategories.contains(BackupCategory.SMS.name)) {
                val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    val threads = database.smsDao().getThreads().first()
                    writeJson("sms_threads.json", stagingDir, threads, SmsThreadEntity::class.java)
                    recordCounts["SMS"] = threads.size
                } else {
                    recordCounts["SMS_PERMISSION_MISSING"] = 0
                }
            }

            // 2. Create Manifest
            val manifest = BackupManifest(
                backupId = UUID.randomUUID().toString(),
                accountId = accountId,
                timestamp = System.currentTimeMillis(),
                appVersion = "0.9-Alpha",
                selectedCategories = selectedCategories.toList(),
                recordCounts = recordCounts,
                schemaVersion = database.openHelper.readableDatabase.version,
                checksum = "SHA256_PLACEHOLDER"
            )
            val manifestAdapter = moshi.adapter(BackupManifest::class.java)
            File(stagingDir, "manifest.json").writeText(manifestAdapter.toJson(manifest) ?: "{}")

            // 3. Zip Staging Directory
            val zipFile = File(context.cacheDir, "staging_${System.currentTimeMillis()}.zip")
            zipFolder(stagingDir, zipFile)

            // 4. Encrypt to .ktb File
            val tempOutput = File(backupDir, "temp_backup.ktb")
            BackupCryptoEngine.encryptFile(zipFile, tempOutput, password)
            zipFile.delete()

            // 5. Atomic Replace in Internal Storage
            if (currentBackupFile.exists()) {
                currentBackupFile.delete()
            }
            tempOutput.renameTo(currentBackupFile)

            // 6. Save a copy to Public Ordinary Storage: Storage/Keeftalk/backups/
            val publicDir = publicBackupDirectory
            val publicFile = File(publicDir, "current_backup.ktb")
            if (publicFile.exists()) publicFile.delete()
            currentBackupFile.copyTo(publicFile, overwrite = true)

            val timeStampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val timestampedFile = File(publicDir, "Keeftalk_Backup_$timeStampStr.ktb")
            currentBackupFile.copyTo(timestampedFile, overwrite = true)

            Result.success(currentBackupFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            stagingDir.deleteRecursively()
        }
    }

    private inline fun <reified T> writeJson(fileName: String, stagingDir: File, list: List<T>, clazz: Class<T>) {
        val adapter = moshi.adapter<List<T>>(Types.newParameterizedType(List::class.java, clazz))
        File(stagingDir, fileName).writeText(adapter.toJson(list) ?: "[]")
    }

    suspend fun restoreBackup(
        backupFile: File,
        password: String,
        expectedAccountId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val extractDir = File(context.cacheDir, "restore_extract_${System.currentTimeMillis()}")
        val decryptedZip = File(context.cacheDir, "decrypted_${System.currentTimeMillis()}.zip")
        try {
            extractDir.mkdirs()
            // 1. Decrypt
            BackupCryptoEngine.decryptFile(backupFile, decryptedZip, password)

            // 2. Unzip
            unzip(decryptedZip, extractDir)
            decryptedZip.delete()

            // 3. Read and Verify Manifest
            val manifestFile = File(extractDir, "manifest.json")
            if (!manifestFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Invalid backup file: Manifest missing."))
            }
            val manifestAdapter = moshi.adapter(BackupManifest::class.java)
            val manifest = manifestAdapter.fromJson(manifestFile.readText())
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid manifest format."))

            if (manifest.accountId != expectedAccountId) {
                return@withContext Result.failure(SecurityException("Account mismatch: This backup belongs to a different account and cannot be restored here."))
            }

            // 4. Transactional Database Restore using Room withTransaction
            database.withTransaction {
                // Restore books
                val booksFile = File(extractDir, "books.json")
                if (booksFile.exists()) {
                    val adapter = moshi.adapter<List<BookEntity>>(Types.newParameterizedType(List::class.java, BookEntity::class.java))
                    adapter.fromJson(booksFile.readText())?.let { database.bookDao().insertOrUpdateBooks(it) }
                }
                // Restore habits
                val habitsFile = File(extractDir, "habits.json")
                if (habitsFile.exists()) {
                    val adapter = moshi.adapter<List<HabitEntity>>(Types.newParameterizedType(List::class.java, HabitEntity::class.java))
                    adapter.fromJson(habitsFile.readText())?.forEach { database.nativeHabitDao().upsertHabit(it) }
                }
                // Restore todos
                val todoTasksFile = File(extractDir, "todo_tasks.json")
                if (todoTasksFile.exists()) {
                    val adapter = moshi.adapter<List<TodoTaskEntity>>(Types.newParameterizedType(List::class.java, TodoTaskEntity::class.java))
                    adapter.fromJson(todoTasksFile.readText())?.forEach { database.todoDao().upsertTask(it) }
                }
                val todoListsFile = File(extractDir, "todo_lists.json")
                if (todoListsFile.exists()) {
                    val adapter = moshi.adapter<List<TodoListEntity>>(Types.newParameterizedType(List::class.java, TodoListEntity::class.java))
                    adapter.fromJson(todoListsFile.readText())?.forEach { database.todoDao().upsertList(it) }
                }
                // Restore browser history
                val browserFile = File(extractDir, "browser_history.json")
                if (browserFile.exists()) {
                    val adapter = moshi.adapter<List<BrowserHistoryEntity>>(Types.newParameterizedType(List::class.java, BrowserHistoryEntity::class.java))
                    adapter.fromJson(browserFile.readText())?.forEach { database.browserHistoryDao().insertHistoryItem(it) }
                }
                // Restore feed
                val feedFile = File(extractDir, "feed_articles.json")
                if (feedFile.exists()) {
                    val adapter = moshi.adapter<List<FeedArticleEntity>>(Types.newParameterizedType(List::class.java, FeedArticleEntity::class.java))
                    adapter.fromJson(feedFile.readText())?.let { database.feedDao().insertArticles(it) }
                }
                // Restore finances
                val financesFile = File(extractDir, "finance_transactions.json")
                if (financesFile.exists()) {
                    val adapter = moshi.adapter<List<FinanceTransactionEntity>>(Types.newParameterizedType(List::class.java, FinanceTransactionEntity::class.java))
                    adapter.fromJson(financesFile.readText())?.forEach { database.financesDao().insertTransaction(it) }
                }
                // Restore calendar
                val calendarFile = File(extractDir, "calendar_items.json")
                if (calendarFile.exists()) {
                    val adapter = moshi.adapter<List<CalendarItemEntity>>(Types.newParameterizedType(List::class.java, CalendarItemEntity::class.java))
                    adapter.fromJson(calendarFile.readText())?.forEach { database.calendarDao().insertItem(it) }
                }
                // Restore notes
                val notesFile = File(extractDir, "notes.json")
                if (notesFile.exists()) {
                    val adapter = moshi.adapter<List<NoteEntity>>(Types.newParameterizedType(List::class.java, NoteEntity::class.java))
                    adapter.fromJson(notesFile.readText())?.let { database.noteDao().insertNotes(it) }
                }
                // Restore docs
                val docsFile = File(extractDir, "docs.json")
                if (docsFile.exists()) {
                    val adapter = moshi.adapter<List<DocEntity>>(Types.newParameterizedType(List::class.java, DocEntity::class.java))
                    adapter.fromJson(docsFile.readText())?.let { database.docDao().insertOrUpdateDocs(it) }
                }
                // Restore secrets / vault
                val secretsFile = File(extractDir, "vault_items.json")
                if (secretsFile.exists()) {
                    val adapter = moshi.adapter<List<VaultItemEntity>>(Types.newParameterizedType(List::class.java, VaultItemEntity::class.java))
                    adapter.fromJson(secretsFile.readText())?.let { database.vaultDao().insertItems(it) }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            extractDir.deleteRecursively()
            decryptedZip.delete()
        }
    }

    private fun zipFolder(sourceFolder: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            sourceFolder.walkTopDown().forEach { file ->
                if (!file.isDirectory) {
                    val relativePath = file.toURI().toURL().toString().substring(sourceFolder.toURI().toURL().toString().length)
                    zos.putNextEntry(ZipEntry(relativePath))
                    FileInputStream(file).use { fis -> fis.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    private fun unzip(zipFile: File, targetDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
