package ai.bondhu.assistant.data.repo

import ai.bondhu.assistant.core.ActionEvent
import ai.bondhu.assistant.core.SessionController
import ai.bondhu.assistant.data.db.ActionLogEntity
import ai.bondhu.assistant.data.db.BondhuDao
import ai.bondhu.assistant.data.db.MemoryEntity
import ai.bondhu.assistant.data.db.MessageEntity
import ai.bondhu.assistant.data.db.ReplyLogEntity
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BondhuRepository @Inject constructor(private val dao: BondhuDao) {
    suspend fun action(event: ActionEvent): Long {
        val args = JSONObject().apply { event.args.forEach { (k, v) -> put(k, v) } }.toString()
        val result = JSONObject().apply {
            put("status", event.status)
            put("proof", event.proof)
            put("error", event.error ?: JSONObject.NULL)
        }.toString()
        return dao.insertAction(
            ActionLogEntity(
                ts = System.currentTimeMillis(),
                sessionId = SessionController.sessionId,
                tool = event.tool,
                argsJson = args,
                risk = event.risk.name,
                decision = event.decision.name,
                approvedBy = event.approvedBy,
                status = event.status,
                resultJson = result,
                proofRef = "db:action_log",
                latencyMs = event.latencyMs,
                errorCode = event.error,
            )
        )
    }

    suspend fun message(role: String, text: String) = dao.insertMessage(
        MessageEntity(
            ts = System.currentTimeMillis(),
            role = role,
            text = text,
            sessionId = SessionController.sessionId,
        )
    )

    suspend fun reply(said: String, latencyMs: Long) = dao.insertReply(
        ReplyLogEntity(
            ts = System.currentTimeMillis(),
            sessionId = SessionController.sessionId,
            said = said,
            latencyMs = latencyMs,
        )
    )

    suspend fun remember(key: String, value: String) = dao.insertMemory(
        MemoryEntity(key = key, value = value, ts = System.currentTimeMillis())
    )

    suspend fun messages() = dao.messages(50)
    suspend fun actions() = dao.actions(100)
    suspend fun memories() = dao.memories()
    suspend fun actionCount() = dao.actionCount()
}
