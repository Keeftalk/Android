package tn.keeftalk.chat.feature.secrets.util

import tn.keeftalk.chat.feature.secrets.crypto.PasswordStrengthEvaluator
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretCredentialEntity
import java.util.Locale
import java.util.UUID

data class ImportedItem(
    val serviceName: String,
    val username: String,
    val website: String,
    val password: String,
    val category: String,
    val notes: String
)

data class CsvImportResult(
    val detectedSource: String,
    val totalRows: Int,
    val importedItems: List<ImportedItem>,
    val duplicateCount: Int,
    val skippedCount: Int,
    val errors: List<String>
)

object CsvImporterExporter {

    /**
     * Parses a CSV string and returns structured records with quote and escaped quote support.
     */
    fun parseRawCsv(csvText: String): List<List<String>> {
        val cleanText = csvText.replace("^\uFEFF".toRegex(), "") // Strip UTF-8 BOM
        val rows = mutableListOf<List<String>>()
        var currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < cleanText.length) {
            val c = cleanText[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < cleanText.length && cleanText[i + 1] == '"') {
                        currentField.append('"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                if (c == '"') {
                    inQuotes = true
                } else if (c == ',') {
                    currentRecord.add(currentField.toString())
                    currentField.clear()
                } else if (c == '\n' || c == '\r') {
                    if (c == '\r' && i + 1 < cleanText.length && cleanText[i + 1] == '\n') {
                        i++
                    }
                    currentRecord.add(currentField.toString())
                    currentField.clear()
                    if (currentRecord.any { it.isNotBlank() }) {
                        rows.add(currentRecord)
                    }
                    currentRecord = mutableListOf()
                } else {
                    currentField.append(c)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            currentRecord.add(currentField.toString())
            if (currentRecord.any { it.isNotBlank() }) {
                rows.add(currentRecord)
            }
        }

        return rows
    }

    /**
     * Imports password records from a CSV file content string.
     */
    fun importFromCsv(
        csvContent: String,
        existingCredentials: List<SecretCredentialEntity>
    ): CsvImportResult {
        val rows = parseRawCsv(csvContent)
        if (rows.isEmpty()) {
            return CsvImportResult("Unknown CSV", 0, emptyList(), 0, 0, listOf("The CSV file is empty"))
        }

        val header = rows[0].map { it.trim().lowercase(Locale.US) }
        fun findColIndex(vararg keywords: String): Int {
            return header.indexOfFirst { col -> keywords.any { kw -> col == kw || col.contains(kw) } }
        }

        val nameCol = findColIndex("name", "title", "site", "account", "service")
        val urlCol = findColIndex("url", "login_uri", "website", "uri", "link")
        val usernameCol = findColIndex("username", "login_username", "user", "email", "login")
        val passCol = findColIndex("password", "login_password", "pass")
        val noteCol = findColIndex("note", "notes", "extra", "comments")
        val categoryCol = findColIndex("grouping", "folder", "category", "type")

        if (passCol < 0) {
            return CsvImportResult(
                "Unsupported CSV",
                rows.size - 1,
                emptyList(),
                0,
                rows.size - 1,
                listOf("No password column found. Expected a header like 'name', 'url', 'username', 'password'")
            )
        }

        val detectedSource = when {
            header.contains("login_uri") -> "Bitwarden"
            header.contains("grouping") -> "LastPass"
            header.contains("title") -> "1Password"
            header.joinToString(",") == "name,url,username,password,note" || header.joinToString(",") == "name,url,username,password" -> "Google Password Manager"
            else -> "Generic CSV"
        }

        val existingKeys = existingCredentials.map { "${it.serviceName.lowercase(Locale.US)}|${it.username.lowercase(Locale.US)}" }.toSet()
        val importedItems = mutableListOf<ImportedItem>()
        var duplicateCount = 0
        var skippedCount = 0

        for (r in 1 until rows.size) {
            val record = rows[r]
            fun getVal(colIdx: Int): String {
                return if (colIdx >= 0 && colIdx < record.size) record[colIdx].trim() else ""
            }

            val website = getVal(urlCol)
            val password = getVal(passCol)
            var name = getVal(nameCol)

            if (name.isBlank() && website.isNotBlank()) {
                name = try {
                    val cleanUrl = if (website.contains("://")) website else "https://$website"
                    android.net.Uri.parse(cleanUrl).host?.replace("^www\\.".toRegex(), "") ?: website
                } catch (e: Exception) {
                    website
                }
            }

            if (name.isBlank() || password.isBlank()) {
                skippedCount++
                continue
            }

            val username = getVal(usernameCol)
            val key = "${name.lowercase(Locale.US)}|${username.lowercase(Locale.US)}"

            if (existingKeys.contains(key) || importedItems.any { "${it.serviceName.lowercase(Locale.US)}|${it.username.lowercase(Locale.US)}" == key }) {
                duplicateCount++
                continue
            }

            val rawCategory = getVal(categoryCol).lowercase(Locale.US)
            val category = when {
                rawCategory.contains("work") || rawCategory.contains("job") -> "Work"
                rawCategory.contains("social") -> "Social"
                rawCategory.contains("finance") || rawCategory.contains("bank") -> "Finance"
                else -> "Personal"
            }

            val notes = getVal(noteCol)

            importedItems.add(
                ImportedItem(
                    serviceName = name,
                    username = username,
                    website = website,
                    password = password,
                    category = category,
                    notes = notes
                )
            )
        }

        return CsvImportResult(
            detectedSource = detectedSource,
            totalRows = rows.size - 1,
            importedItems = importedItems,
            duplicateCount = duplicateCount,
            skippedCount = skippedCount,
            errors = emptyList()
        )
    }

    /**
     * Exports credential items to an RFC 4180 compliant CSV string for Google Password Manager format.
     */
    fun exportToCsv(credentials: List<Pair<SecretCredentialEntity, String>>): String {
        val sb = StringBuilder()
        sb.append("name,url,username,password,note\r\n")

        credentials.forEach { (entity, plaintextPassword) ->
            sb.append(escapeCsvField(entity.serviceName)).append(",")
            sb.append(escapeCsvField(entity.website)).append(",")
            sb.append(escapeCsvField(entity.username)).append(",")
            sb.append(escapeCsvField(plaintextPassword)).append(",")
            sb.append(escapeCsvField(entity.notes)).append("\r\n")
        }

        return sb.toString()
    }

    private fun escapeCsvField(value: String): String {
        if (value.contains("\"") || value.contains(",") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }
}
