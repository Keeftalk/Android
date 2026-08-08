package com.keeftalk.chat.util

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale

class TranslationManager {

    private val languageIdentifier = LanguageIdentification.getClient()

    suspend fun detectLanguage(text: String): String? {
        return try {
            languageIdentifier.identifyLanguage(text).await().let { languageCode ->
                if (languageCode == "und") null else languageCode
            }
        } catch (e: Exception) {
            Log.e("TranslationManager", "Language detection failed", e)
            null
        }
    }

    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String = Locale.getDefault().language
    ): String? {
        val source = TranslateLanguage.fromLanguageTag(sourceLang) ?: return null
        val target = TranslateLanguage.fromLanguageTag(targetLang) ?: return null

        if (source == target) return text

        val translator = getTranslator(source, target)
        
        return try {
            translator.downloadModelIfNeeded().await()
            translator.translate(text).await()
        } catch (e: Exception) {
            Log.e("TranslationManager", "Translation failed", e)
            null
        } finally {
            translator.close()
        }
    }

    suspend fun preloadModel(langTag: String) {
        val lang = TranslateLanguage.fromLanguageTag(langTag) ?: return
        // To preload, we need a pair. We'll preload with English as a common base
        if (lang == TranslateLanguage.ENGLISH) return
        
        val translator = getTranslator(lang, TranslateLanguage.ENGLISH)
        try {
            translator.downloadModelIfNeeded().await()
        } finally {
            translator.close()
        }
    }

    private fun getTranslator(source: String, target: String) = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
    )
}
