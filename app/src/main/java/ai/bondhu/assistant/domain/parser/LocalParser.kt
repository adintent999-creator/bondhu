package ai.bondhu.assistant.domain.parser

import ai.bondhu.assistant.core.ParsedIntent
import ai.bondhu.assistant.core.ToolRequest
import ai.bondhu.assistant.util.normalizeDigits

class LocalParser {
    fun parse(input: String): List<ParsedIntent> {
        val text = input.normalizeDigits()
        val lower = text.lowercase()
        if (listOf("থামো", "বন্ধ কর", "stop", "cancel").any { lower.contains(it) }) {
            return listOf(ParsedIntent(kill = true))
        }
        val chunks = lower.split(Regex(",\\s*|\\s+আর\\s+|\\s+তারপর\\s+|\\s+এবং\\s+|\\s+then\\s+|\\s+and\\s+"))
            .map { it.trim() }.filter { it.isNotBlank() }
        if (chunks.isEmpty()) return listOf(ParsedIntent(clarify = "আমি কিছু বুঝতে পারিনি। আবার বলুন।"))
        return chunks.map { parseChunk(it, text) }
    }

    private fun parseChunk(raw: String, original: String): ParsedIntent {
        val c = raw.trim()
        when {
            listOf("টর্চ", "torch").any { c.contains(it) } -> {
                val on = listOf("জ্বালা", "জ্বালাও", "অন", "on", "চালু").any { c.contains(it) }
                val off = listOf("নিভা", "নিভাও", "অফ", "off", "বন্ধ").any { c.contains(it) }
                if (!on && !off) return clarify()
                return ParsedIntent(ToolRequest("set_torch", mapOf("enabled" to on.toString()), original))
            }
            listOf("মেসেজ", "message", "messaging").any { c.contains(it) } ->
                return ParsedIntent(ToolRequest("open_app", mapOf("target" to "messages"), original))
            c.contains("dialer") || c.contains("ফোন অ্যাপ") || c.contains("কল অ্যাপ") ->
                return ParsedIntent(ToolRequest("open_app", mapOf("target" to "dialer"), original))
            c.contains("ক্যামেরা") || c.contains("camera") ->
                return ParsedIntent(ToolRequest("open_app", mapOf("target" to "camera"), original))
            c.contains("ব্যাটারি") || c.contains("battery") ->
                return ParsedIntent(ToolRequest("battery_detail", spokenText = original))
            listOf("সময়", "কতটা", "time", "ঘন্টা").any { c.contains(it) } ->
                return ParsedIntent(ToolRequest("get_time", spokenText = original))
            c.contains("অ্যাপ ") || c.contains("apps") || c.contains("অ্যাপলিস্ট") ->
                return ParsedIntent(ToolRequest("list_apps", spokenText = original))
            c.contains("ভলিউম") || c.contains("volume") -> {
                val pct = number(c) ?: 50
                return ParsedIntent(ToolRequest("set_volume", mapOf("percent" to pct.toString()), original))
            }
            c.contains("ব্রাইটনেস") || c.contains("brightness") -> {
                val pct = number(c) ?: 60
                return ParsedIntent(ToolRequest("set_brightness", mapOf("percent" to pct.toString()), original))
            }
            c.contains("ডিএনডি") || c.contains("dnd") || c.contains("বিরক্ত") -> {
                val on = !listOf("অফ", "off", "বন্ধ").any { c.contains(it) }
                return ParsedIntent(ToolRequest("set_dnd", mapOf("enabled" to on.toString()), original))
            }
            c.contains("অ্যালার্ম") || c.contains("alarm") -> {
                val minutes = number(c) ?: 0
                return ParsedIntent(ToolRequest("set_alarm", mapOf("minutes" to minutes.toString()), original))
            }
            c.contains("টাইমার") || c.contains("timer") -> {
                val minutes = number(c) ?: 5
                return ParsedIntent(ToolRequest("set_timer", mapOf("minutes" to minutes.toString()), original))
            }
            c.contains("মনে রাখ") || c.contains("remember") ->
                return ParsedIntent(ToolRequest("remember", spokenText = original))
            c.contains("ডিভাইস") || c.contains("status") ->
                return ParsedIntent(ToolRequest("device_status", spokenText = original))
        }
        return clarify()
    }

    private fun number(s: String): Int? {
        val m = Regex("\\d+").find(s) ?: return null
        return m.value.toIntOrNull()?.coerceIn(0, 100)
    }

    private fun clarify() = ParsedIntent(clarify = "এই কাজটি আমি এখনো জানি না। টর্চ, ভলিউম, অ্যাপ খোলা বা সময় জিজ্ঞেস করুন।")
}
