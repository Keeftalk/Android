package tn.keeftalk.chat.extension.registry

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

data class OfflineDictionaryEntry(
    val id: String,
    val title: String,
    val sourceLang: String = "en",
    val targetLang: String = "en",
    val dbFilePath: String
)

data class DictionaryLookupResult(
    val word: String,
    val definition: String,
    val dictionaryTitle: String
)

class DictionaryRegistry {
    private val TAG = "DictionaryRegistry"
    private val json = Json { ignoreUnknownKeys = true }
    private val dictionariesMap = mutableMapOf<String, OfflineDictionaryEntry>()
    private val jsonMemoryCache = mutableMapOf<String, Map<String, String>>()
    private val _installedDictionaries = MutableStateFlow<List<OfflineDictionaryEntry>>(emptyList())
    val installedDictionaries: StateFlow<List<OfflineDictionaryEntry>> = _installedDictionaries.asStateFlow()

    fun registerDictionary(dict: OfflineDictionaryEntry) {
        dictionariesMap[dict.id] = dict
        _installedDictionaries.value = dictionariesMap.values.toList()
        preloadJsonIfNeeded(dict)
    }

    fun unregisterDictionary(dictId: String) {
        dictionariesMap.remove(dictId)
        jsonMemoryCache.remove(dictId)
        _installedDictionaries.value = dictionariesMap.values.toList()
    }

    fun lookupWord(word: String): DictionaryLookupResult? {
        val rawClean = word.trim().lowercase().trim { !it.isLetterOrDigit() }
        if (rawClean.isBlank()) return null

        val candidates = mutableSetOf(rawClean)
        if (rawClean.endsWith("s") && rawClean.length > 3) {
            candidates.add(rawClean.dropLast(1)) // "books" -> "book"
        }
        if (rawClean.endsWith("es") && rawClean.length > 4) {
            candidates.add(rawClean.dropLast(2)) // "boxes" -> "box"
        }
        if (rawClean.endsWith("ed") && rawClean.length > 4) {
            candidates.add(rawClean.dropLast(2)) // "walked" -> "walk"
            candidates.add(rawClean.dropLast(1)) // "liked" -> "like"
        }
        if (rawClean.endsWith("ing") && rawClean.length > 5) {
            candidates.add(rawClean.dropLast(3)) // "reading" -> "read"
            candidates.add(rawClean.dropLast(3) + "e") // "making" -> "make"
        }

        for (candidate in candidates) {
            val result = queryDictionariesForSingleWord(candidate)
            if (result != null) return result
        }
        return null
    }

    private fun queryDictionariesForSingleWord(cleanWord: String): DictionaryLookupResult? {
        for (entry in dictionariesMap.values) {
            val dbFile = File(entry.dbFilePath)
            if (!dbFile.exists()) continue

            // 1. Try SQLite Database Query if .db or .sqlite file
            if (entry.dbFilePath.endsWith(".db", ignoreCase = true) || entry.dbFilePath.endsWith(".sqlite", ignoreCase = true)) {
                try {
                    val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                    db.use { database ->
                        val cursor = database.rawQuery("SELECT definition FROM dictionary WHERE LOWER(word) = ? LIMIT 1", arrayOf(cleanWord))
                        cursor.use { c ->
                            if (c.moveToFirst()) {
                                val def = c.getString(0)
                                if (!def.isNullOrBlank()) {
                                    return DictionaryLookupResult(cleanWord, def, entry.title)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error querying SQLite dictionary '${entry.title}': ${e.message}")
                }
            }

            // 2. Try Memory Cached JSON Dictionary Query
            val cachedMap = jsonMemoryCache[entry.id]
            if (cachedMap != null) {
                val def = cachedMap[cleanWord] ?: cachedMap[cleanWord.replaceFirstChar { it.uppercase() }]
                if (!def.isNullOrBlank()) {
                    return DictionaryLookupResult(cleanWord, def, entry.title)
                }
            }

            // 3. Try On-Demand Stream Lookup for Large JSON Dictionary
            if (entry.dbFilePath.endsWith(".json", ignoreCase = true)) {
                val streamDef = lookupWordInLargeJsonFile(dbFile, cleanWord)
                if (!streamDef.isNullOrBlank()) {
                    return DictionaryLookupResult(cleanWord, streamDef, entry.title)
                }
            }
        }
        return null
    }

    private fun preloadJsonIfNeeded(entry: OfflineDictionaryEntry) {
        if (entry.dbFilePath.endsWith(".json", ignoreCase = true)) {
            try {
                val file = File(entry.dbFilePath)
                if (file.exists()) {
                    if (file.length() > 2L * 1024L * 1024L) {
                        Log.i(TAG, "Dictionary JSON file '${entry.title}' is large (${file.length() / 1024 / 1024}MB). Using on-demand streaming lookup to avoid memory overhead.")
                        return
                    }
                    val content = file.readText()
                    val jsonObj = json.parseToJsonElement(content).jsonObject
                    val map = mutableMapOf<String, String>()
                    jsonObj.entries.forEach { (k, v) ->
                        val defString = formatJsonValueToDefinition(v)
                        map[k.lowercase()] = defString
                    }
                    jsonMemoryCache[entry.id] = map
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error preloading JSON dictionary '${entry.title}': ${e.message}")
            }
        }
    }

    private fun lookupWordInLargeJsonFile(file: File, word: String): String? {
        try {
            val cleanLower = word.trim().lowercase()
            val cleanUpper = word.trim().uppercase()
            val cleanCap = word.trim().lowercase().replaceFirstChar { it.uppercase() }

            val searchKeys = listOf(
                "\"$cleanUpper\"",
                "\"$cleanLower\"",
                "\"$cleanCap\""
            )

            file.bufferedReader(Charsets.UTF_8).use { reader ->
                val buffer = CharArray(128 * 1024)
                var sb = StringBuilder()
                var readChars: Int

                while (reader.read(buffer).also { readChars = it } != -1) {
                    sb.appendRange(buffer, 0, readChars)

                    for (key in searchKeys) {
                        var keyIdx = sb.indexOf(key)
                        while (keyIdx != -1) {
                            val startObj = sb.indexOf("{", keyIdx + key.length)
                            if (startObj != -1 && startObj - (keyIdx + key.length) < 20) {
                                var braceCount = 0
                                var endObj = -1
                                for (i in startObj until sb.length) {
                                    if (sb[i] == '{') braceCount++
                                    else if (sb[i] == '}') {
                                        braceCount--
                                        if (braceCount == 0) {
                                            endObj = i + 1
                                            break
                                        }
                                    }
                                }

                                if (endObj == -1) {
                                    val extraBuffer = CharArray(128 * 1024)
                                    val extraRead = reader.read(extraBuffer)
                                    if (extraRead != -1) {
                                        sb.appendRange(extraBuffer, 0, extraRead)
                                        for (i in startObj until sb.length) {
                                            if (sb[i] == '{') braceCount++
                                            else if (sb[i] == '}') {
                                                braceCount--
                                                if (braceCount == 0) {
                                                    endObj = i + 1
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }

                                if (endObj != -1 && endObj <= sb.length) {
                                    val objJsonStr = sb.substring(startObj, endObj)
                                    return parseSingleWordJsonDefinition(objJsonStr)
                                }
                            }
                            keyIdx = sb.indexOf(key, keyIdx + key.length)
                        }
                    }

                    if (sb.length > 20_000) {
                        sb = StringBuilder(sb.substring(sb.length - 10_000))
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error performing on-demand stream lookup for word '$word': ${e.message}")
        }
        return null
    }

    private fun parseSingleWordJsonDefinition(jsonObjStr: String): String {
        return try {
            val element = json.parseToJsonElement(jsonObjStr)
            formatJsonValueToDefinition(element)
        } catch (_: Exception) {
            jsonObjStr
        }
    }

    private fun formatJsonValueToDefinition(v: kotlinx.serialization.json.JsonElement): String {
        return try {
            if (v is kotlinx.serialization.json.JsonObject) {
                val sb = StringBuilder()
                val meanings = v["MEANINGS"]?.jsonArray
                if (meanings != null && !meanings.isEmpty()) {
                    meanings.forEach { mElem ->
                        val mArray = mElem.jsonArray
                        if (mArray.size >= 2) {
                            val pos = mArray[0].jsonPrimitive.content
                            val def = mArray[1].jsonPrimitive.content
                            sb.append("• [$pos] $def\n")
                        }
                    }
                }
                val synonyms = v["SYNONYMS"]?.jsonArray
                if (synonyms != null && !synonyms.isEmpty()) {
                    val synList = synonyms.map { it.jsonPrimitive.content }
                    sb.append("\nSynonyms: ${synList.joinToString(", ")}\n")
                }
                val antonyms = v["ANTONYMS"]?.jsonArray
                if (antonyms != null && !antonyms.isEmpty()) {
                    val antList = antonyms.map { it.jsonPrimitive.content }
                    sb.append("Antonyms: ${antList.joinToString(", ")}\n")
                }
                sb.toString().trim().ifBlank { v.toString() }
            } else {
                v.jsonPrimitive.content
            }
        } catch (_: Exception) {
            v.toString()
        }
    }
}
