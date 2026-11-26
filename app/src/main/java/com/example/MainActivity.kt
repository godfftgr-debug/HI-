package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ai.PlaceholderBankingAiRepository
import com.example.data.local.TpmnDatabase
import com.example.data.repository.BankingRepository
import com.example.ui.AppTab
import com.example.ui.DashboardUiState
import com.example.ui.TpmnViewModel
import com.example.ui.components.TpmnBrandMark
import com.example.ui.components.tpmnVaultBackground
import com.example.ui.screens.AdminControlHubScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyObsidian
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TpmnBankTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        val db = TpmnDatabase.getInstance(applicationContext)
        val repository = BankingRepository(db.tpmnDao())
        val aiRepository = PlaceholderBankingAiRepository(repository)

        setContent {
            TpmnBankTheme {
                val vm: TpmnViewModel = viewModel(
                    factory = TpmnViewModel.Factory(repository, aiRepository)
                )
                val authState by vm.authState.collectAsStateWithLifecycle()
                val uiState by vm.uiState.collectAsStateWithLifecycle()

                if (uiState.currentUser == null) {
                    AuthScreen(
                        authState = authState,
                        onLogin = vm::login,
                        onRegister = vm::register,
                        onClearError = vm::clearAuthError
                    )
                } else {
                    TpmnMainShell(
                        state = uiState,
                        vm = vm
                    )
                }
            }
        }
    }
}

@Composable
private fun TpmnMainShell(
    state: DashboardUiState,
    vm: TpmnViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.bannerFeedback) {
        val msg = state.bannerFeedback
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vm.dismissBannerFeedback()
        }
    }

    val navItems = listOf(
        Triple(
            AppTab.VAULT,
            stringResource(R.string.nav_home),
            Icons.Filled.AccountBalanceWallet to Icons.Outlined.AccountBalanceWallet
        ),
        Triple(
            AppTab.AI_BANKER,
            stringResource(R.string.nav_ai),
            Icons.Filled.AutoAwesome to Icons.Outlined.AutoAwesome
        ),
        Triple(
            AppTab.CONTROL_HUB,
            stringResource(R.string.nav_admin),
            Icons.Filled.AdminPanelSettings to Icons.Outlined.AdminPanelSettings
        )
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = NavyObsidian,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = NavySurface,
                        tonalElevation = 0.dp
                    ) {
                        navItems.forEach { (tab, label, icons) ->
                            val selected = state.activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { vm.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = GoldPrimary,
                                    selectedTextColor = GoldPrimary,
                                    indicatorColor = if (tab == AppTab.AI_BANKER) EmeraldDeep else GoldContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .tpmnVaultBackground()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = NavySurface,
                        header = {
                            TpmnBrandMark(
                                sizeDp = 40,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        },
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.weight(0.2f))
                        navItems.forEach { (tab, label, icons) ->
                            val selected = state.activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { vm.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = GoldPrimary,
                                    selectedTextColor = GoldPrimary,
                                    indicatorColor = GoldContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (state.activeTab) {
                        AppTab.VAULT -> DashboardScreen(
                            state = state,
                            onToggleBalance = vm::toggleBalanceVisibility,
                            onToggleFreeze = vm::toggleSelfCardFreeze,
                            onQuickAiCommand = { cmd -> vm.launchAiCommandFromDashboard(cmd, isVoice = false) },
                            onOpenAiBanker = { vm.selectTab(AppTab.AI_BANKER) },
                            onSignOut = vm::logout
                        )
                        AppTab.AI_BANKER -> AiAssistantScreen(
                            state = state,
                            onSendCommand = vm::sendAiCommand,
                            onConfirmPending = vm::confirmPendingAiAction,
                            onCancelPending = vm::cancelPendingAiAction,
                            onClearChat = vm::clearChat,
                            onBackToVault = { vm.selectTab(AppTab.VAULT) }
                        )
                        AppTab.CONTROL_HUB -> AdminControlHubScreen(
                            state = state,
                            onSavePolicy = vm::adminUpdatePolicy,
                            onToggleUserFreeze = vm::adminToggleUserFreeze,
                            onAdjustUserBalance = vm::adminAdjustUserBalance,
                            onCycleUserKyc = vm::adminCycleKycTier,
                            onUpdateUserPermissions = vm::adminUpdateUserPermissions,
                            onBackToVault = { vm.selectTab(AppTab.VAULT) }
                        )
                    }
                }
            }
        }
    }
}
