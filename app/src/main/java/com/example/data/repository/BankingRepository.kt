package com.example.data.repository

import com.example.data.local.AdminAuditLogEntity
import com.example.data.local.AiChatMessageEntity
import com.example.data.local.AiPolicyEntity
import com.example.data.local.TpmnDao
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID

object PasswordHasher {
    private const val SALT = "TPMN_BANK_VAULT_SALT_2026"

    fun hash(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$SALT:$password".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

sealed interface BankingOpResult {
    data class Success(val message: String, val referenceId: String) : BankingOpResult
    data class Failure(val reason: String) : BankingOpResult
}

class BankingRepository(private val dao: TpmnDao) {

    suspend fun ensureSeeded() {
        if (dao.countUsers() > 0) return

        val now = System.currentTimeMillis()
        val alex = UserEntity(
            id = "usr_alex_01",
            username = "alex",
            email = "alex@tpmnbank.com",
            passwordHash = PasswordHasher.hash("user123"),
            fullName = "Alex Morgan",
            role = "USER",
            accountNumber = "TPMN-8840-2910",
            checkingBalance = 14850.00,
            savingsBalance = 42300.00,
            isFrozen = false,
            kycTier = "PLATINUM",
            dailyAiSpendLimit = 3000.0,
            createdAt = now - 86_400_000L * 30
        )
        val sarah = UserEntity(
            id = "usr_sarah_02",
            username = "sarah",
            email = "sarah@tpmnbank.com",
            passwordHash = PasswordHasher.hash("sarah123"),
            fullName = "Sarah Khan",
            role = "USER",
            accountNumber = "TPMN-5521-9044",
            checkingBalance = 8420.00,
            savingsBalance = 19100.00,
            isFrozen = false,
            kycTier = "GOLD",
            dailyAiSpendLimit = 2000.0,
            createdAt = now - 86_400_000L * 20
        )
        val admin = UserEntity(
            id = "usr_admin_00",
            username = "admin",
            email = "admin@tpmnbank.com",
            passwordHash = PasswordHasher.hash("admin123"),
            fullName = "TPMN Chief Architect",
            role = "ADMIN",
            accountNumber = "TPMN-0001-ROOT",
            checkingBalance = 125000.00,
            savingsBalance = 500000.00,
            isFrozen = false,
            kycTier = "PLATINUM",
            dailyAiSpendLimit = 10000.0,
            createdAt = now - 86_400_000L * 90
        )
        dao.insertUsers(listOf(alex, sarah, admin))

        dao.upsertPolicy(
            AiPolicyEntity(
                id = "global_policy",
                aiAssistantEnabled = true,
                allowMoneyTransfer = true,
                allowBillPay = true,
                allowAccountManagement = true,
                allowCustomerSupport = true,
                allowVoiceCommands = true,
                maxAiTransactionAmount = 2500.0,
                requireConfirmationAbove = 500.0,
                allowedFieldsCsv = "recipient,amount,billType,accountAction,cardFreeze,supportQuery",
                customOperationalRule = "Verify checking balance before debit. Require confirmation above threshold.",
                updatedAt = now
            )
        )

        dao.insertTransactions(
            listOf(
                TransactionEntity(
                    id = "tx_seed_1",
                    userId = alex.id,
                    type = "DEPOSIT",
                    title = "Executive Payroll Direct",
                    counterparty = "TPMN Global Corp",
                    amount = 6400.00,
                    category = "Income",
                    status = "COMPLETED",
                    initiatedBy = "MANUAL",
                    note = "Monthly salary settlement",
                    timestamp = now - 86_400_000L * 3
                ),
                TransactionEntity(
                    id = "tx_seed_2",
                    userId = alex.id,
                    type = "BILL_PAY",
                    title = "DESCO Smart Grid Electricity",
                    counterparty = "DESCO Utility",
                    amount = -118.50,
                    category = "Utilities",
                    status = "COMPLETED",
                    initiatedBy = "AI_VOICE",
                    note = "Executed via voice instruction",
                    timestamp = now - 86_400_000L * 2
                ),
                TransactionEntity(
                    id = "tx_seed_3",
                    userId = alex.id,
                    type = "TRANSFER",
                    title = "Instant Transfer to Sarah Khan",
                    counterparty = "Sarah Khan",
                    amount = -240.00,
                    category = "Transfer",
                    status = "COMPLETED",
                    initiatedBy = "AI_CHAT",
                    note = "Dinner & shared travel split",
                    timestamp = now - 36_000_000L
                ),
                TransactionEntity(
                    id = "tx_seed_4",
                    userId = alex.id,
                    type = "VAULT_MOVE",
                    title = "Auto-Sweep to Prosperity Vault",
                    counterparty = "TPMN Savings Vault",
                    amount = -500.00,
                    category = "Vault",
                    status = "COMPLETED",
                    initiatedBy = "AI_CHAT",
                    note = "High-yield 6.2% APY allocation",
                    timestamp = now - 14_400_000L
                )
            )
        )

        dao.insertMessage(
            AiChatMessageEntity(
                id = "msg_welcome_alex",
                userId = alex.id,
                sender = "AI",
                text = "[Sample AI Mode] Welcome to TPMN BANK, Alex. I can execute transfers, pay utility bills, move funds to your Prosperity Vault, lock/unlock your card, or answer support questions via text or voice.",
                inputMode = "TEXT",
                actionBadge = "AI BANKER READY",
                timestamp = now - 7_200_000L
            )
        )

        dao.insertAuditLog(
            AdminAuditLogEntity(
                id = "log_seed_1",
                actorName = "System Bootstrap",
                action = "POLICY_INITIALIZED",
                detail = "Global AI guardrail active: Max TX $2,500 • Confirmation > $500",
                timestamp = now - 86_400_000L
            )
        )
    }

    suspend fun login(identifier: String, password: String): Result<UserEntity> {
        val cleanId = identifier.trim()
        if (cleanId.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your email/username and password."))
        }
        val user = dao.findUserByIdentifier(cleanId)
            ?: return Result.failure(IllegalArgumentException("No TPMN BANK account found for '$cleanId'."))
        if (user.passwordHash != PasswordHasher.hash(password)) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }
        dao.insertAuditLog(
            AdminAuditLogEntity(
                id = "log_${UUID.randomUUID().toString().take(8)}",
                actorName = user.fullName,
                action = "USER_LOGIN",
                detail = "Zero-OTP password sign-in (${user.role})",
                timestamp = System.currentTimeMillis()
            )
        )
        return Result.success(user)
    }

    suspend fun register(
        fullName: String,
        username: String,
        email: String,
        password: String
    ): Result<UserEntity> {
        val cleanName = fullName.trim()
        val cleanUser = username.trim().lowercase().replace(" ", "")
        val cleanEmail = email.trim().lowercase()

        if (cleanName.length < 2) return Result.failure(IllegalArgumentException("Full name is required."))
        if (cleanUser.length < 3) return Result.failure(IllegalArgumentException("Username must be at least 3 characters."))
        if (!cleanEmail.contains("@")) return Result.failure(IllegalArgumentException("Enter a valid email address."))
        if (password.length < 4) return Result.failure(IllegalArgumentException("Password must be at least 4 characters."))

        if (dao.findUserByIdentifier(cleanEmail) != null || dao.findUserByIdentifier(cleanUser) != null) {
            return Result.failure(IllegalArgumentException("An account with that email or username already exists."))
        }

        val now = System.currentTimeMillis()
        val suffix = (1000..9999).random()
        val suffix2 = (1000..9999).random()
        val newUser = UserEntity(
            id = "usr_${UUID.randomUUID().toString().take(8)}",
            username = cleanUser,
            email = cleanEmail,
            passwordHash = PasswordHasher.hash(password),
            fullName = cleanName,
            role = "USER",
            accountNumber = "TPMN-$suffix-$suffix2",
            checkingBalance = 2500.00,
            savingsBalance = 1000.00,
            isFrozen = false,
            kycTier = "GOLD",
            dailyAiSpendLimit = 2500.0,
            createdAt = now
        )
        dao.insertUser(newUser)
        dao.insertTransaction(
            TransactionEntity(
                id = "tx_${UUID.randomUUID().toString().take(8)}",
                userId = newUser.id,
                type = "DEPOSIT",
                title = "Welcome Bonus Settlement",
                counterparty = "TPMN BANK Treasury",
                amount = 2500.00,
                category = "Income",
                status = "COMPLETED",
                initiatedBy = "MANUAL",
                note = "Instant account opening credit",
                timestamp = now
            )
        )
        dao.insertMessage(
            AiChatMessageEntity(
                id = "msg_${UUID.randomUUID().toString().take(8)}",
                userId = newUser.id,
                sender = "AI",
                text = "[Sample AI Mode] Welcome to TPMN BANK, $cleanName! Your vault (${newUser.accountNumber}) is active with $2,500.00 checking balance.",
                inputMode = "TEXT",
                actionBadge = "ACCOUNT ACTIVATED",
                timestamp = now
            )
        )
        return Result.success(newUser)
    }

    fun observeUser(userId: String): Flow<UserEntity?> = dao.observeUser(userId)

    fun observeAllUsers(): Flow<List<UserEntity>> = dao.observeAllUsers()

    fun observeTransactions(userId: String): Flow<List<TransactionEntity>> = dao.observeTransactions(userId)

    fun observeAllTransactions(): Flow<List<TransactionEntity>> = dao.observeAllTransactions()

    fun observePolicy(): Flow<AiPolicyEntity> = dao.observePolicy().map { it ?: AiPolicyEntity() }

    fun observeMessages(userId: String): Flow<List<AiChatMessageEntity>> = dao.observeMessages(userId)

    fun observeAuditLogs(): Flow<List<AdminAuditLogEntity>> = dao.observeAuditLogs()

    suspend fun getPolicy(): AiPolicyEntity = dao.getPolicy() ?: AiPolicyEntity()

    suspend fun saveMessage(msg: AiChatMessageEntity) = dao.insertMessage(msg)

    suspend fun markPendingMessageResolved(messageId: String) = dao.markMessageResolved(messageId)

    suspend fun clearChatHistory(userId: String) = dao.clearMessagesForUser(userId)

    suspend fun executeTransfer(
        userId: String,
        recipientQuery: String,
        amount: Double,
        initiatedBy: String,
        note: String = "AI instant transfer"
    ): BankingOpResult {
        if (amount <= 0) return BankingOpResult.Failure("Amount must be greater than zero.")
        val sender = dao.getUserById(userId) ?: return BankingOpResult.Failure("Sender account not found.")
        if (sender.isFrozen) return BankingOpResult.Failure("Your account is currently frozen by Admin.")
        if (sender.checkingBalance < amount) {
            return BankingOpResult.Failure(
                "Insufficient checking funds. Available: $${"%.2f".format(sender.checkingBalance)}."
            )
        }

        val recipient = dao.findRecipientByQuery(recipientQuery)
        val recipientDisplay = recipient?.fullName ?: recipientQuery.replaceFirstChar { it.uppercase() }
        val now = System.currentTimeMillis()
        val ref = "TPMN-${UUID.randomUUID().toString().take(6).uppercase()}"

        dao.updateUser(sender.copy(checkingBalance = sender.checkingBalance - amount))
        if (recipient != null && recipient.id != sender.id) {
            dao.updateUser(recipient.copy(checkingBalance = recipient.checkingBalance + amount))
            dao.insertTransaction(
                TransactionEntity(
                    id = "tx_${UUID.randomUUID().toString().take(8)}",
                    userId = recipient.id,
                    type = "DEPOSIT",
                    title = "Transfer from ${sender.fullName}",
                    counterparty = sender.fullName,
                    amount = amount,
                    category = "Transfer",
                    status = "COMPLETED",
                    initiatedBy = initiatedBy,
                    note = "Ref: $ref",
                    timestamp = now
                )
            )
        }

        dao.insertTransaction(
            TransactionEntity(
                id = "tx_${UUID.randomUUID().toString().take(8)}",
                userId = sender.id,
                type = "TRANSFER",
                title = "Transfer to $recipientDisplay",
                counterparty = recipientDisplay,
                amount = -amount,
                category = "Transfer",
                status = "COMPLETED",
                initiatedBy = initiatedBy,
                note = "$note • Ref: $ref",
                timestamp = now
            )
        )
        dao.insertAuditLog(
            AdminAuditLogEntity(
                id = "log_${UUID.randomUUID().toString().take(8)}",
                actorName = sender.fullName,
                action = "TRANSFER_$initiatedBy",
                detail = "Sent $${"%.2f".format(amount)} to $recipientDisplay ($ref)",
                timestamp = now
            )
        )
        return BankingOpResult.Success("Transferred $${"%.2f".format(amount)} to $recipientDisplay.", ref)
    }

    suspend fun executeBillPay(
        userId: String,
        billerName: String,
        category: String,
        amount: Double,
        initiatedBy: String
    ): BankingOpResult {
        if (amount <= 0) return BankingOpResult.Failure("Bill amount must be positive.")
        val user = dao.getUserById(userId) ?: return BankingOpResult.Failure("Account not found.")
        if (user.isFrozen) return BankingOpResult.Failure("Account is frozen. Contact Admin Control Hub.")
        if (user.checkingBalance < amount) {
            return BankingOpResult.Failure(
                "Insufficient checking balance ($${"%.2f".format(user.checkingBalance)}) to pay $${"%.2f".format(amount)}."
            )
        }

        val now = System.currentTimeMillis()
        val ref = "BILL-${UUID.randomUUID().toString().take(6).uppercase()}"
        dao.updateUser(user.copy(checkingBalance = user.checkingBalance - amount))
        dao.insertTransaction(
            TransactionEntity(
                id = "tx_${UUID.randomUUID().toString().take(8)}",
                userId = user.id,
                type = "BILL_PAY",
                title = "$billerName Bill Payment",
                counterparty = billerName,
                amount = -amount,
                category = category,
                status = "COMPLETED",
                initiatedBy = initiatedBy,
                note = "Instant bill settlement • Ref: $ref",
                timestamp = now
            )
        )
        dao.insertAuditLog(
            AdminAuditLogEntity(
                id = "log_${UUID.randomUUID().toString().take(8)}",
                actorName = user.fullName,
                action = "BILL_PAY_$initiatedBy",
                detail = "Paid $${"%.2f".format(amount)} to $billerName ($ref)",
                timestamp = now
            )
        )
        return BankingOpResult.Success("Paid $${"%.2f".format(amount)} to $billerName.", ref)
    }

    suspend fun moveToSavingsVault(
        userId: String,
        amount: Double,
        initiatedBy: String
    ): BankingOpResult {
        if (amount <= 0) return BankingOpResult.Failure("Vault sweep amount must be positive.")
        val user = dao.getUserById(userId) ?: return BankingOpResult.Failure("Account not found.")
        if (user.isFrozen) return BankingOpResult.Failure("Account is frozen.")
        if (user.checkingBalance < amount) {
            return BankingOpResult.Failure("Insufficient checking balance for vault sweep.")
        }

        val now = System.currentTimeMillis()
        val ref = "VLT-${UUID.randomUUID().toString().take(6).uppercase()}"
        dao.updateUser(
            user.copy(
                checkingBalance = user.checkingBalance - amount,
                savingsBalance = user.savingsBalance + amount
            )
        )
        dao.insertTransaction(
            TransactionEntity(
                id = "tx_${UUID.randomUUID().toString().take(8)}",
                userId = user.id,
                type = "VAULT_MOVE",
                title = "Sweep to Prosperity Vault",
                counterparty = "TPMN Savings Vault",
                amount = -amount,
                category = "Vault",
                status = "COMPLETED",
                initiatedBy = initiatedBy,
                note = "Moved to savings • Ref: $ref",
                timestamp = now
            )
        )
        return BankingOpResult.Success("Moved $${"%.2f".format(amount)} to Prosperity Vault.", ref)
    }

    suspend fun setUserFrozen(
        targetUserId: String,
        freeze: Boolean,
        actorName: String
    ): BankingOpResult {
        val user = dao.getUserById(targetUserId) ?: return BankingOpResult.Failure("User not found.")
        dao.updateUser(user.copy(isFrozen = freeze))
        dao.insertAuditLog(
            AdminAuditLogEntity(
                id = "log_${UUID.randomUUID().toString().take(8)}",
                actorName = actorName,
                action = if (freeze) "ACCOUNT_FROZEN" else "ACCOUNT_UNFROZEN",
                detail = "${user.fullName} (${user.accountNumber})",
                timestamp = System.currentTimeMillis()
            )
        )
        val verb = if (freeze) "locked/frozen" else "activated/unlocked"
        return BankingOpResult.Success("Account ${user.accountNumber} has been $verb.", "SEC-${targetUserId.takeLast(4)}")
    }

    suspend fun adminAdjustBalance(
        targetUserId: String,
        deltaChecking: Double,
        adminName: String
    ): BankingOpResult {
        val user = dao.getUserById(targetUserId) ?: return BankingOpResult.Failure("User not found.")
        val updated = (user.checkingBalance + deltaChecking).coerceAtLeast(0.0)
        dao.updateUser(user.copy(checkingBalance = updated))
        val now = System.currentTimeMillis()
        dao.insertTransaction(
            TransactionEntity(
                id = "tx_${UUID.randomUUID().toString().take(8)}",
                userId = user.id,
                type = "DEPOSIT",
                title = if (deltaChecking >= 0) "Admin Treasury Credit" else "Admin Treasury Debit",
                counterparty = "TPMN Control Hub",
                amount = deltaChecking,
                category = "Treasury",
                status = "COMPLETED",
                initiatedBy = "ADMIN",
                note = "Supervisor adjustment by $adminName",
                timestamp = now
            )
        )
        dao.insertAuditLog(
            AdminAuditLogEntity(
                id = "log_${UUID.randomUUID().toString().take(8)}",
                actorName = adminName,
                action = "ADMIN_BALANCE_ADJUST",
                detail = "${user.fullName}: ${if (deltaChecking >= 0) "+" else ""}${"%.2f".format(deltaChecking)}",
                timestamp = now
            )
        )
        return BankingOpResult.Success("Updated ${user.fullName}'s checking balance to $${"%.2f".format(updated)}.", "ADM-ADJ")
    }

    suspend fun adminCycleKycTier(targetUserId: String, adminName: String) {
        val user = dao.getUserById(targetUserId) ?: return
        val nextTier = when (user.kycTier) {
            "STANDARD" -> "GOLD"
            "GOLD" -> "PLATINUM"
            else -> "STANDARD"
        }
        dao.updateUser(user.copy(kycTier = nextTier))
        dao.insertAuditLog(
            AdminAuditLogEntity(
                id = "log_${UUID.randomUUID().toString().take(8)}",
                actorName = adminName,
                action = "KYC_TIER_UPDATED",
                detail = "${user.fullName} -> $nextTier",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun adminUpdateUserPermissions(updatedUser: UserEntity, adminName: String): BankingOpResult {
        val existing = dao.getUserById(updatedUser.id) ?: return BankingOpResult.Failure("User not found.")
        dao.updateUser(updatedUser)
        dao.insertAuditLog(
            AdminAuditLogEntity(
                id = "log_${UUID.randomUUID().toString().take(8)}",
                actorName = adminName,
                action = "USER_PERMISSIONS_UPDATED",
                detail = "${existing.fullName}: role=${updatedUser.role}, frozen=${updatedUser.isFrozen}, transfer=${updatedUser.canAiTransfer}, bill=${updatedUser.canAiBillPay}, voice=${updatedUser.canAiVoice}, vault=${updatedUser.canAiVault}, limit=$${updatedUser.dailyAiSpendLimit.toInt()}",
                timestamp = System.currentTimeMillis()
            )
        )
        return BankingOpResult.Success(
            "Updated account permissions for ${updatedUser.fullName}.",
            "PERM-${updatedUser.id.takeLast(4).uppercase()}"
        )
    }

    suspend fun adminUpdatePolicy(policy: AiPolicyEntity, adminName: String) {
        val updated = policy.copy(updatedAt = System.currentTimeMillis())
        dao.upsertPolicy(updated)
        dao.insertAuditLog(
            AdminAuditLogEntity(
                id = "log_${UUID.randomUUID().toString().take(8)}",
                actorName = adminName,
                action = "AI_POLICY_UPDATED",
                detail = "AI=${updated.aiAssistantEnabled}, Transfer=${updated.allowMoneyTransfer}, Bill=${updated.allowBillPay}, Max=$${updated.maxAiTransactionAmount.toInt()}",
                timestamp = updated.updatedAt
            )
        )
    }
}
