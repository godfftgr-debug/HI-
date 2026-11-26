package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TpmnDao {

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Query("SELECT * FROM users WHERE (LOWER(email) = LOWER(:identifier) OR LOWER(username) = LOWER(:identifier)) LIMIT 1")
    suspend fun findUserByIdentifier(identifier: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(fullName) LIKE '%' || LOWER(:query) || '%' OR LOWER(username) LIKE '%' || LOWER(:query) || '%' LIMIT 1")
    suspend fun findRecipientByQuery(query: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY role ASC, createdAt ASC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun observeTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 50")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(txs: List<TransactionEntity>)

    @Query("SELECT * FROM ai_policies WHERE id = 'global_policy' LIMIT 1")
    fun observePolicy(): Flow<AiPolicyEntity?>

    @Query("SELECT * FROM ai_policies WHERE id = 'global_policy' LIMIT 1")
    suspend fun getPolicy(): AiPolicyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPolicy(policy: AiPolicyEntity)

    @Query("SELECT * FROM ai_messages WHERE userId = :userId ORDER BY timestamp ASC")
    fun observeMessages(userId: String): Flow<List<AiChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: AiChatMessageEntity)

    @Query("UPDATE ai_messages SET pendingResolved = 1 WHERE id = :messageId")
    suspend fun markMessageResolved(messageId: String)

    @Query("DELETE FROM ai_messages WHERE userId = :userId")
    suspend fun clearMessagesForUser(userId: String)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 40")
    fun observeAuditLogs(): Flow<List<AdminAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AdminAuditLogEntity)
}
