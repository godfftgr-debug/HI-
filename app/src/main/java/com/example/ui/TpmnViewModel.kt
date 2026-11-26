package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiCommandResult
import com.example.data.ai.BankingAiRepository
import com.example.data.ai.PendingAiAction
import com.example.data.local.AdminAuditLogEntity
import com.example.data.local.AiChatMessageEntity
import com.example.data.local.AiPolicyEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.data.repository.BankingOpResult
import com.example.data.repository.BankingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppTab {
    VAULT,
    AI_BANKER,
    CONTROL_HUB
}

sealed interface AuthUiState {
    data object Unauthenticated : AuthUiState
    data object Loading : AuthUiState
    data class Error(val message: String) : AuthUiState
}

data class DashboardUiState(
    val currentUser: UserEntity? = null,
    val transactions: List<TransactionEntity> = emptyList(),
    val messages: List<AiChatMessageEntity> = emptyList(),
    val policy: AiPolicyEntity = AiPolicyEntity(),
    val allUsers: List<UserEntity> = emptyList(),
    val auditLogs: List<AdminAuditLogEntity> = emptyList(),
    val activeTab: AppTab = AppTab.VAULT,
    val isBalanceVisible: Boolean = true,
    val isAiThinking: Boolean = false,
    val latestSuggestions: List<String> = listOf(
        "Check my balance",
        "Send $120 to Sarah",
        "Pay $65 electric bill",
        "Move $250 to vault"
    ),
    val bannerFeedback: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TpmnViewModel(
    private val repository: BankingRepository,
    private val aiRepository: BankingAiRepository
) : ViewModel() {

    private val _currentUserId = MutableStateFlow<String?>(null)
    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Unauthenticated)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _activeTab = MutableStateFlow(AppTab.VAULT)
    private val _isBalanceVisible = MutableStateFlow(true)
    private val _isAiThinking = MutableStateFlow(false)
    private val _latestSuggestions = MutableStateFlow(
        listOf("Check my balance", "Send $120 to Sarah", "Pay $65 electric bill", "Move $250 to vault")
    )
    private val _bannerFeedback = MutableStateFlow<String?>(null)

    private val userFlow = _currentUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(null) else repository.observeUser(uid)
    }

    private val txFlow = _currentUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repository.observeTransactions(uid)
    }

    private val messagesFlow = _currentUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repository.observeMessages(uid)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        userFlow,
        txFlow,
        messagesFlow,
        repository.observePolicy(),
        repository.observeAllUsers(),
        repository.observeAuditLogs(),
        _activeTab,
        _isBalanceVisible,
        _isAiThinking,
        _latestSuggestions,
        _bannerFeedback
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        DashboardUiState(
            currentUser = values[0] as UserEntity?,
            transactions = values[1] as List<TransactionEntity>,
            messages = values[2] as List<AiChatMessageEntity>,
            policy = values[3] as AiPolicyEntity,
            allUsers = values[4] as List<UserEntity>,
            auditLogs = values[5] as List<AdminAuditLogEntity>,
            activeTab = values[6] as AppTab,
            isBalanceVisible = values[7] as Boolean,
            isAiThinking = values[8] as Boolean,
            latestSuggestions = values[9] as List<String>,
            bannerFeedback = values[10] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DashboardUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            repository.ensureSeeded()
            val result = repository.login(identifier, password)
            result.fold(
                onSuccess = { user ->
                    _currentUserId.value = user.id
                    _activeTab.value = if (user.role == "ADMIN") AppTab.CONTROL_HUB else AppTab.VAULT
                    _authState.value = AuthUiState.Unauthenticated
                },
                onFailure = { error ->
                    _authState.value = AuthUiState.Error(error.message ?: "Login failed.")
                }
            )
        }
    }

    fun register(fullName: String, username: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            repository.ensureSeeded()
            val result = repository.register(fullName, username, email, password)
            result.fold(
                onSuccess = { user ->
                    _currentUserId.value = user.id
                    _activeTab.value = AppTab.VAULT
                    _authState.value = AuthUiState.Unauthenticated
                },
                onFailure = { error ->
                    _authState.value = AuthUiState.Error(error.message ?: "Registration failed.")
                }
            )
        }
    }

    fun clearAuthError() {
        if (_authState.value is AuthUiState.Error) {
            _authState.value = AuthUiState.Unauthenticated
        }
    }

    fun logout() {
        _currentUserId.value = null
        _activeTab.value = AppTab.VAULT
        _bannerFeedback.value = null
        _authState.value = AuthUiState.Unauthenticated
    }

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun toggleBalanceVisibility() {
        _isBalanceVisible.value = !_isBalanceVisible.value
    }

    fun dismissBannerFeedback() {
        _bannerFeedback.value = null
    }

    fun launchAiCommandFromDashboard(prompt: String, isVoice: Boolean = false) {
        _activeTab.value = AppTab.AI_BANKER
        sendAiCommand(prompt, isVoice = isVoice)
    }

    fun sendAiCommand(prompt: String, isVoice: Boolean = false) {
        val clean = prompt.trim()
        val user = uiState.value.currentUser ?: return
        if (clean.isBlank()) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.saveMessage(
                AiChatMessageEntity(
                    id = "msg_${UUID.randomUUID().toString().take(8)}",
                    userId = user.id,
                    sender = "USER",
                    text = clean,
                    inputMode = if (isVoice) "VOICE" else "TEXT",
                    timestamp = now
                )
            )
            _isAiThinking.value = true
            try {
                val policy = repository.getPolicy()
                val res = aiRepository.processBankingCommand(
                    user = user,
                    policy = policy,
                    recentTransactions = uiState.value.transactions,
                    userInput = clean,
                    isVoice = isVoice
                )
                saveAiReply(user.id, res, isVoice)
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun confirmPendingAiAction(message: AiChatMessageEntity) {
        val user = uiState.value.currentUser ?: return
        val actionType = message.pendingActionType ?: return
        val target = message.pendingTarget ?: return
        val amount = message.pendingAmount ?: return

        viewModelScope.launch {
            repository.markPendingMessageResolved(message.id)
            _isAiThinking.value = true
            try {
                val policy = repository.getPolicy()
                val res = aiRepository.executeConfirmedAction(
                    user = user,
                    policy = policy,
                    pending = PendingAiAction(actionType, target, amount, "Confirmed"),
                    isVoice = message.inputMode == "VOICE"
                )
                saveAiReply(user.id, res, message.inputMode == "VOICE")
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun cancelPendingAiAction(message: AiChatMessageEntity) {
        val user = uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.markPendingMessageResolved(message.id)
            repository.saveMessage(
                AiChatMessageEntity(
                    id = "msg_${UUID.randomUUID().toString().take(8)}",
                    userId = user.id,
                    sender = "AI",
                    text = "[Sample AI Mode] Action cancelled. No funds were moved.",
                    inputMode = "TEXT",
                    actionBadge = "CANCELLED",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun clearChat() {
        val uid = _currentUserId.value ?: return
        viewModelScope.launch {
            repository.clearChatHistory(uid)
        }
    }

    private suspend fun saveAiReply(userId: String, res: AiCommandResult, isVoice: Boolean) {
        repository.saveMessage(
            AiChatMessageEntity(
                id = "msg_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                sender = "AI",
                text = res.replyText,
                inputMode = if (isVoice) "VOICE" else "TEXT",
                actionBadge = res.actionBadge,
                pendingActionType = res.pendingAction?.actionType,
                pendingTarget = res.pendingAction?.target,
                pendingAmount = res.pendingAction?.amount,
                pendingResolved = false,
                timestamp = System.currentTimeMillis()
            )
        )
        if (res.followUpSuggestions.isNotEmpty()) {
            _latestSuggestions.value = res.followUpSuggestions
        }
    }

    fun toggleSelfCardFreeze() {
        val user = uiState.value.currentUser ?: return
        viewModelScope.launch {
            when (val res = repository.setUserFrozen(user.id, !user.isFrozen, user.fullName)) {
                is BankingOpResult.Success -> _bannerFeedback.value = res.message
                is BankingOpResult.Failure -> _bannerFeedback.value = res.reason
            }
        }
    }

    fun adminUpdatePolicy(updated: AiPolicyEntity) {
        val adminName = uiState.value.currentUser?.fullName ?: "Admin"
        viewModelScope.launch {
            repository.adminUpdatePolicy(updated, adminName)
            _bannerFeedback.value = "AI Control Hub policy saved and enforced live."
        }
    }

    fun adminToggleUserFreeze(target: UserEntity) {
        val adminName = uiState.value.currentUser?.fullName ?: "Admin"
        viewModelScope.launch {
            when (val res = repository.setUserFrozen(target.id, !target.isFrozen, adminName)) {
                is BankingOpResult.Success -> _bannerFeedback.value = res.message
                is BankingOpResult.Failure -> _bannerFeedback.value = res.reason
            }
        }
    }

    fun adminAdjustUserBalance(targetUserId: String, delta: Double) {
        val adminName = uiState.value.currentUser?.fullName ?: "Admin"
        viewModelScope.launch {
            when (val res = repository.adminAdjustBalance(targetUserId, delta, adminName)) {
                is BankingOpResult.Success -> _bannerFeedback.value = res.message
                is BankingOpResult.Failure -> _bannerFeedback.value = res.reason
            }
        }
    }

    fun adminCycleKycTier(targetUserId: String) {
        val adminName = uiState.value.currentUser?.fullName ?: "Admin"
        viewModelScope.launch {
            repository.adminCycleKycTier(targetUserId, adminName)
            _bannerFeedback.value = "Updated KYC tier."
        }
    }

    fun adminUpdateUserPermissions(updatedUser: UserEntity) {
        val adminName = uiState.value.currentUser?.fullName ?: "Admin"
        viewModelScope.launch {
            when (val res = repository.adminUpdateUserPermissions(updatedUser, adminName)) {
                is BankingOpResult.Success -> _bannerFeedback.value = res.message
                is BankingOpResult.Failure -> _bannerFeedback.value = res.reason
            }
        }
    }

    class Factory(
        private val repository: BankingRepository,
        private val aiRepository: BankingAiRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TpmnViewModel(repository, aiRepository) as T
        }
    }
}
