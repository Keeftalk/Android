package tn.keeftalk.chat.extension.parser

import kotlinx.serialization.json.Json
import tn.keeftalk.chat.extension.model.ExtensionManifest

class KtxpManifestParseException(message: String) : Exception(message)

object KtxpManifestParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        coerceInputValues = true
    }

    fun parseManifest(jsonString: String): ExtensionManifest {
        if (jsonString.isBlank()) {
            throw KtxpManifestParseException("Manifest content is blank.")
        }
        if (jsonString.length > 1_000_000) { // 1 MB limit for manifest text
            throw KtxpManifestParseException("Manifest exceeds maximum size limit of 1MB.")
        }

        val manifest = try {
            json.decodeFromString<ExtensionManifest>(jsonString)
        } catch (e: Exception) {
            throw KtxpManifestParseException("Failed to parse manifest JSON: ${e.message}")
        }

        if (!manifest.isValidPackageId()) {
            throw KtxpManifestParseException("Invalid extension ID '${manifest.id}'. Must use reverse-DNS format (e.g., com.publisher.extension).")
        }

        if (manifest.entry.isBlank()) {
            throw KtxpManifestParseException("Manifest entry path cannot be empty.")
        }

        if (manifest.name.isBlank()) {
            throw KtxpManifestParseException("Manifest extension name cannot be empty.")
        }

        return manifest
    }
}
