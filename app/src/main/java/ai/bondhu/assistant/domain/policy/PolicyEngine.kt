package ai.bondhu.assistant.domain.policy

import ai.bondhu.assistant.core.Decision
import ai.bondhu.assistant.core.Risk
import ai.bondhu.assistant.core.ToolRequest
import ai.bondhu.assistant.core.ToolSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PolicyEngine @Inject constructor() {
    private val denyWords = listOf(
        "upi", "bKash", "bkash", "nagad", "রকেট", "ব্যাংক", "bank", "otp",
        "password", "পাসওয়ার্ড", "pin", "পিন", "delete account", "factory reset",
        "ফ্যাক্টরি রিসেট", "অ্যাকাউন্ট মুছ",
    )

    fun decide(spec: ToolSpec, request: ToolRequest): Decision {
        val text = request.spokenText.lowercase()
        if (denyWords.any { text.contains(it.lowercase()) }) return Decision.DENY
        if (spec.risk == Risk.R2 || spec.risk == Risk.R3) {
            return if (request.confirmed) Decision.ALLOW else Decision.CONFIRM
        }
        return Decision.ALLOW
    }

    fun denyReason(text: String): String? = denyWords.firstOrNull {
        text.lowercase().contains(it.lowercase())
    }
}
