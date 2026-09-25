package com.ramanan.mozhi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Online translation through Google Translate's public web endpoint.
 * Much better than the on-device model for everyday spoken Tamil ("saptiya", "enga irukka").
 * Throws on any problem (no internet, blocked, bad reply) so the caller can fall back to ML Kit offline.
 */
object OnlineTranslator {

    private const val ENDPOINT = "https://translate.googleapis.com/translate_a/single"
    private const val MAX_CHUNK = 800 // characters per request, keeps the URL short

    suspend fun translate(text: String, from: Lang, to: Lang): String = withContext(Dispatchers.IO) {
        if (from == to) return@withContext text
        text.lines().joinToString("\n") { line ->
            if (line.isBlank()) line
            else chunk(line).joinToString(" ") { request(it, from.mlKitCode, to.mlKitCode) }
        }
    }

    /** Split very long lines on spaces so each request stays small. */
    private fun chunk(line: String): List<String> {
        if (line.length <= MAX_CHUNK) return listOf(line)
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        for (word in line.split(" ")) {
            if (current.length + word.length + 1 > MAX_CHUNK && current.isNotEmpty()) {
                parts.add(current.toString()); current.clear()
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(word)
        }
        if (current.isNotEmpty()) parts.add(current.toString())
        return parts
    }

    private fun request(text: String, sl: String, tl: String): String {
        val q = URLEncoder.encode(text, "UTF-8")
        val url = URL("$ENDPOINT?client=gtx&sl=$sl&tl=$tl&dt=t&ie=UTF-8&oe=UTF-8&q=$q")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 6000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) RRMozhi")
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${conn.responseCode}")
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            // Reply: [[["translated part","source part",...], ["next part", ...]], null, "ta", ...]
            val segments = JSONArray(body).getJSONArray(0)
            val out = StringBuilder()
            for (i in 0 until segments.length()) {
                val seg = segments.optJSONArray(i) ?: continue
                if (!seg.isNull(0)) out.append(seg.getString(0))
            }
            val result = out.toString().trim()
            if (result.isEmpty()) error("Empty translation")
            return result
        } finally {
            conn.disconnect()
        }
    }
}
