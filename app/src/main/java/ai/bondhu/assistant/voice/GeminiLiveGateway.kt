package ai.bondhu.assistant.voice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini Live adapter. P0 uses LocalVoiceGateway by default because no key is bundled.
 * Supplying a key in Settings activates this adapter through the same VoiceGateway contract.
 */
@Singleton
class GeminiLiveGateway @Inject constructor() : BaseVoiceGateway() {
    private val client = OkHttpClient()
    private var socket: WebSocket? = null

    suspend fun connect(apiKey: String, model: String = "models/gemini-2.0-flash-exp"): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext false
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val setup = JSONObject().apply {
                    put("setup", JSONObject().apply {
                        put("model", model)
                        put("generation_config", JSONObject().put("response_modalality", "AUDIO"))
                    })
                }
                webSocket.send(setup.toString())
                _state.value = VoiceState.Idle
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _state.value = VoiceState.Error("Gemini Live: ${t.message ?: "connect failed"}")
            }
        })
        true
    }

    override suspend fun listenOnce(): String? {
        _state.value = VoiceState.Error("Gemini Live adapter: speak through the live session. P0 uses local voice mode.")
        return null
    }

    override suspend fun speak(text: String) {
        val payload = JSONObject().apply {
            put("realtime_input", JSONObject().apply {
                put("text", text)
            })
        }.toString()
        socket?.send(payload)
    }

    override fun cancel() { socket?.cancel() }
    override fun destroy() { socket?.cancel(); client.dispatcher.executorService.shutdown() }
}
