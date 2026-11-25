package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.domain.AutomationSequence
import com.example.domain.AutomationStep
import com.example.domain.AutomationStepType
import com.example.domain.ChatMessage
import com.example.domain.MayaEmotion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "automation_sequences")
data class AutomationSequenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val stepsJson: String
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val isFromMaya: Boolean,
    val text: String,
    val emotionKey: String,
    val timestampMillis: Long,
    val actionSummary: String?,
    val actionSuccess: Boolean?
)

@Dao
interface AutomationSequenceDao {
    @Query("SELECT * FROM automation_sequences ORDER BY id ASC")
    fun observeAll(): Flow<List<AutomationSequenceEntity>>

    @Query("SELECT COUNT(*) FROM automation_sequences")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AutomationSequenceEntity): Long

    @Query("DELETE FROM automation_sequences WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestampMillis ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}

@Database(
    entities = [AutomationSequenceEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MayaDatabase : RoomDatabase() {
    abstract fun automationSequenceDao(): AutomationSequenceDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: MayaDatabase? = null

        fun getInstance(context: Context): MayaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MayaDatabase::class.java,
                    "maya_assistant_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

fun AutomationSequenceEntity.toDomain(): AutomationSequence {
    val steps = mutableListOf<AutomationStep>()
    val array = JSONArray(stepsJson)
    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val typeName = obj.optString("type", AutomationStepType.WAIT.name)
        val stepType = AutomationStepType.entries.firstOrNull { it.name == typeName }
            ?: AutomationStepType.WAIT
        steps.add(
            AutomationStep(
                type = stepType,
                param1 = obj.optString("param1", ""),
                param2 = obj.optString("param2", "")
            )
        )
    }
    return AutomationSequence(
        id = id,
        name = name,
        description = description,
        steps = steps
    )
}

fun AutomationSequence.toEntity(): AutomationSequenceEntity {
    val array = JSONArray()
    steps.forEach { step ->
        val obj = JSONObject()
        obj.put("type", step.type.name)
        obj.put("param1", step.param1)
        obj.put("param2", step.param2)
        array.put(obj)
    }
    return AutomationSequenceEntity(
        id = id,
        name = name,
        description = description,
        stepsJson = array.toString()
    )
}

fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    isFromMaya = isFromMaya,
    text = text,
    emotion = MayaEmotion.fromKey(emotionKey),
    timestampMillis = timestampMillis,
    actionSummary = actionSummary,
    actionSuccess = actionSuccess
)

fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = if (id > 0L) id else 0L,
    isFromMaya = isFromMaya,
    text = text,
    emotionKey = emotion.key,
    timestampMillis = timestampMillis,
    actionSummary = actionSummary,
    actionSuccess = actionSuccess
)

fun Flow<List<AutomationSequenceEntity>>.mapToDomainSequences(): Flow<List<AutomationSequence>> =
    map { list -> list.map { it.toDomain() } }

fun Flow<List<ChatMessageEntity>>.mapToDomainMessages(): Flow<List<ChatMessage>> =
    map { list -> list.map { it.toDomain() } }
