package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val role: String, // "USER" or "ADMIN"
    val accountNumber: String,
    val checkingBalance: Double,
    val savingsBalance: Double,
    val isFrozen: Boolean,
    val kycTier: String, // "STANDARD", "GOLD", "PLATINUM"
    val dailyAiSpendLimit: Double,
    val canAiTransfer: Boolean = true,
    val canAiBillPay: Boolean = true,
    val canAiVoice: Boolean = true,
    val canAiVault: Boolean = true,
    val createdAt: Long
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String, // "TRANSFER", "BILL_PAY", "DEPOSIT", "VAULT_MOVE"
    val title: String,
    val counterparty: String,
    val amount: Double, // negative = debit, positive = credit
    val category: String,
    val status: String, // "COMPLETED", "BLOCKED"
    val initiatedBy: String, // "AI_CHAT", "AI_VOICE", "MANUAL"
    val note: String,
    val timestamp: Long
)

@Entity(tableName = "ai_policies")
data class AiPolicyEntity(
    @PrimaryKey val id: String = "global_policy",
    val aiAssistantEnabled: Boolean = true,
    val allowMoneyTransfer: Boolean = true,
    val allowBillPay: Boolean = true,
    val allowAccountManagement: Boolean = true,
    val allowCustomerSupport: Boolean = true,
    val allowVoiceCommands: Boolean = true,
    val maxAiTransactionAmount: Double = 2500.0,
    val requireConfirmationAbove: Double = 500.0,
    val allowedFieldsCsv: String = "recipient,amount,billType,accountAction,cardFreeze,supportQuery",
    val customOperationalRule: String = "Always verify available checking balance before debiting. Escalate high-value transfers for one-tap confirmation.",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_messages")
data class AiChatMessageEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sender: String, // "USER", "AI"
    val text: String,
    val inputMode: String, // "TEXT", "VOICE"
    val actionBadge: String? = null,
    val pendingActionType: String? = null, // "TRANSFER", "BILL_PAY", "VAULT_MOVE"
    val pendingTarget: String? = null,
    val pendingAmount: Double? = null,
    val pendingResolved: Boolean = false,
    val timestamp: Long
)

@Entity(tableName = "audit_logs")
data class AdminAuditLogEntity(
    @PrimaryKey val id: String,
    val actorName: String,
    val action: String,
    val detail: String,
    val timestamp: Long
)
