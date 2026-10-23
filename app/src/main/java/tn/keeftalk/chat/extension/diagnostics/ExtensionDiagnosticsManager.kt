package tn.keeftalk.chat.extension.diagnostics

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import tn.keeftalk.chat.extension.persistence.ExtensionStorageManager
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class DiagnosticLogEntry(
    val timestampMillis: Long = System.currentTimeMillis(),
    val extensionId: String,
    val severity: String = "INFO", // INFO, WARNING, ERROR, CRASH
    val message: String
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestampMillis))
    }
}

class ExtensionDiagnosticsManager(private val context: Context) {
    private val TAG = "ExtensionDiagnosticsManager"
    private val storageManager = ExtensionStorageManager(context)

    private val _logs = MutableStateFlow<List<DiagnosticLogEntry>>(emptyList())
    val logs: StateFlow<List<DiagnosticLogEntry>> = _logs.asStateFlow()

    private val crashCounts = mutableMapOf<String, Int>()

    @Synchronized
    fun logEvent(extensionId: String, severity: String, message: String) {
        val entry = DiagnosticLogEntry(
            timestampMillis = System.currentTimeMillis(),
            extensionId = extensionId,
            severity = severity,
            message = message
        )
        val current = _logs.value.toMutableList()
        current.add(0, entry) // Newest first
        if (current.size > 200) {
            current.removeAt(current.size - 1)
        }
        _logs.value = current

        if (severity == "CRASH" || severity == "ERROR") {
            val count = crashCounts.getOrPut(extensionId) { 0 } + 1
            crashCounts[extensionId] = count
            Log.e(TAG, "Extension [$extensionId] crash/error count: $count — $message")
        }
    }

    fun getCrashCount(extensionId: String): Int {
        return crashCounts[extensionId] ?: 0
    }

    fun getExtensionStorageUsageBytes(extensionId: String): Long {
        return storageManager.getStorageSizeBytes(extensionId)
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
