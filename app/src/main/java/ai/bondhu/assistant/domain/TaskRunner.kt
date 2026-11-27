package ai.bondhu.assistant.domain

import ai.bondhu.assistant.core.ActionEvent
import ai.bondhu.assistant.core.Decision
import ai.bondhu.assistant.core.ParsedIntent
import ai.bondhu.assistant.core.SessionController
import ai.bondhu.assistant.core.ToolRequest
import ai.bondhu.assistant.data.repo.BondhuRepository
import ai.bondhu.assistant.domain.parser.LocalParser
import ai.bondhu.assistant.domain.policy.PolicyEngine
import ai.bondhu.assistant.domain.tool.SystemToolExecutor
import ai.bondhu.assistant.domain.tool.ToolRegistry
import javax.inject.Inject
import javax.inject.Singleton

data class RunOutcome(
    val spoken: String,
    val actionCount: Int,
    val needsConfirm: Boolean = false,
)

@Singleton
class TaskRunner @Inject constructor(
    private val parser: LocalParser,
    private val registry: ToolRegistry,
    private val policy: PolicyEngine,
    private val executor: SystemToolExecutor,
    private val repo: BondhuRepository,
) {
    private var pending: ToolRequest? = null

    suspend fun handle(text: String): RunOutcome {
        val trimmed = text.trim()
        if (isYes(trimmed) && pending != null) {
            val req = pending!!.copy(confirmed = true)
            pending = null
            return execute(listOf(ParsedIntent(req)))
        }
        if (isNo(trimmed) && pending != null) {
            pending = null
            return RunOutcome("ঠিক আছে, কাজটি বাতিল করলাম।", 0)
        }
        val parsed = parser.parse(trimmed)
        if (parsed.any { it.kill }) {
            SessionController.abort()
            return RunOutcome("থামলাম।", 0)
        }
        SessionController.reset()
        return execute(parsed)
    }

    private suspend fun execute(intents: List<ParsedIntent>): RunOutcome {
        val parts = mutableListOf<String>()
        var count = 0
        var needsConfirm = false
        for (intent in intents) {
            if (SessionController.isAborted()) break
            if (intent.clarify != null) {
                parts += intent.clarify!!
                continue
            }
            val request = intent.request ?: continue
            val spec = registry.get(request.id)
            if (spec == null) {
                parts += "অচেনা কাজ।"
                continue
            }
            val decision = policy.decide(spec, request)
            when (decision) {
                Decision.DENY -> {
                    repo.action(
                        ActionEvent(request.id, request.args, spec.risk, decision, "policy", "denied", "", 0, "policy_deny")
                    )
                    parts += "এই কাজটি আমি করব না।"
                }
                Decision.CONFIRM -> {
                    pending = request
                    repo.action(
                        ActionEvent(request.id, request.args, spec.risk, decision, "pending", "awaiting_approval", "", 0, null)
                    )
                    parts += "এটি অনুমতি চাই। নিশ্চিত করতে বলুন হ্যাঁ।"
                    needsConfirm = true
                }
                Decision.CLARIFY -> parts += "একটু পরিষ্কার করে বলুন।"
                Decision.ALLOW -> {
                    val start = System.currentTimeMillis()
                    val result = executor.execute(request)
                    val ms = System.currentTimeMillis() - start
                    val status = if (result.ok) "executed" else "failed"
                    repo.action(
                        ActionEvent(
                            tool = request.id,
                            args = request.args,
                            risk = spec.risk,
                            decision = decision,
                            approvedBy = "spoken",
                            status = status,
                            proof = result.proof,
                            latencyMs = ms,
                            error = result.error,
                        )
                    )
                    parts += result.message
                    if (result.ok) count++
                }
            }
        }
        val spoken = parts.joinToString(" ").trim().ifEmpty { "কিছু করা যায়নি।" }
        return RunOutcome(spoken, count, needsConfirm)
    }

    private fun isYes(s: String) = listOf("হ্যাঁ", "হ্যা", "yes", "ঠিক আছে", "ok").any { s.lowercase().contains(it) }
    private fun isNo(s: String) = listOf("না", "no", "বাতিল").any { s.lowercase().contains(it) }
}
