package com.ramanan.mozhi

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.common.MlKitException
import com.ramanan.mozhi.databinding.ActivityMainBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private val engine = TranslationEngine()

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsFailed = false

    private var translateJob: Job? = null
    private var lastResult = ""
    private var lastRoman = ""
    private var lastResultLang = Lang.HINDI

    /** Opens the phone's speech recogniser in the chosen language and receives what was said. */
    private val speechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    val spoken = result.data
                        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        ?.firstOrNull()
                        .orEmpty()
                    if (spoken.isNotBlank()) {
                        binding.heardText.setText(spoken)
                        translate()
                    } else {
                        toast(getString(R.string.err_speech_failed))
                    }
                }
                Activity.RESULT_CANCELED -> Unit // user closed the mic pop-up
                else -> toast(getString(R.string.err_speech_failed)) // no match / network / audio error
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)

        binding.modeGroup.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) applyMode()
        }
        binding.sourceGroup.setOnCheckedStateChangeListener { _, _ -> applySource() }
        binding.speakGroup.setOnCheckedStateChangeListener { _, _ ->
            binding.heardText.setText("")
            applySource()
        }
        binding.targetGroup.setOnCheckedStateChangeListener { _, _ ->
            // Switching the target language re-translates what is on screen (or being translated)
            if (binding.resultCard.isVisible || translateJob?.isActive == true) translate()
        }

        binding.translateButton.setOnClickListener { translate() }
        binding.micButton.setOnClickListener { startListening() }
        binding.speakButton.setOnClickListener { speakResult() }
        binding.copyButton.setOnClickListener { copyResult() }
        binding.shareButton.setOnClickListener { shareResult() }

        applyMode()
        handleSharedText(intent)
    }

    // ---------------------------------------------------------------- UI state

    private fun isVoiceMode() = binding.modeGroup.checkedButtonId == R.id.btnModeVoice

    private fun currentSource(): Source = when {
        isVoiceMode() -> Source.VOICE
        binding.chipEnglish.isChecked -> Source.ENGLISH
        else -> Source.TANGLISH
    }

    /** Language the user will speak in Voice mode. */
    private fun spokenLang(): Lang = when (binding.speakGroup.checkedChipId) {
        R.id.chipSpeakHindi -> Lang.HINDI
        R.id.chipSpeakEnglish -> Lang.ENGLISH
        R.id.chipSpeakMalay -> Lang.MALAY
        else -> Lang.TAMIL
    }

    /** Language of the text that goes into the translator (Tanglish is converted to Tamil first). */
    private fun sourceLang(): Lang = when (currentSource()) {
        Source.TANGLISH -> Lang.TAMIL
        Source.ENGLISH -> Lang.ENGLISH
        Source.VOICE -> spokenLang()
    }

    private fun currentTarget(): Lang = when (binding.targetGroup.checkedChipId) {
        R.id.chipEnglishTarget -> Lang.ENGLISH
        R.id.chipMalay -> Lang.MALAY
        R.id.chipTamilTarget -> Lang.TAMIL
        else -> Lang.HINDI
    }

    private fun applyMode() {
        val voice = isVoiceMode()
        binding.textSection.isVisible = !voice
        binding.voiceSection.isVisible = voice
        applySource()
    }

    /**
     * Text:  Tanglish → Hindi / English / Malay,  English → Hindi / Malay / Tamil.
     * Voice: Tamil → Hindi / English / Malay,  Hindi / English / Malay → Tamil (others also allowed).
     * The target chip for the source language itself is always hidden.
     */
    private fun applySource() {
        translateJob?.cancel()
        setBusy(false)
        hideResult()

        val from = sourceLang()
        val voice = isVoiceMode()
        val targetChips = mapOf(
            Lang.HINDI to binding.chipHindi,
            Lang.ENGLISH to binding.chipEnglishTarget,
            Lang.MALAY to binding.chipMalay,
            Lang.TAMIL to binding.chipTamilTarget,
        )
        // Tanglish text goes out of Tamil, so Tamil is never a target there
        targetChips.forEach { (lang, chip) -> chip.isVisible = lang != from }

        when {
            // Speaking Hindi / English / Malay → translate into Tamil by default
            voice && from != Lang.TAMIL -> binding.chipTamilTarget.isChecked = true
            // Selected target is now hidden (same as source) → pick the first visible one
            targetChips[currentTarget()]?.isVisible != true ->
                targetChips.values.first { it.isVisible }.isChecked = true
        }

        binding.inputLayout.hint = getString(
            if (from == Lang.ENGLISH) R.string.hint_english else R.string.hint_tanglish
        )
        binding.tamilPreview.isVisible = false

        val spoken = spokenLang()
        binding.voiceTitle.text = getString(R.string.voice_title_fmt, spoken.label)
        binding.voiceSubtitle.text = getString(R.string.voice_subtitle_fmt, spoken.label)
        binding.heardLayout.hint = getString(R.string.heard_hint_fmt, spoken.label)
    }

    private fun setBusy(busy: Boolean, status: String? = null) {
        binding.progress.isVisible = busy
        binding.translateButton.isEnabled = !busy
        binding.micButton.isEnabled = !busy
        setStatus(status)
    }

    private fun setStatus(status: String?) {
        binding.statusText.isVisible = !status.isNullOrBlank()
        binding.statusText.text = status.orEmpty()
    }

    private fun hideResult() {
        binding.resultCard.isVisible = false
        tts?.stop()
    }

    private fun showResult(text: String, lang: Lang, offline: Boolean) {
        lastResult = text
        lastResultLang = lang
        binding.resultLabel.text = getString(
            if (offline) R.string.result_label_offline else R.string.result_label, lang.label
        )
        binding.resultText.text = text
        // Hindi script is hard to read for many users, so also show it in English letters
        // (only when the result really contains Hindi / Devanagari letters)
        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        lastRoman = if (lang == Lang.HINDI && hasDevanagari) HindiRomanizer.romanize(text) else ""
        binding.resultRoman.text = lastRoman
        binding.resultRoman.isVisible = lastRoman.isNotBlank()
        binding.resultCard.isVisible = true
        binding.root.post {
            binding.resultCard.requestRectangleOnScreen(
                android.graphics.Rect(0, 0, binding.resultCard.width, binding.resultCard.height)
            )
        }
    }

    // ---------------------------------------------------------------- translate

    private fun translate() {
        val source = currentSource()
        val input = when (source) {
            Source.VOICE -> binding.heardText.text?.toString().orEmpty()
            else -> binding.inputText.text?.toString().orEmpty()
        }.trim()

        if (input.isEmpty()) {
            toast(getString(if (source == Source.VOICE) R.string.err_empty_voice else R.string.err_empty_text))
            return
        }
        val target = currentTarget()
        hideKeyboard()

        translateJob?.cancel()
        hideResult() // never show an old result next to a new error
        translateJob = lifecycleScope.launch {
            setBusy(true, getString(R.string.status_translating))
            try {
                // Step 1: get the text into a language ML Kit understands
                val (text, from) = when (source) {
                    Source.TANGLISH -> {
                        setStatus(getString(R.string.status_converting))
                        val tamil = TanglishTransliterator.toTamil(input)
                        binding.tamilPreview.text = getString(R.string.tamil_preview, tamil)
                        binding.tamilPreview.isVisible = true
                        tamil to Lang.TAMIL
                    }
                    Source.ENGLISH -> input to Lang.ENGLISH
                    Source.VOICE -> input to spokenLang()
                }

                // Step 2: online Google Translate first (understands everyday spoken Tamil)
                setStatus(getString(R.string.status_translating))
                val online = try {
                    OnlineTranslator.translate(text, from, target)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null // no internet or service unavailable → offline below
                }

                if (online != null) {
                    showResult(online, target, offline = false)
                    setBusy(false)
                    prefetchOfflinePacks(from, target)
                    return@launch
                }

                // Step 3: offline fallback, on-phone ML Kit (download packs the first time)
                if (!engine.isReady(from, target)) {
                    setStatus(getString(R.string.status_downloading, from.label, target.label))
                    engine.downloadIfNeeded(from, target)
                }
                setStatus(getString(R.string.status_translating_offline))
                val result = engine.translate(text, from, target)

                // The offline model copies words it doesn't know. Don't pass that off as a translation.
                if (result.trim() == text.trim()) {
                    setBusy(false)
                    setStatus(getString(R.string.err_offline_not_understood))
                    return@launch
                }
                showResult(result, target, offline = true)
                setBusy(false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: MlKitException) {
                setBusy(false)
                val msg = if (e.errorCode == MlKitException.UNAVAILABLE || e.errorCode == MlKitException.NETWORK_ISSUE)
                    getString(R.string.err_download)
                else getString(R.string.err_generic, e.localizedMessage ?: e.toString())
                setStatus(msg)
            } catch (e: Exception) {
                setBusy(false)
                setStatus(getString(R.string.err_generic, e.localizedMessage ?: e.toString()))
            }
        }
    }

    /** After an online translation, quietly fetch the offline packs on Wi-Fi so the app also works without internet. */
    private fun prefetchOfflinePacks(from: Lang, to: Lang) {
        lifecycleScope.launch {
            try {
                if (!engine.isReady(from, to)) engine.downloadIfNeeded(from, to, wifiOnly = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // not on Wi-Fi or no space: try again next time
            }
        }
    }

    // ---------------------------------------------------------------- voice input

    private fun startListening() {
        val spoken = spokenLang()
        val prompt = if (spoken == Lang.TAMIL) getString(R.string.speak_prompt_tamil)
        else getString(R.string.speak_prompt_fmt, spoken.label)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, spoken.speechTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, spoken.speechTag)
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            toast(getString(R.string.err_no_speech))
        }
    }

    // ---------------------------------------------------------------- voice output

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        ttsFailed = !ttsReady
    }

    private fun isMissing(res: Int) =
        res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED

    private fun speakResult() {
        val engineTts = tts
        if (ttsFailed) {
            toast(getString(R.string.err_tts_unavailable)); return
        }
        if (!ttsReady || engineTts == null) {
            toast(getString(R.string.err_tts_not_ready)); return
        }
        var res = engineTts.setLanguage(lastResultLang.locale)
        // Many phones have no Malay voice; Indonesian is close enough to read Malay text
        if (isMissing(res) && lastResultLang == Lang.MALAY) {
            res = engineTts.setLanguage(java.util.Locale("id", "ID"))
            if (!isMissing(res)) toast(getString(R.string.tts_fallback_malay))
        }
        if (isMissing(res)) {
            Snackbar.make(binding.root, getString(R.string.err_tts_lang, lastResultLang.label), Snackbar.LENGTH_LONG)
                .setAction(R.string.install) {
                    try {
                        startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
                    } catch (e: ActivityNotFoundException) {
                        toast(getString(R.string.err_tts_lang, lastResultLang.label))
                    }
                }
                .show()
            return
        }
        engineTts.speak(lastResult, TextToSpeech.QUEUE_FLUSH, null, "mozhi-result")
    }

    // ---------------------------------------------------------------- copy / share

    private fun copyResult() {
        val cm = getSystemService(ClipboardManager::class.java) ?: return
        cm.setPrimaryClip(ClipData.newPlainText("translation", lastResult))
        // Android 13+ shows its own "copied" confirmation
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) toast(getString(R.string.copied))
    }

    private fun shareResult() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, if (lastRoman.isBlank()) lastResult else "$lastResult\n($lastRoman)")
        }
        startActivity(Intent.createChooser(send, getString(R.string.share)))
    }

    /** Text selected in another app via "RR Mozhi Translate" in the pop-up menu. */
    private fun handleSharedText(intent: Intent?) {
        if (intent?.action != Intent.ACTION_PROCESS_TEXT) return
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: return
        binding.btnModeText.isChecked = true
        binding.inputText.setText(text)
    }

    // ---------------------------------------------------------------- helpers

    private fun hideKeyboard() {
        val imm = getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
        binding.inputText.clearFocus()
        binding.heardText.clearFocus()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        engine.close()
        super.onDestroy()
    }
}
