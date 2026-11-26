package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AdminAuditLogEntity
import com.example.data.local.AiPolicyEntity
import com.example.data.local.UserEntity
import com.example.ui.DashboardUiState
import com.example.ui.components.formatShortTime
import com.example.ui.components.formatUsd
import com.example.ui.theme.DangerContainer
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.NavyObsidian
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminControlHubScreen(
    state: DashboardUiState,
    onSavePolicy: (AiPolicyEntity) -> Unit,
    onToggleUserFreeze: (UserEntity) -> Unit,
    onAdjustUserBalance: (String, Double) -> Unit,
    onCycleUserKyc: (String) -> Unit,
    onUpdateUserPermissions: (UserEntity) -> Unit,
    onBackToVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToVault)

    val isAdmin = state.currentUser?.role == "ADMIN"
    var subTab by remember { mutableIntStateOf(0) }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }

    val activeModulesCount = listOf(
        state.policy.allowMoneyTransfer,
        state.policy.allowBillPay,
        state.policy.allowAccountManagement,
        state.policy.allowVoiceCommands,
        state.policy.allowCustomerSupport
    ).count { it }
    val frozenCount = state.allUsers.count { it.isFrozen }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
        ) {
            // Executive Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GoldContainer,
                        border = BorderStroke(0.9.dp, GoldPrimary.copy(alpha = 0.7f)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Admin Control Hub",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isAdmin) "AI Feature Governance & Account Permissions" else "Read-Only Governance Preview",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (state.policy.aiAssistantEnabled) EmeraldDeep else DangerContainer,
                    border = BorderStroke(
                        0.8.dp,
                        if (state.policy.aiAssistantEnabled) EmeraldGlow else DangerCrimson
                    )
                ) {
                    Text(
                        text = if (state.policy.aiAssistantEnabled) "AI LIVE" else "AI PAUSED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.policy.aiAssistantEnabled) EmeraldGlow else DangerCrimson,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Live Governance Telemetry Strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                item {
                    TelemetryKpiCard(
                        label = "AI Modules",
                        value = if (state.policy.aiAssistantEnabled) "$activeModulesCount / 5" else "0 / 5",
                        accent = if (state.policy.aiAssistantEnabled) EmeraldGlow else DangerCrimson
                    )
                }
                item {
                    TelemetryKpiCard(
                        label = "Accounts",
                        value = "${state.allUsers.size}",
                        accent = GoldLight
                    )
                }
                item {
                    TelemetryKpiCard(
                        label = "Frozen",
                        value = "$frozenCount",
                        accent = if (frozenCount > 0) DangerCrimson else EmeraldGlow
                    )
                }
                item {
                    TelemetryKpiCard(
                        label = "Global AI Cap",
                        value = "$${state.policy.maxAiTransactionAmount.toInt()}",
                        accent = GoldPrimary
                    )
                }
            }

            TabRow(
                selectedTabIndex = subTab,
                containerColor = NavySurface,
                contentColor = GoldPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[subTab]),
                        color = GoldPrimary
                    )
                }
            ) {
                Tab(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    text = { Text("AI Features") },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("admin_tab_rules")
                )
                Tab(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    text = { Text("Permissions") },
                    icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("admin_tab_accounts")
                )
                Tab(
                    selected = subTab == 2,
                    onClick = { subTab = 2 },
                    text = { Text("Audit & PRD") },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("admin_tab_prd")
                )
            }

            when (subTab) {
                0 -> AiFeaturesToggleTab(
                    policy = state.policy,
                    isAdmin = isAdmin,
                    onInstantPolicyChange = onSavePolicy,
                    onSavePolicy = onSavePolicy
                )
                1 -> UserPermissionsManagementTab(
                    users = state.allUsers,
                    isAdmin = isAdmin,
                    onToggleFreeze = onToggleUserFreeze,
                    onAdjustBalance = onAdjustUserBalance,
                    onCycleKyc = onCycleUserKyc,
                    onOpenPermissionEditor = { editingUser = it }
                )
                else -> AuditAndPrdTab(auditLogs = state.auditLogs)
            }
        }
    }

    val selectedForEdit = editingUser
    if (selectedForEdit != null) {
        val latestUser = state.allUsers.firstOrNull { it.id == selectedForEdit.id } ?: selectedForEdit
        ModalBottomSheet(
            onDismissRequest = { editingUser = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = NavySurface,
            contentColor = TextPrimary
        ) {
            UserPermissionsSheetContent(
                user = latestUser,
                isAdmin = isAdmin,
                onSavePermissions = { updated ->
                    onUpdateUserPermissions(updated)
                    editingUser = null
                },
                onAdjustBalance = { delta -> onAdjustUserBalance(latestUser.id, delta) },
                onClose = { editingUser = null }
            )
        }
    }
}

@Composable
private fun TelemetryKpiCard(
    label: String,
    value: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NavySurface,
        border = BorderStroke(0.75.dp, NavyBorder),
        modifier = Modifier.width(128.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = TextSecondary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accent,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiFeaturesToggleTab(
    policy: AiPolicyEntity,
    isAdmin: Boolean,
    onInstantPolicyChange: (AiPolicyEntity) -> Unit,
    onSavePolicy: (AiPolicyEntity) -> Unit
) {
    var draft by remember(policy) { mutableStateOf(policy) }
    var maxTxFloat by remember(policy) { mutableFloatStateOf(policy.maxAiTransactionAmount.toFloat()) }
    var confirmThresholdFloat by remember(policy) { mutableFloatStateOf(policy.requireConfirmationAbove.toFloat()) }

    LaunchedEffect(policy) {
        draft = policy
        maxTxFloat = policy.maxAiTransactionAmount.toFloat()
        confirmThresholdFloat = policy.requireConfirmationAbove.toFloat()
    }

    val allAvailableFields = listOf(
        "recipient",
        "amount",
        "billType",
        "accountAction",
        "cardFreeze",
        "supportQuery"
    )
    val selectedFields = draft.allowedFieldsCsv
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toMutableSet()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isAdmin) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = GoldContainer,
                    border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "Read-only preview: Sign in as Admin (admin@tpmnbank.com / admin123) to toggle live AI features and user permissions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoldLight,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Master AI Kill-Switch Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = NavySurface,
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            if (draft.aiAssistantEnabled) EmeraldGlow.copy(alpha = 0.7f) else DangerCrimson.copy(alpha = 0.7f),
                            GoldPrimary.copy(alpha = 0.5f)
                        )
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (draft.aiAssistantEnabled) GoldPrimary else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Master AI Core Engine",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = if (draft.aiAssistantEnabled) {
                                    "All enabled AI banking features are live across user sessions."
                                } else {
                                    "Global kill-switch active: AI execution halted."
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = draft.aiAssistantEnabled,
                        onCheckedChange = { checked ->
                            val next = draft.copy(aiAssistantEnabled = checked)
                            draft = next
                            onInstantPolicyChange(next)
                        },
                        enabled = isAdmin,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NavyObsidian,
                            checkedTrackColor = EmeraldGlow,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DangerContainer
                        ),
                        modifier = Modifier.testTag("toggle_master_ai")
                    )
                }
            }
        }

        // Instant Feature Toggles
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NavySurface,
                border = BorderStroke(0.85.dp, NavyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "AI Feature Toggles (Live Enforcement)",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldLight
                    )
                    PolicyToggleRow(
                        label = "Money Transfers",
                        subtitle = "Allow AI to initiate P2P & external transfers",
                        checked = draft.allowMoneyTransfer,
                        enabled = isAdmin,
                        tag = "toggle_transfer_ai"
                    ) { checked ->
                        val next = draft.copy(allowMoneyTransfer = checked)
                        draft = next
                        onInstantPolicyChange(next)
                    }

                    HorizontalDivider(color = NavyBorder.copy(alpha = 0.6f))

                    PolicyToggleRow(
                        label = "Bill Payments",
                        subtitle = "Allow AI to settle utility, telecom & grid bills",
                        checked = draft.allowBillPay,
                        enabled = isAdmin,
                        tag = "toggle_bill_ai"
                    ) { checked ->
                        val next = draft.copy(allowBillPay = checked)
                        draft = next
                        onInstantPolicyChange(next)
                    }

                    HorizontalDivider(color = NavyBorder.copy(alpha = 0.6f))

                    PolicyToggleRow(
                        label = "Account & Card Controls",
                        subtitle = "Allow AI to freeze cards and sweep funds to vault",
                        checked = draft.allowAccountManagement,
                        enabled = isAdmin,
                        tag = "toggle_account_ai"
                    ) { checked ->
                        val next = draft.copy(allowAccountManagement = checked)
                        draft = next
                        onInstantPolicyChange(next)
                    }

                    HorizontalDivider(color = NavyBorder.copy(alpha = 0.6f))

                    PolicyToggleRow(
                        label = "Voice Command Execution",
                        subtitle = "Permit hands-free SpeechRecognizer banking",
                        checked = draft.allowVoiceCommands,
                        enabled = isAdmin,
                        tag = "toggle_voice_ai"
                    ) { checked ->
                        val next = draft.copy(allowVoiceCommands = checked)
                        draft = next
                        onInstantPolicyChange(next)
                    }

                    HorizontalDivider(color = NavyBorder.copy(alpha = 0.6f))

                    PolicyToggleRow(
                        label = "AI Customer Support",
                        subtitle = "Allow conversational support & policy inquiries",
                        checked = draft.allowCustomerSupport,
                        enabled = isAdmin,
                        tag = "toggle_support_ai"
                    ) { checked ->
                        val next = draft.copy(allowCustomerSupport = checked)
                        draft = next
                        onInstantPolicyChange(next)
                    }
                }
            }
        }

        // Allowed AI Fields Matrix
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NavySurface,
                border = BorderStroke(0.85.dp, NavyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Authorized AI Execution Fields",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldLight
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap any field to immediately allow or block the AI from populating it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allAvailableFields.forEach { field ->
                            val active = field in selectedFields
                            FilterChip(
                                selected = active,
                                enabled = isAdmin,
                                onClick = {
                                    if (active) selectedFields.remove(field) else selectedFields.add(field)
                                    val next = draft.copy(allowedFieldsCsv = selectedFields.joinToString(","))
                                    draft = next
                                    onInstantPolicyChange(next)
                                },
                                label = { Text(field, fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldDeep,
                                    selectedLabelColor = EmeraldGlow,
                                    containerColor = NavyElevated,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("field_chip_$field")
                            )
                        }
                    }
                }
            }
        }

        // Numeric Limits & Operational Rule
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NavySurface,
                border = BorderStroke(0.85.dp, NavyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Transaction Caps & Guardrail Rule",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldLight
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Max AI Transaction Limit: ${formatUsd(maxTxFloat.toDouble())}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Slider(
                        value = maxTxFloat,
                        onValueChange = {
                            maxTxFloat = it
                            draft = draft.copy(maxAiTransactionAmount = it.toDouble())
                        },
                        enabled = isAdmin,
                        valueRange = 100f..10000f,
                        colors = SliderDefaults.colors(
                            thumbColor = GoldPrimary,
                            activeTrackColor = GoldPrimary
                        )
                    )

                    Text(
                        text = "Require 1-Tap Confirmation Above: ${formatUsd(confirmThresholdFloat.toDouble())}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Slider(
                        value = confirmThresholdFloat,
                        onValueChange = {
                            confirmThresholdFloat = it
                            draft = draft.copy(requireConfirmationAbove = it.toDouble())
                        },
                        enabled = isAdmin,
                        valueRange = 50f..5000f,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldGlow,
                            activeTrackColor = EmeraldGlow
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = draft.customOperationalRule,
                        onValueChange = { draft = draft.copy(customOperationalRule = it) },
                        enabled = isAdmin,
                        label = { Text("Operational Guardrail Rule") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onSavePolicy(draft) },
                        enabled = isAdmin,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = NavyObsidian
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_ai_policy_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Caps & Guardrail Rule", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicyToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NavyObsidian,
                checkedTrackColor = EmeraldGlow,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = NavyElevated
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

private enum class UserFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    FROZEN("Frozen"),
    ADMINS("Admins")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserPermissionsManagementTab(
    users: List<UserEntity>,
    isAdmin: Boolean,
    onToggleFreeze: (UserEntity) -> Unit,
    onAdjustBalance: (String, Double) -> Unit,
    onCycleKyc: (String) -> Unit,
    onOpenPermissionEditor: (UserEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(UserFilter.ALL) }

    val filteredUsers = remember(users, searchQuery, activeFilter) {
        users.filter { user ->
            val matchesQuery = searchQuery.isBlank() ||
                user.fullName.contains(searchQuery, ignoreCase = true) ||
                user.username.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true) ||
                user.accountNumber.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (activeFilter) {
                UserFilter.ALL -> true
                UserFilter.ACTIVE -> !user.isFrozen
                UserFilter.FROZEN -> user.isFrozen
                UserFilter.ADMINS -> user.role == "ADMIN"
            }
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search name, @username, email or account #", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = NavyBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = NavySurface,
                    unfocusedContainerColor = NavySurface
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_user_search_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(UserFilter.entries) { filter ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { activeFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldContainer,
                            selectedLabelColor = GoldLight,
                            containerColor = NavySurface,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("user_filter_${filter.name.lowercase()}")
                    )
                }
            }
        }

        if (filteredUsers.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = NavySurface,
                    border = BorderStroke(0.8.dp, NavyBorder)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No matching accounts", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Try clearing the search query or filter.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        } else {
            items(filteredUsers, key = { it.id }) { acct ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = NavySurface,
                    border = BorderStroke(
                        0.85.dp,
                        if (acct.isFrozen) DangerCrimson.copy(alpha = 0.65f) else NavyBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = acct.fullName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "@${acct.username}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        color = GoldPrimary
                                    )
                                }
                                Text(
                                    text = "${acct.accountNumber} • ${acct.email}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (acct.isFrozen) DangerContainer else if (acct.role == "ADMIN") GoldContainer else EmeraldDeep
                            ) {
                                Text(
                                    text = if (acct.isFrozen) "FROZEN" else "${acct.role} • ${acct.kycTier}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (acct.isFrozen) DangerCrimson else if (acct.role == "ADMIN") GoldLight else EmeraldGlow,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Checking: ${formatUsd(acct.checkingBalance)}   •   AI Cap: $${acct.dailyAiSpendLimit.toInt()}/tx",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp),
                            color = GoldLight
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PermissionBadge("Transfer", acct.canAiTransfer)
                            PermissionBadge("Bill Pay", acct.canAiBillPay)
                            PermissionBadge("Voice", acct.canAiVoice)
                            PermissionBadge("Vault", acct.canAiVault)
                        }

                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onOpenPermissionEditor(acct) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldDeep,
                                        contentColor = EmeraldGlow
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(48.dp)
                                        .testTag("manage_permissions_${acct.username}")
                                ) {
                                    Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Permissions", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                if (acct.role != "ADMIN") {
                                    OutlinedButton(
                                        onClick = { onToggleFreeze(acct) },
                                        border = BorderStroke(0.8.dp, if (acct.isFrozen) EmeraldGlow else DangerCrimson),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("admin_freeze_${acct.username}")
                                    ) {
                                        Icon(
                                            imageVector = if (acct.isFrozen) Icons.Default.LockOpen else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (acct.isFrozen) EmeraldGlow else DangerCrimson,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (acct.isFrozen) "Unfreeze" else "Freeze",
                                            color = if (acct.isFrozen) EmeraldGlow else DangerCrimson,
                                            fontSize = 12.sp
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onCycleKyc(acct.id) },
                                        border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier
                                            .weight(0.75f)
                                            .height(48.dp)
                                    ) {
                                        Text("KYC", color = GoldLight, fontSize = 12.sp)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onAdjustBalance(acct.id, 500.0) },
                                        border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Text("+$500", color = GoldLight, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionBadge(label: String, allowed: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (allowed) EmeraldDeep.copy(alpha = 0.7f) else DangerContainer.copy(alpha = 0.7f),
        border = BorderStroke(
            0.6.dp,
            if (allowed) EmeraldGlow.copy(alpha = 0.5f) else DangerCrimson.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = if (allowed) "$label ✓" else "$label ✕",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (allowed) EmeraldGlow else DangerCrimson,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserPermissionsSheetContent(
    user: UserEntity,
    isAdmin: Boolean,
    onSavePermissions: (UserEntity) -> Unit,
    onAdjustBalance: (Double) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    var draft by remember(user.id) { mutableStateOf(user) }
    var spendCapFloat by remember(user.id) { mutableFloatStateOf(user.dailyAiSpendLimit.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Manage Account Permissions",
                    style = MaterialTheme.typography.titleLarge,
                    color = GoldLight
                )
                Text(
                    text = "${user.fullName} (${user.accountNumber})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Icon(Icons.Default.Shield, contentDescription = null, tint = GoldPrimary)
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NavyElevated,
            border = BorderStroke(0.8.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PolicyToggleRow(
                    label = "Freeze Account Access",
                    subtitle = "Blocks transfers, bill payments & vault sweeps",
                    checked = draft.isFrozen,
                    enabled = isAdmin && user.role != "ADMIN",
                    tag = "sheet_toggle_freeze"
                ) { draft = draft.copy(isFrozen = it) }

                HorizontalDivider(color = NavyBorder.copy(alpha = 0.5f))

                PolicyToggleRow(
                    label = "AI Money Transfers",
                    subtitle = "Permit AI-initiated P2P transfers",
                    checked = draft.canAiTransfer,
                    enabled = isAdmin,
                    tag = "sheet_toggle_transfer"
                ) { draft = draft.copy(canAiTransfer = it) }

                PolicyToggleRow(
                    label = "AI Bill Payments",
                    subtitle = "Permit AI utility & telecom settlements",
                    checked = draft.canAiBillPay,
                    enabled = isAdmin,
                    tag = "sheet_toggle_billpay"
                ) { draft = draft.copy(canAiBillPay = it) }

                PolicyToggleRow(
                    label = "Voice Banking Commands",
                    subtitle = "Permit voice-triggered AI execution",
                    checked = draft.canAiVoice,
                    enabled = isAdmin,
                    tag = "sheet_toggle_voice"
                ) { draft = draft.copy(canAiVoice = it) }

                PolicyToggleRow(
                    label = "Prosperity Vault Sweeps",
                    subtitle = "Permit moving funds to savings vault",
                    checked = draft.canAiVault,
                    enabled = isAdmin,
                    tag = "sheet_toggle_vault"
                ) { draft = draft.copy(canAiVault = it) }
            }
        }

        // Per-User Limit, Role & KYC Tier
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NavyElevated,
            border = BorderStroke(0.8.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Per-Transaction AI Spend Cap: ${formatUsd(spendCapFloat.toDouble())}",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Slider(
                    value = spendCapFloat,
                    onValueChange = {
                        spendCapFloat = it
                        draft = draft.copy(dailyAiSpendLimit = it.toDouble())
                    },
                    enabled = isAdmin,
                    valueRange = 250f..10000f,
                    colors = SliderDefaults.colors(
                        thumbColor = GoldPrimary,
                        activeTrackColor = GoldPrimary
                    )
                )

                Text(text = "KYC Verification Tier", style = MaterialTheme.typography.labelLarge, color = GoldLight)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("STANDARD", "GOLD", "PLATINUM").forEach { tier ->
                        FilterChip(
                            selected = draft.kycTier == tier,
                            enabled = isAdmin,
                            onClick = { draft = draft.copy(kycTier = tier) },
                            label = { Text(tier) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldDeep,
                                selectedLabelColor = EmeraldGlow
                            )
                        )
                    }
                }

                Text(text = "Account Role", style = MaterialTheme.typography.labelLarge, color = GoldLight)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("USER", "ADMIN").forEach { role ->
                        FilterChip(
                            selected = draft.role == role,
                            enabled = isAdmin,
                            onClick = { draft = draft.copy(role = role) },
                            label = { Text(role) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldContainer,
                                selectedLabelColor = GoldLight
                            )
                        )
                    }
                }

                if (isAdmin) {
                    Text(
                        text = "Treasury Balance Quick Adjust (Checking: ${formatUsd(user.checkingBalance)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onAdjustBalance(500.0) },
                            border = BorderStroke(0.8.dp, EmeraldGlow),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("+$500 Credit", color = EmeraldGlow)
                        }
                        OutlinedButton(
                            onClick = { onAdjustBalance(-250.0) },
                            border = BorderStroke(0.8.dp, DangerCrimson),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("-$250 Debit", color = DangerCrimson)
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onClose,
                border = BorderStroke(0.8.dp, NavyBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("Cancel", color = TextSecondary)
            }
            Button(
                onClick = { onSavePermissions(draft) },
                enabled = isAdmin,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = NavyObsidian
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.4f)
                    .height(50.dp)
                    .testTag("save_user_permissions_button")
            ) {
                Text("Save Permissions", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun AuditAndPrdTab(auditLogs: List<AdminAuditLogEntity>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Live Supervision Audit Log (${auditLogs.size})",
                style = MaterialTheme.typography.titleMedium,
                color = GoldLight
            )
        }

        items(auditLogs, key = { it.id }) { log ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NavySurface.copy(alpha = 0.85f),
                border = BorderStroke(0.6.dp, NavyBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = log.action,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGlow
                        )
                        Text(
                            text = formatShortTime(log.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${log.actorName}: ${log.detail}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            SpecCard(
                title = "TPMN BANK — PRD & Architecture",
                body = "• Brand: TPMN BANK (TRUST • PROSPERITY • MODERN NETWORK)\n" +
                    "• AI-First Architecture: Natural language text & voice commands execute real Room-backed transfers, bills, vault sweeps & card security.\n" +
                    "• Dual-Layer Governance: Global AI Control Hub switches + per-user granular account permissions & spend limits."
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NavySurface,
                border = BorderStroke(0.85.dp, NavyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "UI/UX Design Palette",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldLight
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ColorSwatchChip("Obsidian", "#061121", NavyObsidian, Modifier.weight(1f))
                        ColorSwatchChip("Gold", "#E2B94B", GoldPrimary, Modifier.weight(1f))
                        ColorSwatchChip("Emerald", "#0F5C4D", EmeraldDeep, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecCard(title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = NavySurface,
        border = BorderStroke(0.85.dp, NavyBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = GoldLight)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun ColorSwatchChip(name: String, hex: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = NavyElevated,
        border = BorderStroke(0.75.dp, NavyBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .background(color, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = name, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = TextPrimary)
            Text(text = hex, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}
