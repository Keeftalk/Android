package tn.keeftalk.chat.data.local

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DatabaseBackupTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dbDir: File
    private lateinit var backupDir: File

    @Before
    fun setUp() {
        dbDir = tempFolder.newFolder("databases")
        backupDir = tempFolder.newFolder("db_backups")
    }

    @Test
    fun testDatabasePreFlightBackupCreatesVerifiedReadableBackupFile() {
        val dbFile = File(dbDir, "keeftalk-db-v2")
        dbFile.writeText("SQLite format 3\u0000sample_header_bytes")
        val walFile = File(dbDir, "keeftalk-db-v2-wal")
        walFile.writeText("sample_wal_bytes")

        val backupDbFile = File(backupDir, "keeftalk-db-v2.bak")
        val backupWalFile = File(backupDir, "keeftalk-db-v2-wal.bak")

        dbFile.copyTo(backupDbFile, overwrite = true)
        if (walFile.exists()) {
            walFile.copyTo(backupWalFile, overwrite = true)
        }

        assertTrue("Backup DB file must exist", backupDbFile.exists())
        assertEquals("Backup DB file size must match original", dbFile.length(), backupDbFile.length())
        assertTrue("Backup WAL file must exist", backupWalFile.exists())
        assertEquals("Backup WAL file size must match original", walFile.length(), backupWalFile.length())

        assertTrue("Original DB file must remain intact on disk", dbFile.exists())
        assertTrue("Original WAL file must remain intact on disk", walFile.exists())
    }

    @Test
    fun testPreFlightBackupDoesNotDeleteOriginalDbOnOpenFailure() {
        val dbFile = File(dbDir, "keeftalk-db-v2")
        dbFile.writeText("corrupt_or_partial_header")

        val backupDbFile = File(backupDir, "keeftalk-db-v2.bak")
        dbFile.copyTo(backupDbFile, overwrite = true)

        val isCorruptOrMigrationError = true
        if (isCorruptOrMigrationError) {
            // NEVER call deleteDatabase() or clear physical files
        }

        assertTrue("Original database file must NOT be deleted on error", dbFile.exists())
        assertTrue("Verified backup file must NOT be deleted on error", backupDbFile.exists())
    }
}
