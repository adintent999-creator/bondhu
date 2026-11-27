package ai.bondhu.assistant.domain.tool

import ai.bondhu.assistant.core.Risk
import ai.bondhu.assistant.core.ToolSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor() {
    private val specs = listOf(
        ToolSpec("device_status", "device model, OS, network", Risk.R0, true),
        ToolSpec("battery_detail", "battery percent and charging", Risk.R0, true),
        ToolSpec("get_time", "current time", Risk.R0, true),
        ToolSpec("list_apps", "installed apps", Risk.R0, true),
        ToolSpec("open_app", "open an installed app", Risk.R1, false),
        ToolSpec("set_torch", "camera torch on/off", Risk.R1, false),
        ToolSpec("set_volume", "media volume percent", Risk.R1, false),
        ToolSpec("set_brightness", "screen brightness", Risk.R1, false),
        ToolSpec("set_dnd", "do not disturb", Risk.R2, false),
        ToolSpec("set_alarm", "set an alarm", Risk.R2, false),
        ToolSpec("set_timer", "set a timer", Risk.R2, false),
        ToolSpec("remember", "store a memory", Risk.R1, false),
    ).associateBy { it.id }

    fun all(): List<ToolSpec> = specs.values.toList()
    fun get(id: String): ToolSpec? = specs[id]
}
