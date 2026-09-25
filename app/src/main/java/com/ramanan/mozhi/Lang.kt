package com.ramanan.mozhi

import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.Locale

/** Languages the app works with: ML Kit code + locale used for speech output. */
enum class Lang(val label: String, val mlKitCode: String, val locale: Locale, val speechTag: String) {
    TAMIL("Tamil", TranslateLanguage.TAMIL, Locale("ta", "IN"), "ta-IN"),
    ENGLISH("English", TranslateLanguage.ENGLISH, Locale("en", "IN"), "en-IN"),
    HINDI("Hindi", TranslateLanguage.HINDI, Locale("hi", "IN"), "hi-IN"),
    MALAY("Malay", TranslateLanguage.MALAY, Locale("ms", "MY"), "ms-MY");
}

/** What the user is translating from. */
enum class Source { TANGLISH, ENGLISH, VOICE }
