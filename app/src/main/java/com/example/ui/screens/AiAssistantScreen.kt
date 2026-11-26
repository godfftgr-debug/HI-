package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.AiChatMessageEntity
import com.example.ui.DashboardUiState
import com.example.ui.components.SampleAiNoticeBanner
import com.example.ui.components.formatShortTime
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
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    state: DashboardUiState,
    onSendCommand: (String, Boolean) -> Unit,
    onConfirmPending: (AiChatMessageEntity) -> Unit,
    onCancelPending: (AiChatMessageEntity) -> Unit,
    onClearChat: () -> Unit,
    onBackToVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToVault)

    val context = LocalContext.current
    var promptText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var showVoiceFallbackSheet by remember { mutableStateOf(false) }
    var voiceStatusMessage by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                voiceStatusMessage = "Listening for banking command…"
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                voiceStatusMessage = "Voice capture unavailable on this device—use preset phrases or type below."
                showVoiceFallbackSheet = true
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    voiceStatusMessage = null
                    onSendCommand(spoken, true)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    fun startSpeechCapture() {
        if (speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your TPMN BANK command")
            }
            try {
                speechRecognizer.startListening(intent)
                isListening = true
            } catch (_: Exception) {
                isListening = false
                showVoiceFallbackSheet = true
            }
        } else {
            voiceStatusMessage = "Device speech recognizer unavailable—select a voice command sample or type."
            showVoiceFallbackSheet = true
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSpeechCapture()
        } else {
            voiceStatusMessage = "Microphone permission denied. You can still type or test voice phrases."
            showVoiceFallbackSheet = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GoldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "TPMN AI Banker",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Max AI TX: $${state.policy.maxAiTransactionAmount.toInt()} • Confirm > $${state.policy.requireConfirmationAbove.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGlow
                        )
                    }
                }
                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier.testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear AI chat",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            SampleAiNoticeBanner()

            // Live Policy Capability Strip
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val chips = listOf(
                    "Transfer" to state.policy.allowMoneyTransfer,
                    "Bill Pay" to state.policy.allowBillPay,
                    "Vault/Card" to state.policy.allowAccountManagement,
                    "Voice" to state.policy.allowVoiceCommands,
                    "Support" to state.policy.allowCustomerSupport
                )
                items(chips) { (label, enabled) ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (enabled) EmeraldDeep.copy(alpha = 0.75f) else NavyElevated,
                        border = BorderStroke(
                            0.7.dp,
                            if (enabled) EmeraldGlow.copy(alpha = 0.55f) else DangerCrimson.copy(alpha = 0.45f)
                        )
                    ) {
                        Text(
                            text = if (enabled) "$label ✓" else "$label ✕",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (enabled) EmeraldGlow else DangerCrimson,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Conversation Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    ChatBubbleCard(
                        message = msg,
                        onConfirm = { onConfirmPending(msg) },
                        onCancel = { onCancelPending(msg) }
                    )
                }

                if (state.isAiThinking) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = NavySurface,
                            border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = GoldPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Evaluating Admin Guardrails & executing…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GoldLight
                                )
                            }
                        }
                    }
                }
            }

            // Voice Listening or Emulator Voice Phrase Helper
            AnimatedVisibility(visible = isListening || showVoiceFallbackSheet) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = EmeraldDeep.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.65f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = EmeraldGlow)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = voiceStatusMessage ?: "Voice Banking Mode",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    speechRecognizer?.stopListening()
                                    isListening = false
                                    showVoiceFallbackSheet = false
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close voice", tint = TextSecondary)
                            }
                        }
                        if (isListening) {
                            LinearProgressIndicator(
                                color = GoldPrimary,
                                trackColor = NavyObsidian,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            )
                        }
                        if (showVoiceFallbackSheet) {
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Send $150 to Sarah",
                                    "Pay $90 electric bill",
                                    "Freeze my card",
                                    "Check my balance"
                                ).forEach { sampleVoice ->
                                    AssistChip(
                                        onClick = {
                                            showVoiceFallbackSheet = false
                                            onSendCommand(sampleVoice, true)
                                        },
                                        label = { Text("🎙 $sampleVoice") },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = NavySurface,
                                            labelColor = GoldLight
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Follow-up Suggestion Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(state.latestSuggestions) { suggestion ->
                    AssistChip(
                        onClick = { onSendCommand(suggestion, false) },
                        label = { Text(suggestion, style = MaterialTheme.typography.bodyMedium) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = NavySurface,
                            labelColor = GoldLight
                        ),
                        border = BorderStroke(0.75.dp, GoldPrimary.copy(alpha = 0.45f))
                    )
                }
            }

            // Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.ai_input_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (promptText.isNotBlank()) {
                                onSendCommand(promptText, false)
                                promptText = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = NavySurface,
                        unfocusedContainerColor = NavySurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = {
                        val hasAudio = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasAudio) startSpeechCapture()
                        else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isListening) GoldPrimary else EmeraldDeep,
                    border = BorderStroke(1.dp, if (isListening) GoldLight else EmeraldGlow),
                    modifier = Modifier
                        .size(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("ai_voice_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.cd_voice_mic),
                            tint = if (isListening) NavyObsidian else EmeraldGlow
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = {
                        if (promptText.isNotBlank()) {
                            onSendCommand(promptText, false)
                            promptText = ""
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = GoldPrimary,
                    modifier = Modifier
                        .size(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("ai_send_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.cd_send_command),
                            tint = NavyObsidian
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleCard(
    message: AiChatMessageEntity,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val isUser = message.sender == "USER"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 330.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) EmeraldDeep.copy(alpha = 0.85f) else NavySurface,
            border = BorderStroke(
                0.85.dp,
                if (isUser) EmeraldGlow.copy(alpha = 0.6f) else GoldPrimary.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!message.actionBadge.isNullOrBlank()) {
                        val badgeColor = when {
                            message.actionBadge.contains("BLOCK") || message.actionBadge.contains("DECLIN") || message.actionBadge.contains("PAUSED") -> DangerCrimson
                            message.actionBadge.contains("CONFIRM") -> WarningAmber
                            else -> GoldPrimary
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.18f),
                            border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = message.actionBadge,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (message.inputMode == "VOICE") {
                        Text(
                            text = "VOICE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = EmeraldGlow
                        )
                    }
                }

                if (!message.actionBadge.isNullOrBlank() || message.inputMode == "VOICE") {
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )

                if (message.pendingActionType != null && !message.pendingResolved) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = NavyObsidian
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("confirm_ai_action_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Confirm", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onCancel,
                            border = BorderStroke(0.8.dp, DangerCrimson),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("cancel_ai_action_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = DangerCrimson, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", color = DangerCrimson)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatShortTime(message.timestamp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp
                    ),
                    color = TextMuted
                )
            }
        }
    }
}
