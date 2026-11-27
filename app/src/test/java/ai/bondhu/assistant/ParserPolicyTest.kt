package ai.bondhu.assistant

import ai.bondhu.assistant.core.Risk
import ai.bondhu.assistant.core.ToolRequest
import ai.bondhu.assistant.core.ToolSpec
import ai.bondhu.assistant.domain.parser.LocalParser
import ai.bondhu.assistant.domain.policy.PolicyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserPolicyTest {
    private val parser = LocalParser()
    private val policy = PolicyEngine()

    @Test
    fun parses_multistep_torch_and_open_app() {
        val intents = parser.parse("টর্চ জ্বালাও, তারপর মেসেজ অ্যাপ খোলো")
        assertEquals(2, intents.size)
        assertEquals("set_torch", intents[0].request?.id)
        assertEquals("true", intents[0].request?.args?.get("enabled"))
        assertEquals("open_app", intents[1].request?.id)
    }

    @Test
    fun parses_bengali_digits() {
        val intents = parser.parse("ভলিউম ৪০")
        assertEquals("40", intents[0].request?.args?.get("percent"))
    }

    @Test
    fun kill_switch_detected() {
        assertTrue(parser.parse("থামো")[0].kill)
    }

    @Test
    fun deny_banking_words() {
        val spec = ToolSpec("open_app", "open", Risk.R1, false)
        val decision = policy.decide(spec, ToolRequest("open_app", spokenText = "ব্যাংক অ্যাপ খোলো"))
        assertEquals("DENY", decision.name)
    }
}
