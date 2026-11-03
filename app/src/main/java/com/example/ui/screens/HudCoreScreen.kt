package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.EmotionMood
import com.example.data.model.EmotionState
import com.example.data.model.MemorySuggestion
import com.example.data.model.PendingSystemAction
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanDim
import com.example.ui.theme.HudDeepNavy
import com.example.ui.theme.HudEmerald
import com.example.ui.theme.HudIceWhite
import com.example.ui.theme.HudMutedText
import com.example.ui.theme.HudObsidian
import com.example.ui.theme.HudPanelElevated
import com.example.ui.theme.HudPanelSurface
import com.example.ui.theme.HudSkyBlue
import com.example.viewmodel.AiTaskState
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HudCoreScreen(
    emotionState: EmotionState,
    chatMessages: List<ChatMessage>,
    chatTaskState: AiTaskState,
    isListening: Boolean,
    isSpeaking: Boolean,
    halfDuplexEnabled: Boolean,
    trustedModeAuto: Boolean,
    conversationVoiceMode: Boolean,
    pendingMemoryPrompt: MemorySuggestion?,
    onToggleVoiceListen: () -> Unit,
    onStopSpeaking: () -> Unit,
    onToggleTrustedMode: (Boolean) -> Unit,
    onToggleHalfDuplex: (Boolean) -> Unit,
    onToggleConversationVoiceMode: () -> Unit,
    onSelectMood: (EmotionMood) -> Unit,
    onSendCommand: (String) -> Unit,
    onQuickWebSearch: (String) -> Unit,
    onSpeakMessage: (String) -> Unit,
    onRegenerateMessage: (ChatMessage) -> Unit,
    onNewConversation: () -> Unit,
    onClearConversation: () -> Unit,
    onConfirmMemory: (MemorySuggestion) -> Unit,
    onDismissMemory: () -> Unit,
    onTriggerAction: (PendingSystemAction) -> Unit
) {
    val context = LocalContext.current
    var commandText by remember { mutableStateOf("") }
    var showEmotionCalibrator by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val greetingText = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "GOOD MORNING, SIR"
            hour < 17 -> "GOOD AFTERNOON, SIR"
            else -> "GOOD EVENING, SIR"
        }
    }

    val lastMsgLength = chatMessages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(chatMessages.size, lastMsgLength, chatTaskState) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // JARVIS Main HUD Core Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hud_visualizer_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HudPanelSurface.copy(alpha = 0.92f)),
            border = BorderStroke(1.dp, HudCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status & Conversation Action Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Trusted Mode Toggle Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (trustedModeAuto) HudEmerald.copy(alpha = 0.18f) else HudAmber.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, if (trustedModeAuto) HudEmerald else HudAmber),
                        modifier = Modifier
                            .clickable { onToggleTrustedMode(!trustedModeAuto) }
                            .testTag("trusted_mode_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (trustedModeAuto) Icons.Default.VerifiedUser else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (trustedModeAuto) HudEmerald else HudAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (trustedModeAuto) stringResource(R.string.trusted_mode_active) else stringResource(R.string.trusted_mode_confirm),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (trustedModeAuto) HudEmerald else HudAmber
                            )
                        }
                    }

                    // Half-Duplex Echo Guard Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (halfDuplexEnabled) HudCyan.copy(alpha = 0.15f) else HudPanelElevated,
                        border = BorderStroke(1.dp, if (halfDuplexEnabled) HudCyan else HudCyanDim.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clickable { onToggleHalfDuplex(!halfDuplexEnabled) }
                            .testTag("half_duplex_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (halfDuplexEnabled) HudCyan else HudMutedText,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (halfDuplexEnabled) "ECHO GUARD: ON" else "ECHO GUARD: OFF",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (halfDuplexEnabled) HudCyan else HudMutedText
                            )
                        }
                    }

                    // New Conversation Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HudPanelElevated,
                        border = BorderStroke(1.dp, HudSkyBlue.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clickable { onNewConversation() }
                            .testTag("new_conversation_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddComment,
                                contentDescription = "New Conversation",
                                tint = HudSkyBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("NEW CHAT", style = MaterialTheme.typography.labelSmall, color = HudSkyBlue)
                        }
                    }

                    // Clear Conversation Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HudPanelElevated,
                        border = BorderStroke(1.dp, HudMutedText.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clickable { onClearConversation() }
                            .testTag("clear_conversation_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Conversation",
                                tint = HudMutedText,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("CLEAR", style = MaterialTheme.typography.labelSmall, color = HudMutedText)
                        }
                    }
                }

                // Animated Circular JARVIS AI Core + Center Greeting & Telemetry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ArcReactorVisualizer(
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        isThinking = chatTaskState is AiTaskState.Loading || chatTaskState is AiTaskState.Streaming,
                        mood = emotionState.mood,
                        modifier = Modifier
                            .size(108.dp)
                            .clickable { onToggleVoiceListen() }
                            .testTag("arc_reactor_visualizer")
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = greetingText,
                            style = MaterialTheme.typography.titleMedium,
                            color = HudIceWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                isSpeaking -> "JARVIS VOICE ENGINE // SPEAKING"
                                isListening -> "JARVIS VOICE LINK // LISTENING"
                                chatTaskState is AiTaskState.Loading || chatTaskState is AiTaskState.Streaming -> "JARVIS BRAIN // ANALYZING"
                                else -> "JARVIS SYSTEM ONLINE"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = when {
                                isSpeaking -> HudEmerald
                                isListening -> HudAmber
                                else -> HudCyan
                            }
                        )

                        // Emotion AI / Neural Context Bar
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = HudObsidian.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, HudSkyBlue.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEmotionCalibrator = !showEmotionCalibrator }
                                .testTag("emotion_widget_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = HudCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "CONTEXT: ${emotionState.mood.id.uppercase()} (${emotionState.mood.adapterTone})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HudIceWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${emotionState.confidence}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HudCyan
                                )
                            }
                        }

                        // Voice Mic & Stop Speaking Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onToggleVoiceListen,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isListening) HudAmber else HudCyan.copy(alpha = 0.22f),
                                    contentColor = if (isListening) HudObsidian else HudCyan
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("voice_listen_button")
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isListening) "Listening…" else "Hey JARVIS",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            if (isSpeaking) {
                                OutlinedButton(
                                    onClick = onStopSpeaking,
                                    border = BorderStroke(1.dp, HudEmerald),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("stop_tts_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Interrupt speaking",
                                        tint = HudEmerald,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stop", style = MaterialTheme.typography.labelSmall, color = HudEmerald)
                                }
                            }
                        }
                    }
                }

                // Expandable Emotion AI Calibrator
                AnimatedVisibility(visible = showEmotionCalibrator) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EmotionMood.entries.forEach { mood ->
                            FilterChip(
                                selected = emotionState.mood == mood,
                                onClick = { onSelectMood(mood) },
                                label = {
                                    Text(
                                        text = mood.id.uppercase(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HudCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = HudCyan
                                )
                            )
                        }
                    }
                }
            }
        }

        // Memory Prompt Banner
        AnimatedVisibility(visible = pendingMemoryPrompt != null) {
            pendingMemoryPrompt?.let { suggestion ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("memory_prompt_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HudPanelElevated),
                    border = BorderStroke(1.dp, HudEmerald)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = null,
                                    tint = HudEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PERMANENT MEMORY DETECTED [${suggestion.category.uppercase()}]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HudEmerald
                                )
                            }
                            Text(
                                text = "${suggestion.key}: ${suggestion.content}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HudIceWhite,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = onDismissMemory,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Skip", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { onConfirmMemory(suggestion) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HudEmerald,
                                    contentColor = HudObsidian
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("confirm_save_memory_button")
                            ) {
                                Text("Save", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Quick Voice & Phone Control Command Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickPrompts = listOf(
                "Hey Jarvis" to "Hey Jarvis, give me a full system status report",
                "Flashlight ON" to "Turn on flashlight",
                "Battery %" to "Tell me the battery percentage",
                "Open YouTube" to "Open YouTube",
                "Open WhatsApp" to "Open WhatsApp",
                "Set Alarm" to "Set an alarm for 07:00",
                "Read Notifications" to "Read my notifications",
                "Open Settings" to "Open settings",
                "नेपालीमा कुरा" to "नमस्ते जार्विस, के छ खबर?"
            )
            quickPrompts.forEach { (label, prompt) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = HudPanelSurface,
                    border = BorderStroke(1.dp, HudCyanDim.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { onSendCommand(prompt) }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = HudSkyBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // AI Conversation Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("hud_chat_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                ChatBubbleItem(
                    message = msg,
                    onSpeakMessage = { onSpeakMessage(msg.text) },
                    onCopyMessage = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("JARVIS Response", msg.text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onRegenerate = { onRegenerateMessage(msg) },
                    onExecuteAction = onTriggerAction
                )
            }

            if (chatTaskState is AiTaskState.Loading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = HudCyan,
                        trackColor = HudPanelElevated
                    )
                }
            }

            if (chatTaskState is AiTaskState.Error) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = HudCrimson.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, HudCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "NEURAL ALERT: ${chatTaskState.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HudCrimson,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Bottom Console: Microphone Button + Text Input + Search Button + Voice Mode Button + Send Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Microphone Activation Button
            IconButton(
                onClick = onToggleVoiceListen,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isListening) HudAmber else HudPanelElevated)
                    .border(1.dp, if (isListening) HudAmber else HudCyanDim.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .testTag("bottom_mic_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Microphone activation",
                    tint = if (isListening) HudObsidian else HudCyan
                )
            }

            OutlinedTextField(
                value = commandText,
                onValueChange = { commandText = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.chat_input_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = HudMutedText.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (commandText.isNotBlank()) {
                            onSendCommand(commandText)
                            commandText = ""
                        }
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HudCyan,
                    unfocusedBorderColor = HudCyanDim.copy(alpha = 0.5f),
                    focusedContainerColor = HudPanelSurface,
                    unfocusedContainerColor = HudPanelSurface,
                    focusedTextColor = HudIceWhite,
                    unfocusedTextColor = HudIceWhite
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_command_input")
            )

            // Search Button
            IconButton(
                onClick = {
                    val q = commandText.trim()
                    if (q.isNotBlank()) {
                        commandText = ""
                    }
                    onQuickWebSearch(q)
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HudPanelElevated)
                    .border(1.dp, HudSkyBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .testTag("bottom_search_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Internet Search",
                    tint = HudSkyBlue
                )
            }

            // Voice Conversation Mode Toggle Button
            IconButton(
                onClick = onToggleConversationVoiceMode,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (conversationVoiceMode) HudEmerald.copy(alpha = 0.25f) else HudPanelElevated)
                    .border(
                        1.dp,
                        if (conversationVoiceMode) HudEmerald else HudMutedText.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("bottom_voice_mode_button")
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Conversation Voice Mode",
                    tint = if (conversationVoiceMode) HudEmerald else HudMutedText
                )
            }

            // Send Command Button
            IconButton(
                onClick = {
                    if (commandText.isNotBlank()) {
                        onSendCommand(commandText)
                        commandText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HudCyan)
                    .testTag("send_command_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send_command),
                    tint = HudObsidian
                )
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    onSpeakMessage: () -> Unit,
    onCopyMessage: () -> Unit,
    onRegenerate: () -> Unit,
    onExecuteAction: (PendingSystemAction) -> Unit
) {
    val isUser = message.sender == "USER"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) HudPanelElevated else HudDeepNavy
            ),
            border = BorderStroke(
                1.dp,
                if (isUser) HudSkyBlue.copy(alpha = 0.5f) else HudCyan.copy(alpha = 0.45f)
            ),
            modifier = Modifier.widthIn(max = 350.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isUser) "SIR // OPERATOR" else "JARVIS AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUser) HudSkyBlue else HudCyan,
                            fontWeight = FontWeight.Bold
                        )
                        message.emotionTag?.let { tag ->
                            Text(
                                text = "[$tag]",
                                style = MaterialTheme.typography.labelSmall,
                                color = HudEmerald
                            )
                        }
                    }

                    // Voice Playback, Copy, & Regenerate Action Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onSpeakMessage,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak message",
                                tint = HudCyan,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        IconButton(
                            onClick = onCopyMessage,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy response",
                                tint = HudMutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (!isUser && message.id != "welcome_1") {
                            IconButton(
                                onClick = onRegenerate,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate response",
                                    tint = HudSkyBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = HudIceWhite
                )

                message.pendingAction?.let { action ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HudObsidian,
                        border = BorderStroke(1.dp, HudAmber.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExecuteAction(action) }
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ANDROID ACTION // ${action.title}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HudAmber
                                )
                                Text(
                                    text = action.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HudIceWhite,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Execute Android action",
                                tint = HudAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArcReactorVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    isThinking: Boolean,
    mood: EmotionMood,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor")
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking || isSpeaking) 3500 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening || isSpeaking) 550 else 1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val coreColor = when {
        isListening -> HudAmber
        isSpeaking -> HudEmerald
        mood == EmotionMood.STRESSED -> HudCrimson
        else -> HudCyan
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(coreColor.copy(alpha = 0.25f), HudObsidian)
                )
            )
            .border(1.5.dp, coreColor.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f

            rotate(degrees = outerRotation, pivot = center) {
                for (i in 0 until 6) {
                    drawArc(
                        color = coreColor.copy(alpha = 0.75f),
                        startAngle = i * 60f,
                        sweepAngle = 36f,
                        useCenter = false,
                        topLeft = Offset(center.x - maxR * 0.86f, center.y - maxR * 0.86f),
                        size = Size(maxR * 1.72f, maxR * 1.72f),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            rotate(degrees = -outerRotation * 1.3f, pivot = center) {
                for (i in 0 until 12) {
                    val angleRad = Math.toRadians((i * 30.0))
                    val r1 = maxR * 0.56f
                    val r2 = maxR * 0.68f
                    drawLine(
                        color = HudSkyBlue.copy(alpha = 0.8f),
                        start = Offset(
                            center.x + (r1 * cos(angleRad)).toFloat(),
                            center.y + (r1 * sin(angleRad)).toFloat()
                        ),
                        end = Offset(
                            center.x + (r2 * cos(angleRad)).toFloat(),
                            center.y + (r2 * sin(angleRad)).toFloat()
                        ),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            drawCircle(
                color = coreColor.copy(alpha = 0.35f),
                radius = maxR * 0.42f * pulseScale,
                center = center
            )
            drawCircle(
                color = HudIceWhite,
                radius = maxR * 0.20f * pulseScale,
                center = center
            )
        }
    }
}
