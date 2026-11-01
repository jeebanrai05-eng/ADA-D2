package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PendingSystemAction
import com.example.data.model.SmartDevice
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanDim
import com.example.ui.theme.HudEmerald
import com.example.ui.theme.HudIceWhite
import com.example.ui.theme.HudMutedText
import com.example.ui.theme.HudObsidian
import com.example.ui.theme.HudPanelElevated
import com.example.ui.theme.HudPanelSurface
import com.example.ui.theme.HudSkyBlue
import com.example.viewmodel.DataUiState
import java.util.UUID

@Composable
fun SystemsKasaScreen(
    smartDevicesState: DataUiState<List<SmartDevice>>,
    activeProject: String,
    onSelectProject: (String) -> Unit,
    onToggleDevice: (SmartDevice) -> Unit,
    onBrightnessChange: (SmartDevice, Long) -> Unit,
    onApplyScene: (String) -> Unit,
    onAddDevice: (String, String, String, String) -> Unit,
    onTriggerSystemAction: (PendingSystemAction) -> Unit
) {
    var youtubeQuery by remember { mutableStateOf("") }
    var whatsappRecipient by remember { mutableStateOf("") }
    var whatsappMessage by remember { mutableStateOf("") }
    var webAgentQuery by remember { mutableStateOf("") }
    var showAddDeviceDialog by remember { mutableStateOf(false) }

    val devices = (smartDevicesState as? DataUiState.Success)?.data.orEmpty()
    val totalWatts = devices.filter { it.isOn }.sumOf { it.powerWatts }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active Project Context Switcher
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HudPanelSurface),
            border = BorderStroke(1.dp, HudCyan.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PROJECT WORKSPACE // $activeProject",
                    style = MaterialTheme.typography.titleLarge,
                    color = HudCyan
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val projects = listOf(
                        "MARK II // CORE LAB",
                        "EXOSKELETON // V4",
                        "AUTONOMOUS DRONE SWARM",
                        "HOME AUTOMATION MATRIX"
                    )
                    projects.forEach { proj ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (activeProject == proj) HudCyan.copy(alpha = 0.25f) else HudPanelElevated,
                            border = BorderStroke(1.dp, if (activeProject == proj) HudCyan else HudCyanDim.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { onSelectProject(proj) }
                        ) {
                            Text(
                                text = proj,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeProject == proj) HudCyan else HudIceWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Kasa Smart Home IoT Matrix
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HudPanelSurface),
            border = BorderStroke(1.dp, HudSkyBlue.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("kasa_iot_matrix_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = HudAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "KASA SMART HOME IOT MATRIX",
                                style = MaterialTheme.typography.titleLarge,
                                color = HudIceWhite
                            )
                            Text(
                                text = "Active Load: ${totalWatts}W across ${devices.count { it.isOn }}/${devices.size} nodes",
                                style = MaterialTheme.typography.labelSmall,
                                color = HudEmerald
                            )
                        }
                    }

                    IconButton(
                        onClick = { showAddDeviceDialog = true },
                        modifier = Modifier.testTag("add_kasa_device_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Kasa Device", tint = HudCyan)
                    }
                }

                // Scene Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val scenes = listOf(
                        "FOCUS" to "Focus Protocol",
                        "FULL_POWER" to "Full Illumination",
                        "NIGHT_LAB" to "Night Workshop",
                        "LOCKDOWN" to "Lab Lockdown"
                    )
                    scenes.forEach { (id, label) ->
                        OutlinedButton(
                            onClick = { onApplyScene(id) },
                            border = BorderStroke(1.dp, HudCyan.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("kasa_scene_$id")
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = HudCyan)
                        }
                    }
                }

                devices.forEach { device ->
                    SmartDeviceRow(
                        device = device,
                        onToggle = { onToggleDevice(device) },
                        onBrightness = { b -> onBrightnessChange(device, b) }
                    )
                }
            }
        }

        // System Control: YouTube Media & WhatsApp Communication & Web Agent
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HudPanelSurface),
            border = BorderStroke(1.dp, HudCyan.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("system_agent_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SYSTEM CONTROL & MEDIA AGENT",
                    style = MaterialTheme.typography.titleLarge,
                    color = HudCyan
                )

                // 1. YouTube Direct Playback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = youtubeQuery,
                        onValueChange = { youtubeQuery = it },
                        leadingIcon = {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = HudAmber)
                        },
                        placeholder = { Text("YouTube video or music query…", color = HudMutedText.copy(alpha = 0.6f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HudAmber,
                            unfocusedBorderColor = HudCyanDim.copy(alpha = 0.4f),
                            focusedContainerColor = HudObsidian,
                            unfocusedContainerColor = HudObsidian
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("youtube_query_input")
                    )

                    Button(
                        onClick = {
                            val q = youtubeQuery.ifBlank { "Iron Man Jarvis HUD Soundtrack" }
                            onTriggerSystemAction(
                                PendingSystemAction(
                                    id = UUID.randomUUID().toString(),
                                    actionType = "YOUTUBE_PLAY",
                                    title = "YouTube Media Playback",
                                    description = "Launch YouTube search/playback for: \"$q\"",
                                    targetOrQuery = q
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HudAmber,
                            contentColor = HudObsidian
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("youtube_play_button")
                    ) {
                        Text("Play YT", style = MaterialTheme.typography.labelLarge)
                    }
                }

                // 2. WhatsApp Message Dispatcher
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "WHATSAPP DIRECT DISPATCHER",
                        style = MaterialTheme.typography.labelSmall,
                        color = HudEmerald
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = whatsappRecipient,
                            onValueChange = { whatsappRecipient = it },
                            placeholder = { Text("Contact / Phone", color = HudMutedText.copy(alpha = 0.6f)) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HudEmerald,
                                unfocusedBorderColor = HudCyanDim.copy(alpha = 0.4f),
                                focusedContainerColor = HudObsidian,
                                unfocusedContainerColor = HudObsidian
                            ),
                            modifier = Modifier
                                .weight(0.4f)
                                .testTag("whatsapp_recipient_input")
                        )

                        OutlinedTextField(
                            value = whatsappMessage,
                            onValueChange = { whatsappMessage = it },
                            placeholder = { Text("Message payload…", color = HudMutedText.copy(alpha = 0.6f)) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HudEmerald,
                                unfocusedBorderColor = HudCyanDim.copy(alpha = 0.4f),
                                focusedContainerColor = HudObsidian,
                                unfocusedContainerColor = HudObsidian
                            ),
                            modifier = Modifier
                                .weight(0.6f)
                                .testTag("whatsapp_message_input")
                        )
                    }

                    Button(
                        onClick = {
                            val target = whatsappRecipient.ifBlank { "Engineering Team" }
                            val msg = whatsappMessage.ifBlank { "ADA v2 Telemetry Check: All systems nominal." }
                            onTriggerSystemAction(
                                PendingSystemAction(
                                    id = UUID.randomUUID().toString(),
                                    actionType = "WHATSAPP_SEND",
                                    title = "WhatsApp Message Dispatch",
                                    description = "To: $target // Message: \"$msg\"",
                                    targetOrQuery = target,
                                    secondaryParam = msg
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HudEmerald,
                            contentColor = HudObsidian
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("whatsapp_dispatch_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dispatch WhatsApp Intent", style = MaterialTheme.typography.labelLarge)
                    }
                }

                // 3. Web Agent Research Launcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = webAgentQuery,
                        onValueChange = { webAgentQuery = it },
                        leadingIcon = {
                            Icon(Icons.Default.Language, contentDescription = null, tint = HudSkyBlue)
                        },
                        placeholder = { Text("Web Agent URL or research query…", color = HudMutedText.copy(alpha = 0.6f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HudSkyBlue,
                            unfocusedBorderColor = HudCyanDim.copy(alpha = 0.4f),
                            focusedContainerColor = HudObsidian,
                            unfocusedContainerColor = HudObsidian
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("web_agent_query_input")
                    )

                    Button(
                        onClick = {
                            val q = webAgentQuery.ifBlank { "https://developer.android.com" }
                            onTriggerSystemAction(
                                PendingSystemAction(
                                    id = UUID.randomUUID().toString(),
                                    actionType = "WEB_SEARCH",
                                    title = "Web Agent Research",
                                    description = "Open Web Agent for: $q",
                                    targetOrQuery = q
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HudSkyBlue,
                            contentColor = HudObsidian
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("web_agent_launch_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showAddDeviceDialog) {
        AddSmartDeviceDialog(
            onDismiss = { showAddDeviceDialog = false },
            onConfirm = { name, room, type, ip ->
                onAddDevice(name, room, type, ip)
                showAddDeviceDialog = false
            }
        )
    }
}

@Composable
private fun SmartDeviceRow(
    device: SmartDevice,
    onToggle: () -> Unit,
    onBrightness: (Long) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = HudObsidian.copy(alpha = 0.7f),
        border = BorderStroke(
            1.dp,
            if (device.isOn) HudCyan.copy(alpha = 0.6f) else HudCyanDim.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_device_card_${device.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = if (device.isOn) HudCyan else HudMutedText.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = HudIceWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${device.room} // ${device.ipAddress} // ${device.powerWatts}W",
                            style = MaterialTheme.typography.labelSmall,
                            color = HudMutedText
                        )
                    }
                }

                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.testTag("toggle_device_${device.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Toggle power",
                        tint = if (device.isOn) HudEmerald else HudMutedText
                    )
                }
            }

            Slider(
                value = device.brightness.toFloat(),
                onValueChange = { onBrightness(it.toLong()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = HudCyan,
                    activeTrackColor = HudCyan,
                    inactiveTrackColor = HudPanelElevated
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AddSmartDeviceDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("Main Lab") }
    var ip by remember { mutableStateOf("192.168.1.120") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HudPanelSurface,
        title = {
            Text("Link Kasa Smart Node", style = MaterialTheme.typography.titleLarge, color = HudCyan)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room / Sector") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("Local IP Address") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, room, "strip", ip)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HudCyan, contentColor = HudObsidian)
            ) {
                Text("Link Node")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
