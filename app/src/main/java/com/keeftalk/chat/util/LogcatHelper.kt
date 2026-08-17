package com.keeftalk.chat.util

import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LogcatHelper {
    suspend fun getLogcat(): String = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("logcat -d -t 1000")
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            val log = StringBuilder()
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                log.append(line).append("\n")
            }
            log.toString()
        } catch (e: Exception) {
            "Error capturing logcat: ${e.message}"
        }
    }
}
