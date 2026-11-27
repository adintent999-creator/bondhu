package ai.bondhu.assistant.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocalVoiceGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : BaseVoiceGateway() {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        Log.d(TAG, "init recognitionAvailable=$available")
        if (available) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            Log.d(TAG, "recognizer created")
        } else {
            Log.e(TAG, "no speech recognizer on this device")
        }
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                var langResult = tts?.setLanguage(Locale("bn", "BD"))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED)
                ) {
                    val bnVoice = tts?.voices?.firstOrNull {
                        val language = it.locale.language.lowercase(Locale.getDefault())
                        language.startsWith("bn")
                    }
                    if (bnVoice != null) {
                        langResult = tts?.setVoice(bnVoice)
                        Log.d(TAG, "tts voice selected=${bnVoice.name}")
                    }
                }
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                    Log.w(TAG, "bn-BD unavailable; TTS fell back to English")
                }
                Log.d(TAG, "tts init status=$status langResult=$langResult")
            } else {
                Log.e(TAG, "tts init failed status=$status")
            }
        }
    }

    override suspend fun listenOnce(): String? {
        val rec = recognizer ?: run {
            Log.e(TAG, "listenOnce: recognizer is null")
            _state.value = VoiceState.Error("এই ফোনে স্পিচ রিকগনিশন নেই।")
            return null
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "listenOnce permission=$granted")
        if (!granted) {
            _state.value = VoiceState.Error("মাইক্রোফোন অনুমতি দেওয়া হয়নি।")
            return null
        }
        _state.value = VoiceState.Listening
        Log.d(TAG, "listenOnce start bn-BD")
        return suspendCancellableCoroutine { cont ->
            var resumed = false
            val timeout = Runnable {
                if (!resumed) {
                    resumed = true
                    Log.w(TAG, "listen timeout 12000ms")
                    _state.value = VoiceState.Error("সময় শেষ, আবার বলুন।")
                    cont.resume(null)
                }
            }
            mainHandler.postDelayed(timeout, LISTEN_TIMEOUT_MS)

            fun finish(text: String?, error: Int? = null) {
                if (resumed) return
                resumed = true
                mainHandler.removeCallbacks(timeout)
                if (error != null) {
                    Log.w(TAG, "listen error=$error ${errorText(error)}")
                    _state.value = VoiceState.Error(errorText(error))
                    cont.resume(null)
                } else {
                    val clean = text.orEmpty().trim()
                    Log.d(TAG, "listen result='$clean'")
                    _state.value = if (clean.isEmpty()) {
                        VoiceState.Error("কিছু শোনা যায়নি।")
                    } else {
                        VoiceState.Thinking
                    }
                    cont.resume(clean.ifEmpty { null })
                }
            }

            rec.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "onReadyForSpeech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "onBeginningOfSpeech")
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d(TAG, "onEndOfSpeech")
                }

                override fun onError(error: Int) = finish(null, error)

                override fun onResults(results: Bundle?) {
                    val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    finish(list?.firstOrNull())
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val part = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!part.isNullOrBlank()) Log.d(TAG, "partial='$part'")
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "বলুন...")
            }
            cont.invokeOnCancellation {
                mainHandler.removeCallbacks(timeout)
                runCatching { rec.cancel() }
            }
            val started = runCatching { rec.startListening(intent) }
            Log.d(TAG, "startListening called ok=${started.isFailure.not()} err=${started.exceptionOrNull()?.message}")
            if (started.isFailure) finish(null)
        }
    }

    override suspend fun speak(text: String) {
        if (text.isBlank()) return
        _state.value = VoiceState.Speaking
        val queueResult = tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "bondhu_${System.currentTimeMillis()}"
        )
        Log.d(TAG, "speak len=${text.length} queueResult=$queueResult")
    }

    override fun cancel() {
        Log.d(TAG, "cancel")
        recognizer?.cancel()
        tts?.stop()
        _state.value = VoiceState.Idle
    }

    override fun destroy() {
        Log.d(TAG, "destroy")
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

    private companion object {
        const val TAG = "BondhuVoice"
        const val LISTEN_TIMEOUT_MS = 12_000L
    }
}
