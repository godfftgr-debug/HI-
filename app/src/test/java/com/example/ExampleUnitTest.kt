package com.example

import com.example.data.ai.PlaceholderBankingAiRepository
import com.example.data.local.AdminAuditLogEntity
import com.example.data.local.AiChatMessageEntity
import com.example.data.local.AiPolicyEntity
import com.example.data.local.TpmnDao
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.data.repository.BankingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class InMemoryTpmnDao : TpmnDao {
  private val users = MutableStateFlow<List<UserEntity>>(emptyList())
  private val txs = MutableStateFlow<List<TransactionEntity>>(emptyList())
  private val policy = MutableStateFlow<AiPolicyEntity?>(null)
  private val messages = MutableStateFlow<List<AiChatMessageEntity>>(emptyList())
  private val logs = MutableStateFlow<List<AdminAuditLogEntity>>(emptyList())

  override suspend fun countUsers(): Int = users.value.size
  override suspend fun findUserByIdentifier(identifier: String): UserEntity? =
    users.value.firstOrNull { it.email.equals(identifier, true) || it.username.equals(identifier, true) }
  override suspend fun findRecipientByQuery(query: String): UserEntity? =
    users.value.firstOrNull { it.fullName.contains(query, true) || it.username.contains(query, true) }
  override fun observeUser(userId: String): Flow<UserEntity?> =
    users.map { list -> list.firstOrNull { it.id == userId } }
  override suspend fun getUserById(userId: String): UserEntity? =
    users.value.firstOrNull { it.id == userId }
  override fun observeAllUsers(): Flow<List<UserEntity>> = users
  override suspend fun insertUser(user: UserEntity) {
    users.value = users.value.filterNot { it.id == user.id } + user
  }
  override suspend fun insertUsers(users: List<UserEntity>) {
    users.forEach { insertUser(it) }
  }
  override suspend fun updateUser(user: UserEntity) = insertUser(user)
  override fun observeTransactions(userId: String): Flow<List<TransactionEntity>> =
    txs.map { list -> list.filter { it.userId == userId } }
  override fun observeAllTransactions(): Flow<List<TransactionEntity>> = txs
  override suspend fun insertTransaction(tx: TransactionEntity) {
    txs.value = listOf(tx) + txs.value
  }
  override suspend fun insertTransactions(txs: List<TransactionEntity>) {
    txs.forEach { insertTransaction(it) }
  }
  override fun observePolicy(): Flow<AiPolicyEntity?> = policy
  override suspend fun getPolicy(): AiPolicyEntity? = policy.value
  override suspend fun upsertPolicy(policy: AiPolicyEntity) { this.policy.value = policy }
  override fun observeMessages(userId: String): Flow<List<AiChatMessageEntity>> =
    messages.map { list -> list.filter { it.userId == userId } }
  override suspend fun insertMessage(msg: AiChatMessageEntity) {
    messages.value = messages.value + msg
  }
  override suspend fun markMessageResolved(messageId: String) {
    messages.value = messages.value.map { if (it.id == messageId) it.copy(pendingResolved = true) else it }
  }
  override suspend fun clearMessagesForUser(userId: String) {
    messages.value = messages.value.filterNot { it.userId == userId }
  }
  override fun observeAuditLogs(): Flow<List<AdminAuditLogEntity>> = logs
  override suspend fun insertAuditLog(log: AdminAuditLogEntity) {
    logs.value = listOf(log) + logs.value
  }
}

class ExampleUnitTest {

  @Test
  fun zero_otp_login_ai_transfer_and_admin_policy_work() = runTest {
    val dao = InMemoryTpmnDao()
    val repo = BankingRepository(dao)
    val ai = PlaceholderBankingAiRepository(repo)
    repo.ensureSeeded()

    val loginRes = repo.login("alex", "user123")
    assertTrue(loginRes.isSuccess)
    val alex = loginRes.getOrThrow()

    val policy = repo.getPolicy()
    val txRes = ai.processBankingCommand(alex, policy, emptyList(), "Send $120 to Sarah", isVoice = false)
    assertEquals("TRANSFER EXECUTED", txRes.actionBadge)
    assertEquals(alex.checkingBalance - 120.0, dao.getUserById(alex.id)!!.checkingBalance, 0.01)

    repo.adminUpdatePolicy(policy.copy(allowBillPay = false), "Admin")
    val blocked = ai.processBankingCommand(dao.getUserById(alex.id)!!, repo.getPolicy(), emptyList(), "Pay $65 electric bill", isVoice = false)
    assertEquals("BLOCKED BY POLICY", blocked.actionBadge)

    // Verify per-user permission revocation blocks AI transfer even if global policy allows it
    val alexAfter = dao.getUserById(alex.id)!!
    repo.adminUpdateUserPermissions(alexAfter.copy(canAiTransfer = false), "Admin")
    val userPermBlocked = ai.processBankingCommand(
      dao.getUserById(alex.id)!!,
      repo.getPolicy(),
      emptyList(),
      "Send $50 to Sarah",
      isVoice = false
    )
    assertEquals("BLOCKED BY POLICY", userPermBlocked.actionBadge)
  }
}
