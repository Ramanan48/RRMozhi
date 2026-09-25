package com.ramanan.mozhi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Turns Tanglish (Tamil typed in English letters) into Tamil script,
 * so it can then be translated like normal Tamil.
 *
 * 1. Online: Google Input Tools transliteration (best accuracy, understands common spellings).
 * 2. Offline fallback: the phonetic rules in [OfflineTamilTransliterator].
 */
object TanglishTransliterator {

    private const val ENDPOINT = "https://inputtools.google.com/request"

    suspend fun toTamil(text: String): String = withContext(Dispatchers.IO) {
        var online = true
        text.lines().joinToString("\n") { line ->
            when {
                line.isBlank() -> line
                // Already in Tamil script (or no English letters at all): nothing to convert
                line.none { it in 'a'..'z' || it in 'A'..'Z' } -> line
                online -> try {
                    fetchOnline(line)
                } catch (e: Exception) {
                    online = false // no network: stop trying for the remaining lines
                    OfflineTamilTransliterator.convert(line)
                }
                else -> OfflineTamilTransliterator.convert(line)
            }
        }
    }

    private fun fetchOnline(line: String): String {
        val query = URLEncoder.encode(line, "UTF-8")
        val url = URL("$ENDPOINT?text=$query&itc=ta-t-i0-und&num=1&cp=0&cs=1&ie=utf-8&oe=utf-8")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${conn.responseCode}")
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            // Response: ["SUCCESS",[["input text",["தமிழ் candidate", ...], ...], ...]]
            val root = JSONArray(body)
            if (root.getString(0) != "SUCCESS") error("Transliteration failed")
            val segments = root.getJSONArray(1)
            val out = StringBuilder()
            for (i in 0 until segments.length()) {
                val segment = segments.getJSONArray(i)
                val candidates = segment.getJSONArray(1)
                out.append(if (candidates.length() > 0) candidates.getString(0) else segment.getString(0))
            }
            val result = out.toString()
            if (result.isBlank()) error("Empty transliteration")
            return result
        } finally {
            conn.disconnect()
        }
    }
}

/**
 * Rule-based Tanglish → Tamil converter that works without internet.
 * Handles everyday spellings: "naan veetuku poren", "enga irukka", "nandri", "saapaadu".
 * Capital letters inside a word select the "hard" letters: L=ள, N=ண, R=ற, E=ஏ, O=ஓ.
 */
object OfflineTamilTransliterator {

    private const val VIRAMA = "்" // pulli

    // independent vowel to vowel sign (empty sign = inherent "a")
    private val vowels: List<Triple<String, String, String>> = listOf(
        Triple("aa", "ஆ", "ா"), Triple("ai", "ஐ", "ை"), Triple("au", "ஔ", "ௌ"),
        Triple("ii", "ஈ", "ீ"), Triple("ee", "ஈ", "ீ"), Triple("uu", "ஊ", "ூ"),
        Triple("oo", "ஊ", "ூ"), Triple("oa", "ஓ", "ோ"), Triple("ae", "ஏ", "ே"),
        Triple("A", "ஆ", "ா"), Triple("I", "ஈ", "ீ"), Triple("U", "ஊ", "ூ"),
        Triple("E", "ஏ", "ே"), Triple("O", "ஓ", "ோ"),
        Triple("a", "அ", ""), Triple("i", "இ", "ி"), Triple("u", "உ", "ு"),
        Triple("e", "எ", "ெ"), Triple("o", "ஒ", "ொ"),
    )

    // Consonant clusters first (longest match wins). Value = Tamil letters, last one takes the vowel sign.
    private val consonants: List<Pair<String, String>> = listOf(
        "ksh" to "க்ஷ", "ndr" to "ன்ற", "nth" to "ந்த", "ndh" to "ந்த",
        "ng" to "ங்க", "nj" to "ஞ்ச", "nd" to "ண்ட", "mb" to "ம்ப", "tr" to "ற்ற",
        "th" to "த", "dh" to "த", "ch" to "ச", "sh" to "ஷ", "zh" to "ழ",
        "kh" to "க", "gh" to "க", "bh" to "ப", "ph" to "ஃப",
        "k" to "க", "g" to "க", "c" to "க", "q" to "க", "s" to "ச", "j" to "ஜ",
        "t" to "ட", "d" to "ட", "p" to "ப", "b" to "ப", "m" to "ம", "y" to "ய",
        "r" to "ர", "l" to "ல", "v" to "வ", "w" to "வ", "h" to "ஹ", "f" to "ஃப",
        "z" to "ஜ", "x" to "க்ஸ", "L" to "ள", "N" to "ண", "R" to "ற", "S" to "ஸ",
        "n" to "ன", // word-initial n becomes ந (handled below)
    )

    fun convert(text: String): String {
        val out = StringBuilder()
        val word = StringBuilder()
        for (ch in text) {
            if (ch in 'a'..'z' || ch in 'A'..'Z') {
                word.append(ch)
            } else {
                if (word.isNotEmpty()) { out.append(convertWord(word.toString())); word.clear() }
                out.append(ch)
            }
        }
        if (word.isNotEmpty()) out.append(convertWord(word.toString()))
        return out.toString()
    }

    private fun convertWord(raw: String): String {
        // "Naan" or "NAAN" is just capitalisation, not a request for hard letters
        val w = if (raw.drop(1).all { it.isLowerCase() } || raw.all { it.isUpperCase() }) raw.lowercase() else raw
        val out = StringBuilder()
        var i = 0
        var pendingConsonant = false
        while (i < w.length) {
            val vowel = vowels.firstOrNull { w.startsWith(it.first, i) }
            if (vowel != null) {
                out.append(if (pendingConsonant) vowel.third else vowel.second)
                pendingConsonant = false
                i += vowel.first.length
                continue
            }
            val cons = consonants.firstOrNull { w.startsWith(it.first, i) }
            if (cons != null) {
                if (pendingConsonant) out.append(VIRAMA)
                val letters = if (cons.first == "n" && i == 0) "ந" else cons.second
                out.append(letters)
                pendingConsonant = true
                i += cons.first.length
                continue
            }
            if (pendingConsonant) { out.append(VIRAMA); pendingConsonant = false }
            out.append(w[i])
            i++
        }
        if (pendingConsonant) out.append(VIRAMA)
        return out.toString()
    }
}
