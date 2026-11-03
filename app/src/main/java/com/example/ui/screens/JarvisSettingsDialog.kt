package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.service.JarvisApiKeysConfig
import com.example.service.JarvisVoiceConfig
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanDim
import com.example.ui.theme.HudEmerald
import com.example.ui.theme.HudIceWhite
import com.example.ui.theme.HudMutedText
import com.example.ui.theme.HudObsidian
import com.example.ui.theme.HudPanelSurface
import com.example.ui.theme.HudSkyBlue

@Composable
fun JarvisSettingsDialog(
    initialSection: String, // "VOICE", "AI", "API"
    voiceConfig: JarvisVoiceConfig,
    selectedAiEngine: String,
    jarvisPersonality: String,
    apiKeysConfig: JarvisApiKeysConfig,
    onSaveVoiceConfig: (JarvisVoiceConfig) -> Unit,
    onSaveAiSettings: (engine: String, personality: String) -> Unit,
    onSaveApiKeys: (JarvisApiKeysConfig) -> Unit,
    onTestVoice: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember(initialSection) { mutableStateOf(initialSection) }

    // Voice local state
    var voiceId by remember(voiceConfig) { mutableStateOf(voiceConfig.voiceId) }
    var voicePersona by remember(voiceConfig) { mutableStateOf(voiceConfig.voicePersonaName) }
    var speechRate by remember(voiceConfig) { mutableFloatStateOf(voiceConfig.speechRate) }
    var pitch by remember(voiceConfig) { mutableFloatStateOf(voiceConfig.pitch) }
    var autoSpeak by remember(voiceConfig) { mutableStateOf(voiceConfig.autoSpeakReplies) }
    var convMode by remember(voiceConfig) { mutableStateOf(voiceConfig.conversationVoiceMode) }
    var preferEleven by remember(voiceConfig) { mutableStateOf(voiceConfig.preferElevenLabs) }

    // AI local state
    var engine by remember(selectedAiEngine) { mutableStateOf(selectedAiEngine) }
    var personality by remember(jarvisPersonality) { mutableStateOf(jarvisPersonality) }

    // API local state
    var openAiKey by remember(apiKeysConfig) { mutableStateOf(apiKeysConfig.openAiKeyOverride) }
    var searchKey by remember(apiKeysConfig) { mutableStateOf(apiKeysConfig.searchApiKeyOverride) }
    var elevenKey by remember(apiKeysConfig) { mutableStateOf(apiKeysConfig.elevenLabsKeyOverride) }
    var elevenVoiceOverride by remember(apiKeysConfig) { mutableStateOf(apiKeysConfig.elevenLabsVoiceIdOverride) }

    val voicePresets = listOf(
        Triple("JBFqnCBsd6RMkjVDRZzb", "Classic British Butler", 0.95f),
        Triple("ErXwobaYiN019PkySvjV", "Tactical Commander", 0.88f),
        Triple("EXAVITQu4vr4xnSDxMaL", "Crisp Neural Analyst", 1.05f),
        Triple("TxGEqnHWrfWFTfGW9XjX", "Deep Stark Core", 0.82f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HudPanelSurface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "JARVIS SYSTEM CONFIGURATION",
                    style = MaterialTheme.typography.titleLarge,
                    color = HudCyan,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "VOICE" to "Voice Engine",
                        "AI" to "AI Brain",
                        "API" to "API Config"
                    ).forEach { (id, label) ->
                        FilterChip(
                            selected = activeTab == id,
                            onClick = { activeTab = id },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HudCyan.copy(alpha = 0.25f),
                                selectedLabelColor = HudCyan
                            )
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (activeTab) {
                    "VOICE" -> {
                        Text(
                            text = "ELEVENLABS & ANDROID VOICE ENGINE",
                            style = MaterialTheme.typography.labelLarge,
                            color = HudSkyBlue
                        )
                        Text(
                            text = "Select Voice Persona:",
                            style = MaterialTheme.typography.labelSmall,
                            color = HudMutedText
                        )
                        voicePresets.forEach { (id, name, presetPitch) ->
                            FilterChip(
                                selected = voiceId == id,
                                onClick = {
                                    voiceId = id
                                    voicePersona = "JARVIS // $name"
                                    pitch = presetPitch
                                },
                                label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HudEmerald.copy(alpha = 0.25f),
                                    selectedLabelColor = HudEmerald
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Text(
                            text = "Speaking Speed: ${"%.2f".format(speechRate)}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = HudIceWhite
                        )
                        Slider(
                            value = speechRate,
                            onValueChange = { speechRate = it },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = HudCyan, activeTrackColor = HudCyan)
                        )

                        Text(
                            text = "Voice Pitch: ${"%.2f".format(pitch)}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = HudIceWhite
                        )
                        Slider(
                            value = pitch,
                            onValueChange = { pitch = it },
                            valueRange = 0.6f..1.4f,
                            colors = SliderDefaults.colors(thumbColor = HudCyan, activeTrackColor = HudCyan)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Automatic Speech Playback", style = MaterialTheme.typography.bodyMedium, color = HudIceWhite)
                            Switch(checked = autoSpeak, onCheckedChange = { autoSpeak = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Conversation Voice Mode", style = MaterialTheme.typography.bodyMedium, color = HudIceWhite)
                            Switch(checked = convMode, onCheckedChange = { convMode = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Prefer ElevenLabs Streaming", style = MaterialTheme.typography.bodyMedium, color = HudIceWhite)
                            Switch(checked = preferEleven, onCheckedChange = { preferEleven = it })
                        }

                        OutlinedButton(
                            onClick = {
                                onTestVoice("Good day, Sir. JARVIS Voice Engine calibrated at ${"%.1f".format(speechRate)}x speed.")
                            },
                            border = BorderStroke(1.dp, HudCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Test JARVIS Voice Output", color = HudCyan)
                        }
                    }

                    "AI" -> {
                        Text(
                            text = "JARVIS AI BRAIN ENGINE",
                            style = MaterialTheme.typography.labelLarge,
                            color = HudSkyBlue
                        )
                        listOf(
                            "AUTO" to "Auto (ChatGPT-4o + Gemini 3.8 Flash)",
                            "OPENAI_GPT4O" to "OpenAI ChatGPT-4o Primary",
                            "GEMINI_FLASH" to "Firebase AI Logic (Gemini 3.8 Flash)"
                        ).forEach { (engId, label) ->
                            FilterChip(
                                selected = engine == engId,
                                onClick = { engine = engId },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HudCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = HudCyan
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Configurable JARVIS System Personality:",
                            style = MaterialTheme.typography.labelSmall,
                            color = HudIceWhite
                        )
                        OutlinedTextField(
                            value = personality,
                            onValueChange = { personality = it },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    "API" -> {
                        Text(
                            text = "API KEYS & SECRETS CONFIGURATION",
                            style = MaterialTheme.typography.labelLarge,
                            color = HudSkyBlue
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HudObsidian,
                            border = BorderStroke(1.dp, HudCyanDim.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "• Firebase AI Logic & Firestore: PROVISIONED (Active)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HudEmerald
                                )
                                Text(
                                    text = "• OPENAI_API_KEY: ${if (apiKeysConfig.effectiveOpenAiKey.isNotBlank()) "CONFIGURED" else "Using Gemini 3.8 Flash / Set in Secrets"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (apiKeysConfig.effectiveOpenAiKey.isNotBlank()) HudEmerald else HudAmber
                                )
                                Text(
                                    text = "• ELEVENLABS_API_KEY: ${if (apiKeysConfig.effectiveElevenLabsKey.isNotBlank()) "CONFIGURED" else "Using Android TTS / Set in Secrets"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (apiKeysConfig.effectiveElevenLabsKey.isNotBlank()) HudEmerald else HudAmber
                                )
                                Text(
                                    text = "Tip: Add keys in the AI Studio Secrets panel (.env) or enter session overrides below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HudMutedText
                                )
                            }
                        }

                        OutlinedTextField(
                            value = openAiKey,
                            onValueChange = { openAiKey = it },
                            label = { Text("OPENAI_API_KEY (Optional Override)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = searchKey,
                            onValueChange = { searchKey = it },
                            label = { Text("SEARCH_API_KEY (Optional Override)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = elevenKey,
                            onValueChange = { elevenKey = it },
                            label = { Text("ELEVENLABS_API_KEY (Optional Override)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = elevenVoiceOverride,
                            onValueChange = { elevenVoiceOverride = it },
                            label = { Text("ELEVENLABS_VOICE_ID") },
                            placeholder = { Text("JBFqnCBsd6RMkjVDRZzb") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveVoiceConfig(
                        JarvisVoiceConfig(
                            voiceId = elevenVoiceOverride.ifBlank { voiceId },
                            voicePersonaName = voicePersona,
                            speechRate = speechRate,
                            pitch = pitch,
                            autoSpeakReplies = autoSpeak,
                            conversationVoiceMode = convMode,
                            preferElevenLabs = preferEleven
                        )
                    )
                    onSaveAiSettings(engine, personality)
                    onSaveApiKeys(
                        JarvisApiKeysConfig(
                            openAiKeyOverride = openAiKey,
                            searchApiKeyOverride = searchKey,
                            elevenLabsKeyOverride = elevenKey,
                            elevenLabsVoiceIdOverride = elevenVoiceOverride
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = HudCyan, contentColor = HudObsidian)
            ) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
