package ai.bondhu.assistant.ui

import ai.bondhu.assistant.core.SessionController
import ai.bondhu.assistant.data.prefs.SettingsStore
import ai.bondhu.assistant.data.repo.BondhuRepository
import ai.bondhu.assistant.domain.TaskRunner
import ai.bondhu.assistant.voice.LocalVoiceGateway
import ai.bondhu.assistant.voice.VoiceState
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runner: TaskRunner,
    private val voice: LocalVoiceGateway,
    private val repo: BondhuRepository,
    private val settings: SettingsStore,
) : ViewModel() {
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            voice.state.collect { st ->
                val text = when (st) {
                    VoiceState.Idle -> "প্রস্তুত"
                    VoiceState.Listening -> "শুনছি..."
                    VoiceState.Thinking -> "বুঝছি..."
                    VoiceState.Speaking -> "বলছি..."
                    is VoiceState.Error -> st.message
                }
                _ui.update { it.copy(status = text) }
            }
        }
        viewModelScope.launch {
            settings.onboarded.collect { done -> _ui.update { it.copy(onboarded = done) } }
        }
        refresh()
    }

    fun refresh() {
        _ui.update {
            it.copy(
                micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
                overlayGranted = Settings.canDrawOverlays(context),
            )
        }
        viewModelScope.launch {
            val hasKey = runCatching { settings.readGeminiKey() }.getOrNull() != null
            _ui.update {
                it.copy(
                    actionCount = repo.actionCount(),
                    actions = repo.actions(),
                    keyPresent = hasKey,
                )
            }
        }
    }

    fun setOnboarded() {
        viewModelScope.launch { settings.setOnboarded(true) }
        refresh()
    }

    fun saveKey(key: String) {
        viewModelScope.launch {
            settings.saveGeminiKey(key.trim())
            refresh()
        }
    }

    fun onPermissionsResult() {
        SessionController.newSession()
        refresh()
    }

    fun startVoiceScoped() {
        viewModelScope.launch { startVoice() }
    }

    suspend fun startVoice() {
        if (_ui.value.busy) return
        _ui.update { it.copy(busy = true) }
        val heard = voice.listenOnce()
        _ui.update { it.copy(busy = false, lastHeard = heard.orEmpty()) }
        if (!heard.isNullOrBlank()) handle(heard)
    }

    fun onText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { handle(text) }
    }

    private suspend fun handle(text: String) {
        repo.message("user", text)
        val outcome = runner.handle(text)
        repo.message("bondhu", outcome.spoken)
        repo.reply(outcome.spoken, 0)
        _ui.update {
            it.copy(
                lastReply = outcome.spoken,
                actionCount = repo.actionCount(),
                actions = repo.actions(),
                status = if (outcome.actionCount > 0) "${outcome.actionCount} টি কাজ সম্পন্ন" else "প্রস্তুত",
            )
        }
        voice.speak(outcome.spoken)
    }

    fun stop() {
        voice.cancel()
        SessionController.abort()
        _ui.update { it.copy(status = "থামানো হয়েছে", busy = false) }
    }

    override fun onCleared() {
        voice.destroy()
        super.onCleared()
    }
}
