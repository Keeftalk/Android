package com.keeftalk.chat.util.job

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class JobStorage(context: Context) {
    private val dbHelper = DatabaseHelper(context)
    private val db: SQLiteDatabase by lazy { dbHelper.writableDatabase }

    companion object {
        private const val TABLE_NAME = "jobs"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_DATA = "data"
        private const val COLUMN_NEXT_RUN = "next_run"
        private const val COLUMN_IS_RUNNING = "is_running"
    }

    fun insertJob(spec: JobSpec) {
        val values = ContentValues().apply {
            put(COLUMN_ID, spec.id)
            put(COLUMN_TYPE, spec.type)
            put(COLUMN_DATA, spec.data)
            put(COLUMN_NEXT_RUN, spec.nextRunAttempt)
            put(COLUMN_IS_RUNNING, if (spec.isRunning) 1 else 0)
        }
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getNextEligibleJob(now: Long): JobSpec? {
        val cursor = db.query(
            TABLE_NAME, null,
            "$COLUMN_NEXT_RUN <= ? AND $COLUMN_IS_RUNNING = 0", arrayOf(now.toString()),
            null, null, "$COLUMN_NEXT_RUN ASC", "1"
        )
        return cursor.use {
            if (it.moveToFirst()) {
                JobSpec(
                    id = it.getString(it.getColumnIndexOrThrow(COLUMN_ID)),
                    type = it.getString(it.getColumnIndexOrThrow(COLUMN_TYPE)),
                    data = it.getString(it.getColumnIndexOrThrow(COLUMN_DATA)),
                    nextRunAttempt = it.getLong(it.getColumnIndexOrThrow(COLUMN_NEXT_RUN)),
                    isRunning = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_RUNNING)) == 1
                )
            } else null
        }
    }

    fun markRunning(id: String) {
        val values = ContentValues().apply {
            put(COLUMN_IS_RUNNING, 1)
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id))
    }

    fun deleteJob(id: String) {
        db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id))
    }

    fun updateRetry(id: String, backoff: Long) {
        val nextRun = System.currentTimeMillis() + backoff
        val values = ContentValues().apply {
            put(COLUMN_NEXT_RUN, nextRun)
            put(COLUMN_IS_RUNNING, 0)
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id))
    }

    fun resetRunningJobs() {
        val values = ContentValues().apply {
            put(COLUMN_IS_RUNNING, 0)
        }
        db.update(TABLE_NAME, values, null, null)
    }

    private class DatabaseHelper(context: Context) : 
        SQLiteOpenHelper(context, "keeftalk_jobs.db", null, 1) {
        
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE $TABLE_NAME (
                    $COLUMN_ID TEXT PRIMARY KEY,
                    $COLUMN_TYPE TEXT,
                    $COLUMN_DATA TEXT,
                    $COLUMN_NEXT_RUN INTEGER,
                    $COLUMN_IS_RUNNING INTEGER
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX idx_job_next_run ON $TABLE_NAME ($COLUMN_NEXT_RUN)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }
}
