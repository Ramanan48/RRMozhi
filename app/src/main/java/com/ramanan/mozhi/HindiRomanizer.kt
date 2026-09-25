package com.ramanan.mozhi

/**
 * Writes Hindi (Devanagari) in simple English letters, the way people type Hinglish:
 * "आप कैसे हैं" → "aap kaise hain", "मेरा नाम" → "mera naam".
 * Works fully offline. Includes the usual "schwa deletion" so कमरा reads "kamra", not "kamaraa".
 */
object HindiRomanizer {

    private const val VIRAMA = '्'
    private const val NUKTA = '़'

    private val consonants = mapOf(
        'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh", 'ङ' to "n",
        'च' to "ch", 'छ' to "chh", 'ज' to "j", 'झ' to "jh", 'ञ' to "n",
        'ट' to "t", 'ठ' to "th", 'ड' to "d", 'ढ' to "dh", 'ण' to "n",
        'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
        'प' to "p", 'फ' to "ph", 'ब' to "b", 'भ' to "bh", 'म' to "m",
        'य' to "y", 'र' to "r", 'ल' to "l", 'ळ' to "l", 'व' to "v",
        'श' to "sh", 'ष' to "sh", 'स' to "s", 'ह' to "h",
        // precomposed nukta letters
        '\u0958' to "q", '\u0959' to "kh", '\u095A' to "gh", '\u095B' to "z",
        '\u095C' to "d", '\u095D' to "dh", '\u095E' to "f", '\u095F' to "y",
    )
    private val nuktaForms = mapOf("k" to "q", "g" to "gh", "j" to "z", "ph" to "f")

    private val independentVowels = mapOf(
        'अ' to "a", 'आ' to "aa", 'इ' to "i", 'ई' to "ee", 'उ' to "u", 'ऊ' to "oo",
        'ऋ' to "ri", 'ए' to "e", 'ऐ' to "ai", 'ओ' to "o", 'औ' to "au", 'ऑ' to "o", 'ऍ' to "e",
    )
    private val matras = mapOf(
        'ा' to "aa", 'ि' to "i", 'ी' to "ee", 'ु' to "u", 'ू' to "oo", 'ृ' to "ri",
        'े' to "e", 'ै' to "ai", 'ो' to "o", 'ौ' to "au", 'ॉ' to "o", 'ॅ' to "e",
    )
    private val labials = setOf('प', 'फ', 'ब', 'भ', 'म')

    /** One syllable: consonant cluster + vowel (+ nasal/visarga). */
    private class Syllable(
        val cons: String,          // "" for a bare vowel
        val isCluster: Boolean,
        var vowel: String,         // "a" when inherent
        val inherent: Boolean,
        var tail: String = "",     // n / m / h
    ) {
        var deleted = false
    }

    fun romanize(text: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (isDevanagariLetter(c)) {
                var j = i
                while (j < text.length && isDevanagariLetter(text[j])) j++
                out.append(romanizeWord(text.substring(i, j)))
                i = j
            } else {
                out.append(
                    when (c) {
                        '।', '॥' -> '.'
                        in '०'..'९' -> '0' + (c - '०')
                        else -> c
                    }
                )
                i++
            }
        }
        return out.toString()
    }

    private fun isDevanagariLetter(c: Char) = c in 'ऀ'..'ॣ' || c in '॰'..'ॿ'

    private fun romanizeWord(w: String): String {
        val units = mutableListOf<Syllable>()
        var i = 0
        while (i < w.length) {
            val c = w[i]
            when {
                consonants.containsKey(c) -> {
                    // gather cluster: C (nukta) (virama C (nukta))*
                    val cons = StringBuilder()
                    var count = 0
                    while (i < w.length && consonants.containsKey(w[i])) {
                        var r = consonants.getValue(w[i])
                        i++
                        if (i < w.length && w[i] == NUKTA) { r = nuktaForms[r] ?: r; i++ }
                        cons.append(r); count++
                        if (i < w.length && w[i] == VIRAMA && i + 1 < w.length && consonants.containsKey(w[i + 1])) {
                            i++ // join into cluster
                        } else break
                    }
                    when {
                        i < w.length && w[i] == VIRAMA -> { // dead consonant at word end
                            units.add(Syllable(cons.toString(), count > 1, "", false)); i++
                        }
                        i < w.length && matras.containsKey(w[i]) -> {
                            units.add(Syllable(cons.toString(), count > 1, matras.getValue(w[i]), false)); i++
                        }
                        else -> units.add(Syllable(cons.toString(), count > 1, "a", true))
                    }
                }
                independentVowels.containsKey(c) -> {
                    units.add(Syllable("", false, independentVowels.getValue(c), false)); i++
                }
                c == 'ं' || c == 'ँ' -> {
                    val next = w.getOrNull(i + 1)
                    units.lastOrNull()?.let { it.tail += if (next != null && next in labials) "m" else "n" }
                    i++
                }
                c == 'ः' -> { units.lastOrNull()?.let { it.tail += "h" }; i++ }
                else -> i++ // unknown sign: skip
            }
        }
        if (units.isEmpty()) return ""

        // Schwa deletion (right to left)
        val n = units.size
        if (n > 1 && units[n - 1].inherent && units[n - 1].tail.isEmpty()) units[n - 1].deleted = true
        var k = n - 2
        while (k >= 1) {
            val u = units[k]
            val prev = units[k - 1]
            val next = units[k + 1]
            if (u.inherent && !u.isCluster && u.tail.isEmpty() &&
                !prev.deleted && prev.vowel.isNotEmpty() &&
                !next.deleted && !next.isCluster && next.vowel.isNotEmpty() && next.cons.isNotEmpty()
            ) {
                u.deleted = true
                k -= 2 // never delete two schwas side by side
            } else {
                k--
            }
        }

        // Hinglish style: word-final long "aa"/"ee" written short (raha, kya, ja, nahin)
        val last = units.last()
        if (last.cons.isNotEmpty()) {
            if (last.vowel == "aa") last.vowel = "a"
            if (last.vowel == "ee") last.vowel = "i"
        }

        val sb = StringBuilder()
        for (u in units) {
            sb.append(u.cons)
            if (!u.deleted) sb.append(u.vowel)
            sb.append(u.tail)
        }
        return sb.toString().replace("chchh", "chh") // अच्छा → achha
    }
}
