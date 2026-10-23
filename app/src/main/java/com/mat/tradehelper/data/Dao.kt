package com.mat.tradehelper.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao interface TradeDao {
 @Query("SELECT * FROM trades ORDER BY createdAt DESC") fun all():Flow<List<Trade>>
 @Query("SELECT * FROM trades ORDER BY createdAt ASC") suspend fun snapshot():List<Trade>
 @Query("SELECT COUNT(*) FROM trades") suspend fun count():Int
 @Insert suspend fun add(x:Trade):Long
 @Insert suspend fun addAll(items:List<Trade>)
 @Delete suspend fun delete(x:Trade)
 @Query("DELETE FROM trades") suspend fun clear()
}
@Dao interface KnowledgeDao {
 @Query("SELECT * FROM knowledge ORDER BY createdAt DESC") fun all():Flow<List<Knowledge>>
 @Query("SELECT * FROM knowledge ORDER BY createdAt ASC") suspend fun snapshot():List<Knowledge>
 @Query("SELECT COUNT(*) FROM knowledge") suspend fun count():Int
 @Insert suspend fun add(x:Knowledge):Long
 @Insert suspend fun addAll(items:List<Knowledge>)
 @Delete suspend fun delete(x:Knowledge)
 @Query("DELETE FROM knowledge") suspend fun clear()
}
@Dao interface AuditDao {
 @Query("SELECT * FROM audit_log ORDER BY createdAt DESC") fun all():Flow<List<AuditLog>>
 @Query("SELECT COUNT(*) FROM audit_log") suspend fun count():Int
 @Insert suspend fun add(x:AuditLog)
 @Query("DELETE FROM audit_log") suspend fun clear()
}
