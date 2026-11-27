package ai.bondhu.assistant.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocalVoiceGateway @Inject constructor(
    private val context: Context,
) : BaseVoiceGateway() {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("bn", "BD"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
            }
        }
    }

    override suspend fun listenOnce(): String? {
        val rec = recognizer ?: run {
            _state.value = VoiceState.Error("এই ফোনে স্পিচ রিকগনিশন নেই।")
            return null
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            _state.value = VoiceState.Error("মাইক্রোফোন অনুমতি দেওয়া হয়নি।")
            return null
        }
        _state.value = VoiceState.Listening
        return suspendCancellableCoroutine { cont ->
            var resumed = false
            rec.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    if (resumed) return
                    resumed = true
                    _state.value = VoiceState.Error(errorText(error))
                    cont.resume(null)
                }
                override fun onResults(results: Bundle?) {
                    if (resumed) return
                    resumed = true
                    val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = list?.firstOrNull().orEmpty().trim()
                    _state.value = if (text.isEmpty()) VoiceState.Error("কিছু শোনা যায়নি।") else VoiceState.Thinking
                    cont.resume(text.ifEmpty { null })
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "বলুন...")
            }
            cont.invokeOnCancellation { runCatching { rec.cancel() } }
            runCatching { rec.startListening(intent) }
        }
    }

    override suspend fun speak(text: String) {
        if (text.isBlank()) return
        _state.value = VoiceState.Speaking
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bondhu_${System.currentTimeMillis()}")
    }

    override fun cancel() {
        recognizer?.cancel()
        tts?.stop()
        _state.value = VoiceState.Idle
    }

    override fun destroy() {
        recognizer?.destroy()
        tts?.shutdown()
        recognizer = null
        tts = null
    }

    private fun errorText(error: Int) = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "অডিও ত্রুটি।"
        SpeechRecognizer.ERROR_CLIENT -> "মাইক ক্লায়েন্ট ত্রুটি।"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন অনুমতি নেই।"
        SpeechRecognizer.ERROR_NETWORK -> "নেটওয়ার্ক ত্রুটি।"
        SpeechRecognizer.ERROR_NO_MATCH -> "কথা বুঝতে পারিনি।"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "রিকগনাইজার ব্যস্ত।"
        SpeechRecognizer.ERROR_SERVER -> "সার্ভার ত্রুটি।"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কথা শোনা যায়নি।"
        else -> "স্পিচ ত্রুটি: $error"
    }
}
