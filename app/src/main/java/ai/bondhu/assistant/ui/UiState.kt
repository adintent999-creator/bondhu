package ai.bondhu.assistant.ui

import ai.bondhu.assistant.data.db.ActionLogEntity

data class UiState(
    val status: String = "প্রস্তুত",
    val lastHeard: String = "",
    val lastReply: String = "",
    val actionCount: Int = 0,
    val actions: List<ActionLogEntity> = emptyList(),
    val onboarded: Boolean = false,
    val micGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val keyPresent: Boolean = false,
    val busy: Boolean = false,
)
