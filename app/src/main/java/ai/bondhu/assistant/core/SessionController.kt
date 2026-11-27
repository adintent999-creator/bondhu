package ai.bondhu.assistant.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object SessionController {
    private val _aborted = MutableStateFlow(false)
    val aborted: StateFlow<Boolean> = _aborted.asStateFlow()
    var sessionId: String = UUID.randomUUID().toString()
        private set

    fun newSession() {
        sessionId = UUID.randomUUID().toString()
        _aborted.value = false
    }

    fun abort() { _aborted.value = true }
    fun reset() { _aborted.value = false }
    fun isAborted() = _aborted.value
}
