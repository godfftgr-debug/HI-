package com.example.data.ai

import com.example.data.local.AiPolicyEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.data.repository.BankingOpResult
import com.example.data.repository.BankingRepository
import kotlinx.coroutines.delay

data class PendingAiAction(
    val actionType: String, // "TRANSFER", "BILL_PAY", "VAULT_MOVE"
    val target: String,
    val amount: Double,
    val category: String
)

data class AiCommandResult(
    val replyText: String,
    val actionBadge: String?,
    val pendingAction: PendingAiAction? = null,
    val followUpSuggestions: List<String> = emptyList()
)

interface BankingAiRepository {
    suspend fun processBankingCommand(
        user: UserEntity,
        policy: AiPolicyEntity,
        recentTransactions: List<TransactionEntity>,
        userInput: String,
        isVoice: Boolean
    ): AiCommandResult

    suspend fun executeConfirmedAction(
        user: UserEntity,
        policy: AiPolicyEntity,
        pending: PendingAiAction,
        isVoice: Boolean
    ): AiCommandResult
}

/**
 * Sample / Placeholder implementation used when Firebase AI Logic is not enabled.
 * Parses natural-language banking intents locally, enforces live Admin Control Hub policies,
 * and executes real Room database transactions while clearly labeling responses as Sample AI Mode.
 */
class PlaceholderBankingAiRepository(
    private val bankingRepository: BankingRepository
) : BankingAiRepository {

    override suspend fun processBankingCommand(
        user: UserEntity,
        policy: AiPolicyEntity,
        recentTransactions: List<TransactionEntity>,
        userInput: String,
        isVoice: Boolean
    ): AiCommandResult {
        delay(350) // Simulate realistic async inference latency

        val initiatedBy = if (isVoice) "AI_VOICE" else "AI_CHAT"
        val lower = userInput.lowercase().trim()
        val allowedFields = policy.allowedFieldsCsv.split(",").map { it.trim() }.toSet()

        if (!policy.aiAssistantEnabled) {
            return AiCommandResult(
                replyText = "[Sample AI Mode] TPMN AI Assistant is currently paused by the Admin Control Hub. Ask your administrator to re-enable AI operations.",
                actionBadge = "PAUSED BY ADMIN",
                followUpSuggestions = listOf("Check policy status")
            )
        }

        if (isVoice && (!policy.allowVoiceCommands || !user.canAiVoice)) {
            return AiCommandResult(
                replyText = "[Sample AI Mode] Voice banking commands are currently disabled for your account or by the Admin Control Hub.",
                actionBadge = "VOICE RESTRICTED",
                followUpSuggestions = listOf("Show my balance", "Recent transactions")
            )
        }

        val amount = extractAmount(lower)

        // 1. Card Freeze / Unfreeze Intent
        if (containsAny(lower, "freeze", "lock", "block card", "লক", "ফ্রিজ", "বন্ধ")) {
            if (!policy.allowAccountManagement || "cardFreeze" !in allowedFields) {
                return blocked("Account Management / Card Freeze is disabled by Admin Control Hub.")
            }
            val shouldFreeze = !containsAny(lower, "unfreeze", "unlock", "activate", "আনলক", "খুল")
            return when (val res = bankingRepository.setUserFrozen(user.id, shouldFreeze, "TPMN AI (${user.fullName})")) {
                is BankingOpResult.Success -> AiCommandResult(
                    replyText = "[Sample AI Mode] ${res.message} Ref: ${res.referenceId}.",
                    actionBadge = if (shouldFreeze) "CARD FROZEN" else "CARD ACTIVE",
                    followUpSuggestions = listOf("Check my balance", if (shouldFreeze) "Unlock my card" else "Freeze my card")
                )
                is BankingOpResult.Failure -> AiCommandResult(
                    replyText = "[Sample AI Mode] Could not update card status: ${res.reason}",
                    actionBadge = "ACTION FAILED"
                )
            }
        }

        // 2. Balance / Summary / Transactions Intent
        if (containsAny(lower, "balance", "how much", "summary", "spending", "statement", "ব্যালেন্স", "টাকা আছে", "হিসাব")) {
            val totalSpend = recentTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
            val recentSummary = recentTransactions.take(3).joinToString("\n") { tx ->
                val sign = if (tx.amount >= 0) "+" else "-"
                "• ${tx.title}: $sign$${"%.2f".format(kotlin.math.abs(tx.amount))} (${tx.initiatedBy})"
            }.ifBlank { "• No recent transactions yet." }

            return AiCommandResult(
                replyText = buildString {
                    appendLine("[Sample AI Mode] Account Snapshot for ${user.fullName} (${user.accountNumber}):")
                    appendLine("• Checking: $${"%.2f".format(user.checkingBalance)}")
                    appendLine("• Prosperity Vault (Savings): $${"%.2f".format(user.savingsBalance)}")
                    appendLine("• Total Outflow (${recentTransactions.size} entries): $${"%.2f".format(totalSpend)}")
                    appendLine("Recent Activity:")
                    append(recentSummary)
                },
                actionBadge = "BALANCE SYNCED",
                followUpSuggestions = listOf("Move $200 to vault", "Send $80 to Sarah", "Pay $65 electric bill")
            )
        }

        // 3. Vault / Savings Sweep Intent
        if (containsAny(lower, "vault", "savings", "save ", "sweep", "সেভিংস", "ভল্ট")) {
            if (!policy.allowAccountManagement || !user.canAiVault || "accountAction" !in allowedFields) {
                return blocked("Vault Sweep permission is disabled for this account or by the Admin Control Hub.")
            }
            val sweepAmount = amount ?: 250.0
            val effectiveCap = minOf(policy.maxAiTransactionAmount, user.dailyAiSpendLimit)
            if (sweepAmount > effectiveCap) {
                return blocked("Requested $${"%.2f".format(sweepAmount)} exceeds effective AI limit ($${"%.2f".format(effectiveCap)}).")
            }
            if (sweepAmount > policy.requireConfirmationAbove) {
                return AiCommandResult(
                    replyText = "[Sample AI Mode] Admin guardrail requires confirmation for transactions above $${"%.2f".format(policy.requireConfirmationAbove)}. Confirm moving $${"%.2f".format(sweepAmount)} to your Prosperity Vault?",
                    actionBadge = "CONFIRMATION REQUIRED",
                    pendingAction = PendingAiAction("VAULT_MOVE", "TPMN Savings Vault", sweepAmount, "Vault")
                )
            }
            return when (val res = bankingRepository.moveToSavingsVault(user.id, sweepAmount, initiatedBy)) {
                is BankingOpResult.Success -> AiCommandResult(
                    replyText = "[Sample AI Mode] ${res.message} Ref: ${res.referenceId}.",
                    actionBadge = "VAULT SWEEP EXECUTED",
                    followUpSuggestions = listOf("Check my balance")
                )
                is BankingOpResult.Failure -> AiCommandResult(
                    replyText = "[Sample AI Mode] Vault sweep declined: ${res.reason}",
                    actionBadge = "DECLINED"
                )
            }
        }

        // 4. Bill Payment Intent
        if (containsAny(lower, "bill", "electric", "water", "gas", "internet", "wifi", "recharge", "desco", "বিল", "বিদ্যুৎ", "ইন্টারনেট")) {
            if (!policy.allowBillPay || !user.canAiBillPay || "billType" !in allowedFields || "amount" !in allowedFields) {
                return blocked("AI Bill Payment permission is disabled for this account or by the Admin Control Hub.")
            }
            val billAmount = amount ?: 75.0
            val (biller, category) = detectBiller(lower)
            val effectiveCap = minOf(policy.maxAiTransactionAmount, user.dailyAiSpendLimit)
            if (billAmount > effectiveCap) {
                return blocked("Bill amount $${"%.2f".format(billAmount)} exceeds effective AI limit ($${"%.2f".format(effectiveCap)}).")
            }
            if (billAmount > policy.requireConfirmationAbove) {
                return AiCommandResult(
                    replyText = "[Sample AI Mode] Please confirm paying $${"%.2f".format(billAmount)} to $biller (above $${"%.2f".format(policy.requireConfirmationAbove)} threshold).",
                    actionBadge = "CONFIRMATION REQUIRED",
                    pendingAction = PendingAiAction("BILL_PAY", biller, billAmount, category)
                )
            }
            return when (val res = bankingRepository.executeBillPay(user.id, biller, category, billAmount, initiatedBy)) {
                is BankingOpResult.Success -> AiCommandResult(
                    replyText = "[Sample AI Mode] ${res.message} Ref: ${res.referenceId}.",
                    actionBadge = "BILL PAID",
                    followUpSuggestions = listOf("Check my balance", "Pay $45 internet bill")
                )
                is BankingOpResult.Failure -> AiCommandResult(
                    replyText = "[Sample AI Mode] Bill payment failed: ${res.reason}",
                    actionBadge = "DECLINED"
                )
            }
        }

        // 5. Money Transfer Intent
        if (containsAny(lower, "send", "transfer", "pay ", "give", "পাঠাও", "ট্রান্সফার")) {
            if (!policy.allowMoneyTransfer || !user.canAiTransfer || "recipient" !in allowedFields || "amount" !in allowedFields) {
                return blocked("AI Money Transfer permission is disabled for this account or by the Admin Control Hub.")
            }
            val transferAmount = amount ?: return AiCommandResult(
                replyText = "[Sample AI Mode] Please include an amount for the transfer, e.g. \"Send $120 to Sarah\".",
                actionBadge = "NEED AMOUNT",
                followUpSuggestions = listOf("Send $120 to Sarah", "Send $50 to Alex")
            )
            val effectiveCap = minOf(policy.maxAiTransactionAmount, user.dailyAiSpendLimit)
            if (transferAmount > effectiveCap) {
                return blocked("Transfer of $${"%.2f".format(transferAmount)} exceeds effective AI cap of $${"%.2f".format(effectiveCap)}.")
            }
            val recipient = extractRecipient(userInput)
            if (transferAmount > policy.requireConfirmationAbove) {
                return AiCommandResult(
                    replyText = "[Sample AI Mode] Security guardrail: Confirm sending $${"%.2f".format(transferAmount)} to $recipient (exceeds $${"%.2f".format(policy.requireConfirmationAbove)} auto-approval limit).",
                    actionBadge = "CONFIRMATION REQUIRED",
                    pendingAction = PendingAiAction("TRANSFER", recipient, transferAmount, "Transfer")
                )
            }
            return when (val res = bankingRepository.executeTransfer(user.id, recipient, transferAmount, initiatedBy)) {
                is BankingOpResult.Success -> AiCommandResult(
                    replyText = "[Sample AI Mode] ${res.message} Ref: ${res.referenceId}.",
                    actionBadge = "TRANSFER EXECUTED",
                    followUpSuggestions = listOf("Check my balance", "Move $150 to vault")
                )
                is BankingOpResult.Failure -> AiCommandResult(
                    replyText = "[Sample AI Mode] Transfer declined: ${res.reason}",
                    actionBadge = "DECLINED"
                )
            }
        }

        // 6. Customer Support / Policy Inquiry Intent
        if (!policy.allowCustomerSupport || "supportQuery" !in allowedFields) {
            return blocked("AI Customer Support is currently turned off by Admin Control Hub.")
        }

        return AiCommandResult(
            replyText = buildString {
                appendLine("[Sample AI Mode] TPMN Support Desk:")
                appendLine("I understood: \"$userInput\".")
                appendLine("Active Admin Guardrails: Max AI TX $${"%.0f".format(policy.maxAiTransactionAmount)} • Confirm > $${"%.0f".format(policy.requireConfirmationAbove)}.")
                append("Try commands like \"Send $120 to Sarah\", \"Pay $65 electric bill\", \"Move $250 to vault\", \"Freeze my card\", or \"Check my balance\".")
            },
            actionBadge = "SUPPORT ASSIST",
            followUpSuggestions = listOf("Check my balance", "Send $120 to Sarah", "Pay $65 electric bill", "Freeze my card")
        )
    }

    override suspend fun executeConfirmedAction(
        user: UserEntity,
        policy: AiPolicyEntity,
        pending: PendingAiAction,
        isVoice: Boolean
    ): AiCommandResult {
        val initiatedBy = if (isVoice) "AI_VOICE" else "AI_CHAT"
        if (!policy.aiAssistantEnabled) return blocked("AI Assistant was disabled by Admin.")
        if (pending.amount > policy.maxAiTransactionAmount) {
            return blocked("Amount exceeds updated Admin AI transaction limit.")
        }

        val result = when (pending.actionType) {
            "TRANSFER" -> {
                if (!policy.allowMoneyTransfer) return blocked("Money Transfer was disabled by Admin.")
                bankingRepository.executeTransfer(user.id, pending.target, pending.amount, initiatedBy, "Confirmed AI transfer")
            }
            "BILL_PAY" -> {
                if (!policy.allowBillPay) return blocked("Bill Pay was disabled by Admin.")
                bankingRepository.executeBillPay(user.id, pending.target, pending.category, pending.amount, initiatedBy)
            }
            "VAULT_MOVE" -> {
                if (!policy.allowAccountManagement) return blocked("Account Management was disabled by Admin.")
                bankingRepository.moveToSavingsVault(user.id, pending.amount, initiatedBy)
            }
            else -> BankingOpResult.Failure("Unknown action type.")
        }

        return when (result) {
            is BankingOpResult.Success -> AiCommandResult(
                replyText = "[Sample AI Mode] Confirmed and executed: ${result.message} Ref: ${result.referenceId}.",
                actionBadge = "${pending.actionType} CONFIRMED",
                followUpSuggestions = listOf("Check my balance")
            )
            is BankingOpResult.Failure -> AiCommandResult(
                replyText = "[Sample AI Mode] Execution failed: ${result.reason}",
                actionBadge = "DECLINED"
            )
        }
    }

    private fun blocked(reason: String) = AiCommandResult(
        replyText = "[Sample AI Mode] Blocked by Admin Control Hub: $reason",
        actionBadge = "BLOCKED BY POLICY",
        followUpSuggestions = listOf("Check my balance")
    )

    private fun containsAny(text: String, vararg keywords: String): Boolean =
        keywords.any { text.contains(it) }

    private fun extractAmount(text: String): Double? {
        val regex = Regex("""\$?\s*(\d+(?:\.\d{1,2})?)""")
        return regex.find(text)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
    }

    private fun detectBiller(text: String): Pair<String, String> = when {
        containsAny(text, "electric", "desco", "power", "বিদ্যুৎ") -> "DESCO Smart Grid" to "Utilities"
        containsAny(text, "internet", "wifi", "fiber", "ইন্টারনেট") -> "TPMN FiberNet" to "Telecom"
        containsAny(text, "water", "wasa") -> "Metro Water Utility" to "Utilities"
        containsAny(text, "gas", "titas") -> "City Gas Grid" to "Utilities"
        containsAny(text, "mobile", "recharge", "phone") -> "Global Mobile Top-Up" to "Telecom"
        else -> "TPMN Utility Partner" to "Utilities"
    }

    private fun extractRecipient(original: String): String {
        val toMatch = Regex("""(?:to|for|কে)\s+([A-Za-z][A-Za-z .'-]{1,28})""", RegexOption.IGNORE_CASE)
            .find(original)
        val raw = toMatch?.groupValues?.getOrNull(1)?.trim()
        if (!raw.isNullOrBlank()) {
            return raw.split(" ").take(2).joinToString(" ")
        }
        return if (original.contains("sarah", ignoreCase = true)) "Sarah Khan"
        else if (original.contains("alex", ignoreCase = true)) "Alex Morgan"
        else "Sarah Khan"
    }
}
