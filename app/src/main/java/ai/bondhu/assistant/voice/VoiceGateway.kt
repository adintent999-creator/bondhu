package ai.bondhu.assistant.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class VoiceState {
    data object Idle : VoiceState()
    data object Listening : VoiceState()
    data object Thinking : VoiceState()
    data object Speaking : VoiceState()
    data class Error(val message: String) : VoiceState()
}

interface VoiceGateway {
    val state: StateFlow<VoiceState>
    suspend fun listenOnce(): String?
    suspend fun speak(text: String)
    fun cancel()
    fun destroy()
}

abstract class BaseVoiceGateway : VoiceGateway {
    protected val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    override val state: StateFlow<VoiceState> = _state.asStateFlow()
}
