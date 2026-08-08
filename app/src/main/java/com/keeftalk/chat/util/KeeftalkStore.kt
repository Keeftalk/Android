package com.keeftalk.chat.util

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * High-speed SQLite-backed Key-Value store with memory cache.
 * Follows the Read-Through/Write-Through pattern for instant access.
 */
class KeeftalkStore private constructor(context: Context) {
    private val dbHelper = DatabaseHelper(context)
    private val db: SQLiteDatabase by lazy { dbHelper.writableDatabase }
    private val executor = KeeftalkExecutors.SERIAL
    
    @Volatile
    private var cache: ConcurrentHashMap<String, String>? = null

    @Volatile
    private var isShutdown = false

    companion object {
        private const val TABLE_NAME = "key_value_store"
        private const val COLUMN_KEY = "kv_key"
        private const val COLUMN_VALUE = "kv_value"
        
        @Volatile
        private var instance: KeeftalkStore? = null
        private var appContext: Context? = null

        fun init(context: Context): KeeftalkStore {
            appContext = context.applicationContext
            return getInstance()
        }

        fun getInstance(): KeeftalkStore {
            return instance ?: synchronized(this) {
                instance ?: appContext?.let { KeeftalkStore(it).also { instance = it } }
                    ?: throw IllegalStateException("KeeftalkStore not initialized. Call init(context) first.")
            }
        }

        fun shutdown() {
            synchronized(this) {
                instance?.let {
                    try {
                        it.isShutdown = true
                        it.blockUntilAllWritesFinished()
                        it.dbHelper.close()
                        it.cache = null
                    } catch (e: Exception) {
                        android.util.Log.e("KeeftalkStore", "Error during shutdown", e)
                    }
                }
                instance = null
            }
        }
    }

    private fun ensureCacheLoaded() {
        if (isShutdown) return
        if (cache == null) {
            synchronized(this) {
                if (isShutdown) return
                if (cache == null) {
                    PerformanceProfiler.startStage("KeeftalkStore Cache Load")
                    val newCache = ConcurrentHashMap<String, String>()
                    try {
                        val cursor = db.query(TABLE_NAME, arrayOf(COLUMN_KEY, COLUMN_VALUE), null, null, null, null, null)
                        cursor.use {
                            while (it.moveToNext()) {
                                val key = it.getString(0)
                                val value = it.getString(1)
                                newCache[key] = value
                            }
                        }
                        cache = newCache
                    } catch (e: Exception) {
                        android.util.Log.e("KeeftalkStore", "Failed to load cache", e)
                    }
                    PerformanceProfiler.endStage("KeeftalkStore Cache Load", category = PerformanceProfiler.Category.STORAGE)
                    PerformanceProfiler.logEvent("KeeftalkStore: Items Loaded", info = "${newCache.size}", category = PerformanceProfiler.Category.STORAGE)
                }
            }
        }
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        if (isShutdown) return defaultValue
        ensureCacheLoaded()
        return cache?.get(key) ?: defaultValue
    }

    fun putString(key: String, value: String?) {
        if (isShutdown) return
        ensureCacheLoaded()
        if (value == null) {
            cache?.remove(key)
        } else {
            cache?.put(key, value)
        }

        executor.execute {
            if (isShutdown) return@execute
            try {
                val values = ContentValues().apply {
                    put(COLUMN_KEY, key)
                    put(COLUMN_VALUE, value)
                }
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            } catch (e: Exception) {
                android.util.Log.e("KeeftalkStore", "Failed to write key: $key", e)
            }
        }
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val value = getString(key) ?: return defaultValue
        return value == "1"
    }

    fun putBoolean(key: String, value: Boolean) {
        putString(key, if (value) "1" else "0")
    }

    fun getFloat(key: String, defaultValue: Float): Float {
        return getString(key)?.toFloatOrNull() ?: defaultValue
    }

    fun putFloat(key: String, value: Float) {
        putString(key, value.toString())
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return getString(key)?.toIntOrNull() ?: defaultValue
    }

    fun putInt(key: String, value: Int) {
        putString(key, value.toString())
    }

    /**
     * Vital for crash handling: Blocks the main thread until all
     * background writes are flushed to SQLite.
     */
    fun blockUntilAllWritesFinished() {
        val latch = CountDownLatch(1)
        executor.execute { latch.countDown() }
        latch.await(5, TimeUnit.SECONDS)
    }

    private class DatabaseHelper(context: Context) : 
        SQLiteOpenHelper(context, "keeftalk_kv.db", null, 1) {
        
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE $TABLE_NAME ($COLUMN_KEY TEXT PRIMARY KEY, $COLUMN_VALUE TEXT)")
            db.execSQL("CREATE INDEX idx_kv_key ON $TABLE_NAME ($COLUMN_KEY)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // No-op for now
        }
    }
}
