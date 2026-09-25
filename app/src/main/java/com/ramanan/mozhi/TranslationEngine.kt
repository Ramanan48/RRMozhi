package com.ramanan.mozhi

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

/**
 * Wraps ML Kit on-device translation.
 * Each language pack (~30 MB) is downloaded once; after that translation works offline.
 */
class TranslationEngine {

    private val clients = mutableMapOf<String, Translator>()
    private val modelManager = RemoteModelManager.getInstance()

    private fun client(from: Lang, to: Lang): Translator =
        clients.getOrPut("${from.mlKitCode}>${to.mlKitCode}") {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(from.mlKitCode)
                    .setTargetLanguage(to.mlKitCode)
                    .build()
            )
        }

    /** True if both language packs are already on the phone. */
    suspend fun isReady(from: Lang, to: Lang): Boolean =
        isDownloaded(from) && isDownloaded(to)

    private suspend fun isDownloaded(lang: Lang): Boolean {
        if (lang == Lang.ENGLISH) return true // English is built in
        return try {
            modelManager.isModelDownloaded(TranslateRemoteModel.Builder(lang.mlKitCode).build()).await()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun downloadIfNeeded(from: Lang, to: Lang, wifiOnly: Boolean = false) {
        val conditions = DownloadConditions.Builder().apply { if (wifiOnly) requireWifi() }.build()
        client(from, to).downloadModelIfNeeded(conditions).await()
    }

    suspend fun translate(text: String, from: Lang, to: Lang): String {
        if (from == to) return text
        downloadIfNeeded(from, to)
        val translator = client(from, to)
        // Translate line by line so paragraphs keep their shape
        return text.lines().map { line ->
            if (line.isBlank()) line else translator.translate(line).await()
        }.joinToString("\n")
    }

    fun close() {
        clients.values.forEach { it.close() }
        clients.clear()
    }
}
