package ai.bondhu.assistant.core

import android.os.SystemClock

enum class Risk(val level: Int) { R0(0), R1(1), R2(2), R3(3) }

enum class Decision { ALLOW, DENY, CONFIRM, CLARIFY }

data class ToolSpec(
    val id: String,
    val description: String,
    val risk: Risk,
    val readonly: Boolean,
)

data class ToolRequest(
    val id: String,
    val args: Map<String, String> = emptyMap(),
    val spokenText: String = "",
    val confirmed: Boolean = false,
)

data class ToolResult(
    val ok: Boolean,
    val message: String,
    val proof: String = "",
    val error: String? = null,
)

data class ParsedIntent(
    val request: ToolRequest? = null,
    val clarify: String? = null,
    val kill: Boolean = false,
)

data class ActionEvent(
    val tool: String,
    val args: Map<String, String>,
    val risk: Risk,
    val decision: Decision,
    val approvedBy: String,
    val status: String,
    val proof: String,
    val latencyMs: Long,
    val error: String?,
)

object ClockCompat {
    fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
}
