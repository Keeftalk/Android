package tn.keeftalk.chat.data.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import tn.keeftalk.chat.data.remote.SupabaseXpSyncApi
import tn.keeftalk.chat.data.remote.XpEventDto
import tn.keeftalk.chat.di.AppModule

class XpSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val db = AppModule.provideDatabase(applicationContext)
            val xpDao = db.xpDao()
            val unsynced = xpDao.getUnsyncedXpEvents()
            val api = SupabaseXpSyncApi()

            if (unsynced.isNotEmpty()) {
                val dtos = unsynced.map {
                    XpEventDto(
                        eventId = it.eventId,
                        contentHash = it.contentHash,
                        pageKeyOrRange = it.pageKeyOrRange,
                        dwellMs = it.dwellMs,
                        anchoredAtMs = it.anchoredAtMs,
                        anchored = it.anchored == 1,
                        credit = it.credit,
                        flags = it.flags
                    )
                }
                val res = api.upload(dtos)
                if (res.isSuccess) {
                    val syncedIds = res.getOrNull()?.syncedEventIds ?: dtos.map { it.eventId }
                    xpDao.markXpEventsSynced(syncedIds)
                }
            }

            // Sync user total XP to backend for global ranking
            val readingXpRepo = AppModule.provideReadingXpRepository(applicationContext)
            val readingDays = readingXpRepo.getAllReadingDays()
            val unlockedAchs = readingXpRepo.getUnlockedAchievements()
            val meta = readingXpRepo.getXpMeta()
            val legacyXp = meta?.legacyReadingXp ?: 0L

            val daysMap = readingDays.associate {
                try {
                    java.time.LocalDate.parse(it.date) to tn.keeftalk.chat.domain.xp.ReadingXp.DayRecord(it.creditedPages, it.flagged == 1)
                } catch (_: Exception) {
                    java.time.LocalDate.now() to tn.keeftalk.chat.domain.xp.ReadingXp.DayRecord(it.creditedPages, it.flagged == 1)
                }
            }
            val settled = tn.keeftalk.chat.domain.xp.ReadingXp.settle(daysMap)
            val books = AppModule.provideBookRepository(applicationContext).getAllBooks()
            val achievements = tn.keeftalk.chat.ui.books.components.XpCalculator.calculateAchievementsList(
                books = books,
                highlightsCount = 0,
                bookmarksCount = 0,
                readingDays = readingDays,
                unlockedIds = unlockedAchs.map { it.id }.toSet()
            )
            val totalXp = tn.keeftalk.chat.ui.books.components.XpCalculator.calculateTotalXp(legacyXp, settled, achievements)

            readingXpRepo.syncAndFetchGlobalRank(applicationContext, totalXp)

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
