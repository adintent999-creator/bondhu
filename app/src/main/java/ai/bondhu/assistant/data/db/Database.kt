package ai.bondhu.assistant.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val role: String,
    val text: String,
    val sessionId: String,
)

@Entity(tableName = "memory")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String,
    val ts: Long,
    val kind: String = "fact",
)

@Entity(tableName = "action_log")
data class ActionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val sessionId: String,
    val tool: String,
    val argsJson: String,
    val risk: String,
    val decision: String,
    val approvedBy: String,
    val status: String,
    val resultJson: String,
    val proofRef: String,
    val latencyMs: Long,
    val errorCode: String?,
)

@Entity(tableName = "reply_log")
data class ReplyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val sessionId: String,
    val said: String,
    val latencyMs: Long,
)

@Dao
interface BondhuDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(item: MessageEntity): Long

    @Query("SELECT * FROM messages ORDER BY id DESC LIMIT :limit")
    suspend fun messages(limit: Int = 50): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(item: MemoryEntity): Long

    @Query("SELECT * FROM memory ORDER BY id DESC")
    suspend fun memories(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(item: ActionLogEntity): Long

    @Query("SELECT * FROM action_log ORDER BY id DESC LIMIT :limit")
    suspend fun actions(limit: Int = 100): List<ActionLogEntity>

    @Query("SELECT COUNT(*) FROM action_log")
    suspend fun actionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReply(item: ReplyLogEntity): Long
}

@Database(
    entities = [MessageEntity::class, MemoryEntity::class, ActionLogEntity::class, ReplyLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class BondhuDatabase : RoomDatabase() {
    abstract fun dao(): BondhuDao
}
