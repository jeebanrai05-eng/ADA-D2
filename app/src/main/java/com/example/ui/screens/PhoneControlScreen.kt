package com.example.ui.screens

import android.media.AudioManager
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ActionLogEntry
import com.example.data.model.PendingSystemAction
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisNotificationListenerService
import com.example.service.NotificationSummary
import com.example.service.PhoneControlManager
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
import java.util.UUID

@Composable
fun PhoneControlScreen(
    phoneManager: PhoneControlManager,
    flashlightOn: Boolean,
    hasCameraPermission: Boolean,
    masterStopActive: Boolean,
    emergencyAutomationDisabled: Boolean,
    selectedContactName: String,
    selectedContactPhone: String,
    notifications: List<NotificationSummary>,
    allowedPackages: Set<String>,
    actionHistoryLog: List<ActionLogEntry>,
    onToggleFlashlight: (Boolean) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onUpdateSelectedContact: (name: String, phone: String) -> Unit,
    onRequestConfirmedAction: (PendingSystemAction) -> Unit,
    onTriggerMasterStop: () -> Unit,
    onResetMasterStop: () -> Unit,
    onToggleEmergencyDisable: (Boolean) -> Unit,
    onToggleAllowedNotificationPackage: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    onRecordLog: (title: String, detail: String, status: String) -> Unit
) {
    val context = LocalContext.current
    var battery by remember { mutableStateOf(phoneManager.readBatteryTelemetry()) }
    var volumes by remember { mutableStateOf(phoneManager.readVolumeLevels()) }
    val installedApps = remember { phoneManager.getInstalledLaunchableApps() }
    var appSearchQuery by remember { mutableStateOf("") }

    var dialPhoneInput by remember(selectedContactPhone) { mutableStateOf(selectedContactPhone) }
    var contactNameInput by remember(selectedContactName) { mutableStateOf(selectedContactName) }
    var smsRecipientInput by remember(selectedContactPhone) { mutableStateOf(selectedContactPhone) }
    var smsBodyInput by remember { mutableStateOf("On my way — dispatched via JARVIS AI.") }
    var alarmHourInput by remember { mutableStateOf("07") }
    var alarmMinInput by remember { mutableStateOf("00") }

    val notificationAccessGranted = JarvisNotificationListenerService.isNotificationAccessGranted(context)
    val accessibilityEnabled = JarvisAccessibilityService.isAccessibilityServiceEnabled(context)
    val hasFlashHardware = remember { phoneManager.hasFlashlightHardware() }

    LaunchedEffect(Unit) {
        battery = phoneManager.readBatteryTelemetry()
        volumes = phoneManager.readVolumeLevels()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("phone_control_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        // 0. SECURITY & MASTER STOP CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (masterStopActive || emergencyAutomationDisabled)
                        HudCrimson.copy(alpha = 0.16f) else HudPanelSurface
                ),
                border = BorderStroke(
                    1.dp,
                    if (masterStopActive || emergencyAutomationDisabled) HudCrimson else HudCyan.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GppBad,
                                contentDescription = null,
                                tint = if (masterStopActive) HudCrimson else HudCyan
                            )
                            Text(
                                text = "SECURITY & PRIVACY // MASTER OVERRIDE",
                                style = MaterialTheme.typography.titleMedium,
                                color = HudIceWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        StatusBadge(
                            text = if (masterStopActive) "HALTED" else "PROTECTED",
                            color = if (masterStopActive) HudCrimson else HudEmerald
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (masterStopActive) onResetMasterStop() else onTriggerMasterStop()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (masterStopActive) HudEmerald else HudCrimson,
                                contentColor = HudObsidian
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("master_stop_button")
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (masterStopActive) "RESUME JARVIS SYSTEMS" else "MASTER STOP BUTTON",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Emergency Automation Disable Switch",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HudIceWhite
                            )
                            Text(
                                text = "Immediately locks all accessibility & device automations.",
                                style = MaterialTheme.typography.labelSmall,
                                color = HudMutedText
                            )
                        }
                        Switch(
                            checked = emergencyAutomationDisabled,
                            onCheckedChange = onToggleEmergencyDisable,
                            colors = SwitchDefaults.colors(checkedThumbColor = HudCrimson),
                            modifier = Modifier.testTag("emergency_disable_switch")
                        )
                    }
                }
            }
        }

        // 1. BATTERY STATUS CARD
        item {
            PhoneControlCardFrame(
                title = "1. BATTERY STATUS",
                icon = Icons.Default.BatteryChargingFull,
                badgeText = "AVAILABLE // SYSTEM API",
                badgeColor = HudEmerald
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${battery.percentage}%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (battery.percentage > 20) HudCyan else HudCrimson,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (battery.isCharging) "CHARGING (${battery.powerSource})" else "DISCHARGING (${battery.powerSource})",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (battery.isCharging) HudEmerald else HudSkyBlue
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Temp: ${battery.temperatureC}°C",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HudIceWhite
                        )
                        Text(
                            text = "Potential: ${battery.voltageMv} mV",
                            style = MaterialTheme.typography.labelSmall,
                            color = HudMutedText
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            battery = phoneManager.readBatteryTelemetry()
                            onRecordLog("Battery Telemetry Read", "${battery.percentage}% (${battery.powerSource})", "EXECUTED")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Refresh")
                    }
                    Button(
                        onClick = {
                            battery = phoneManager.readBatteryTelemetry()
                            val msg = "Sir, your battery is at ${battery.percentage} percent, ${if (battery.isCharging) "currently charging via ${battery.powerSource}" else "running on battery cell"} at ${battery.temperatureC} degrees Celsius."
                            onSpeakText(msg)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HudCyan, contentColor = HudObsidian),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Speak Status")
                    }
                }
            }
        }

        // 2. FLASHLIGHT CONTROL CARD
        item {
            PhoneControlCardFrame(
                title = "2. FLASHLIGHT CONTROL",
                icon = if (flashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                badgeText = when {
                    !hasFlashHardware -> "NO FLASH HARDWARE"
                    hasCameraPermission -> "CAMERA PERMISSION: GRANTED"
                    else -> "CAMERA PERMISSION: REQUIRED"
                },
                badgeColor = when {
                    !hasFlashHardware -> HudCrimson
                    hasCameraPermission -> HudEmerald
                    else -> HudAmber
                }
            ) {
                Text(
                    text = "Controls the rear LED torch using Android CameraManager.setTorchMode().",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HudMutedText
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!hasCameraPermission) {
                        Button(
                            onClick = onRequestCameraPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = HudAmber, contentColor = HudObsidian),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Camera Permission for Flashlight")
                        }
                    } else {
                        Button(
                            onClick = { onToggleFlashlight(!flashlightOn) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (flashlightOn) HudAmber else HudCyan,
                                contentColor = HudObsidian
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("flashlight_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (flashlightOn) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (flashlightOn) "TURN FLASHLIGHT OFF" else "TURN FLASHLIGHT ON")
                        }
                    }
                }
            }
        }

        // 3. VOLUME & AUDIO CONTROL CARD
        item {
            PhoneControlCardFrame(
                title = "3. VOLUME & AUDIO STREAMS",
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                badgeText = "AVAILABLE // AUDIOMANAGER",
                badgeColor = HudEmerald
            ) {
                VolumeSliderRow(
                    label = "Media Volume (${volumes.mediaCurrent}/${volumes.mediaMax})",
                    value = volumes.mediaCurrent.toFloat(),
                    max = volumes.mediaMax.toFloat(),
                    onValueChange = {
                        volumes = phoneManager.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt())
                    }
                )
                VolumeSliderRow(
                    label = "Ring Volume (${volumes.ringCurrent}/${volumes.ringMax})",
                    value = volumes.ringCurrent.toFloat(),
                    max = volumes.ringMax.toFloat(),
                    onValueChange = {
                        volumes = phoneManager.setStreamVolume(AudioManager.STREAM_RING, it.toInt())
                    }
                )
                VolumeSliderRow(
                    label = "Alarm Volume (${volumes.alarmCurrent}/${volumes.alarmMax})",
                    value = volumes.alarmCurrent.toFloat(),
                    max = volumes.alarmMax.toFloat(),
                    onValueChange = {
                        volumes = phoneManager.setStreamVolume(AudioManager.STREAM_ALARM, it.toInt())
                    }
                )
            }
        }

        // 4. MEDIA PLAYBACK CONTROL CARD
        item {
            PhoneControlCardFrame(
                title = "4. MEDIA PLAYBACK CONTROL",
                icon = Icons.Default.PlayArrow,
                badgeText = "AVAILABLE // MEDIA KEY DISPATCH",
                badgeColor = HudEmerald
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            phoneManager.dispatchMediaCommand("PREVIOUS")
                            onRecordLog("Media Control", "Dispatched PREVIOUS track key", "EXECUTED")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                    }
                    Button(
                        onClick = {
                            phoneManager.dispatchMediaCommand("PLAY_PAUSE")
                            onRecordLog("Media Control", "Dispatched PLAY/PAUSE key", "EXECUTED")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HudCyan, contentColor = HudObsidian),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play/Pause", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            phoneManager.dispatchMediaCommand("NEXT")
                            onRecordLog("Media Control", "Dispatched NEXT track key", "EXECUTED")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next")
                    }
                }
            }
        }

        // 5. APPLICATIONS CONTROL CARD
        item {
            PhoneControlCardFrame(
                title = "5. APPLICATION LAUNCHER (${installedApps.size} APPS)",
                icon = Icons.Default.Apps,
                badgeText = "AVAILABLE // INTENT SYSTEM",
                badgeColor = HudEmerald
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("YouTube", "WhatsApp", "Chrome", "Settings").forEach { quickApp ->
                        Button(
                            onClick = {
                                val res = phoneManager.launchAppByPackageOrName(quickApp)
                                onRecordLog(
                                    "Launch $quickApp",
                                    res.getOrElse { it.localizedMessage ?: "Failed" },
                                    if (res.isSuccess) "EXECUTED" else "BLOCKED"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HudPanelElevated,
                                contentColor = HudCyan
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Open $quickApp", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = appSearchQuery,
                    onValueChange = { appSearchQuery = it },
                    placeholder = { Text("Filter installed apps or enter package/app name…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val filteredApps = remember(installedApps, appSearchQuery) {
                    if (appSearchQuery.isBlank()) installedApps.take(8)
                    else installedApps.filter {
                        it.appName.contains(appSearchQuery, ignoreCase = true) ||
                            it.packageName.contains(appSearchQuery, ignoreCase = true)
                    }.take(8)
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    filteredApps.forEach { app ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HudObsidian,
                            border = BorderStroke(1.dp, HudCyanDim.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = HudIceWhite,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HudMutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { phoneManager.openAppDetailsSettings(app.packageName) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Info", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Button(
                                        onClick = {
                                            phoneManager.launchAppByPackageOrName(app.packageName)
                                            onRecordLog("App Launch", "Opened ${app.appName}", "EXECUTED")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = HudCyan, contentColor = HudObsidian),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Open", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. NOTIFICATIONS ASSISTANT CARD
        item {
            PhoneControlCardFrame(
                title = "6. NOTIFICATION ASSISTANT",
                icon = Icons.Default.NotificationsActive,
                badgeText = if (notificationAccessGranted) "LISTENER: ENABLED" else "PERMISSION REQUIRED",
                badgeColor = if (notificationAccessGranted) HudEmerald else HudAmber
            ) {
                Text(
                    text = "JARVIS reads notification summaries only when explicitly enabled in Android Notification Listener Settings. Never collects notification data secretly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HudMutedText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { JarvisNotificationListenerService.openNotificationListenerSettings(context) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (notificationAccessGranted) "Configure Access" else "Enable Notification Access",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Button(
                        onClick = {
                            if (notifications.isEmpty()) {
                                onSpeakText("Sir, there are currently no unread notification summaries in the queue.")
                            } else {
                                val summary = "Sir, you have ${notifications.size} notifications. " +
                                    notifications.take(4).joinToString(". ") { "From ${it.appLabel}: ${it.title}, ${it.text}" }
                                onSpeakText(summary)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HudCyan, contentColor = HudObsidian),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Read Aloud (${notifications.size})", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // App Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "com.whatsapp" to "WhatsApp",
                        "com.google.android.gm" to "Gmail",
                        "com.google.android.apps.messaging" to "Messages",
                        "com.google.android.youtube" to "YouTube"
                    ).forEach { (pkg, label) ->
                        FilterChip(
                            selected = pkg in allowedPackages,
                            onClick = { onToggleAllowedNotificationPackage(pkg) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HudCyan.copy(alpha = 0.25f),
                                selectedLabelColor = HudCyan
                            )
                        )
                    }
                }

                if (notifications.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        notifications.take(5).forEach { notif ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = HudObsidian,
                                border = BorderStroke(1.dp, HudSkyBlue.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "[${notif.appLabel.uppercase()}] ${notif.title}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HudCyan
                                    )
                                    Text(
                                        text = notif.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = HudIceWhite,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. CALLS CARD (ACTION_DIAL + CONFIRMATION)
        item {
            PhoneControlCardFrame(
                title = "7. CALLS (ACTION_DIAL // CONFIRMED)",
                icon = Icons.Default.Call,
                badgeText = "AVAILABLE // CONFIRMATION REQUIRED",
                badgeColor = HudAmber
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = contactNameInput,
                        onValueChange = { contactNameInput = it },
                        label = { Text("Selected Contact Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dialPhoneInput,
                        onValueChange = { dialPhoneInput = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onUpdateSelectedContact(contactNameInput, dialPhoneInput) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Default Contact", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = {
                            onRequestConfirmedAction(
                                PendingSystemAction(
                                    id = UUID.randomUUID().toString(),
                                    actionType = "CALL_DIAL",
                                    title = "Initiate Call via ACTION_DIAL",
                                    description = "Open Phone Dialer for $contactNameInput ($dialPhoneInput)",
                                    targetOrQuery = dialPhoneInput,
                                    secondaryParam = contactNameInput,
                                    requiresConfirmation = true
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HudEmerald, contentColor = HudObsidian),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("initiate_call_button")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dial Contact", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 8. MESSAGES & ANDROID SHARESHEET CARD
        item {
            PhoneControlCardFrame(
                title = "8. MESSAGES, WHATSAPP & SHARESHEET",
                icon = Icons.AutoMirrored.Filled.Message,
                badgeText = "AVAILABLE // CONFIRMATION REQUIRED",
                badgeColor = HudAmber
            ) {
                OutlinedTextField(
                    value = smsRecipientInput,
                    onValueChange = { smsRecipientInput = it },
                    label = { Text("Recipient Phone / Contact") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = smsBodyInput,
                    onValueChange = { smsBodyInput = it },
                    label = { Text("Message Payload") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onRequestConfirmedAction(
                                PendingSystemAction(
                                    id = UUID.randomUUID().toString(),
                                    actionType = "SEND_SMS",
                                    title = "Compose SMS in Default Messaging App",
                                    description = "To: $smsRecipientInput // Msg: \"$smsBodyInput\"",
                                    targetOrQuery = smsRecipientInput,
                                    secondaryParam = smsBodyInput,
                                    requiresConfirmation = true
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HudCyan, contentColor = HudObsidian),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Send SMS", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = {
                            onRequestConfirmedAction(
                                PendingSystemAction(
                                    id = UUID.randomUUID().toString(),
                                    actionType = "WHATSAPP_SEND",
                                    title = "Dispatch via WhatsApp",
                                    description = "To: $smsRecipientInput // Msg: \"$smsBodyInput\"",
                                    targetOrQuery = smsRecipientInput,
                                    secondaryParam = smsBodyInput,
                                    requiresConfirmation = true
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HudEmerald, contentColor = HudObsidian),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("WhatsApp", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            phoneManager.shareTextViaSharesheet(smsBodyInput)
                            onRecordLog("Android Sharesheet", "Shared text payload", "EXECUTED")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 9. DEVICE SETTINGS & ALARMS CARD
        item {
            PhoneControlCardFrame(
                title = "9. DEVICE SETTINGS & SYSTEM ALARM",
                icon = Icons.Default.Settings,
                badgeText = "AVAILABLE // SYSTEM INTENTS",
                badgeColor = HudEmerald
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Wi-Fi" to "WIFI",
                        "Bluetooth" to "BLUETOOTH",
                        "Display" to "DISPLAY",
                        "Sound" to "SOUND",
                        "All Settings" to "MAIN"
                    ).forEach { (label, section) ->
                        OutlinedButton(
                            onClick = {
                                phoneManager.openSystemSettings(section)
                                onRecordLog("System Settings", "Opened $label Settings", "EXECUTED")
                            }
                        ) {
                            Icon(
                                imageVector = when (section) {
                                    "WIFI" -> Icons.Default.Wifi
                                    "BLUETOOTH" -> Icons.Default.Bluetooth
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Alarm row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = alarmHourInput,
                        onValueChange = { alarmHourInput = it },
                        label = { Text("Hour (0-23)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = alarmMinInput,
                        onValueChange = { alarmMinInput = it },
                        label = { Text("Min (0-59)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val h = alarmHourInput.toIntOrNull() ?: 7
                            val m = alarmMinInput.toIntOrNull() ?: 0
                            phoneManager.setSystemAlarm(h, m, "JARVIS Alarm")
                            onRecordLog("Set Alarm", "Configured alarm for %02d:%02d".format(h, m), "EXECUTED")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HudCyan, contentColor = HudObsidian),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set Alarm", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 10. OPTIONAL ACCESSIBILITY AUTOMATION & ACTION HISTORY LOG
        item {
            PhoneControlCardFrame(
                title = "10. ACCESSIBILITY WORKFLOW & AUDIT LOG",
                icon = Icons.Default.AccessibilityNew,
                badgeText = if (accessibilityEnabled) "SERVICE: ENABLED" else "OPTIONAL // DISABLED",
                badgeColor = if (accessibilityEnabled) HudEmerald else HudSkyBlue
            ) {
                Text(
                    text = "Why Accessibility is optional: JARVIS uses AccessibilityService ONLY when you explicitly enable it to perform permitted global navigation (Back, Home, Recents, Quick Notifications) by voice. It never bypasses Android security or reads private data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HudMutedText
                )

                OutlinedButton(
                    onClick = { JarvisAccessibilityService.openAccessibilitySettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (accessibilityEnabled) "Manage Accessibility Service in Android Settings"
                        else "Enable Optional JARVIS Accessibility Service"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("BACK", "HOME", "RECENTS", "NOTIFICATIONS").forEach { act ->
                        OutlinedButton(
                            onClick = {
                                val res = JarvisAccessibilityService.performPermittedAction(act)
                                onRecordLog(
                                    "Accessibility: $act",
                                    res.getOrElse { it.localizedMessage ?: "Not enabled" },
                                    if (res.isSuccess) "EXECUTED" else "BLOCKED"
                                )
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(act, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = HudCyan, modifier = Modifier.size(16.dp))
                    Text(
                        text = "ACTION & PERMISSION AUDIT LOG (${actionHistoryLog.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = HudCyan
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    actionHistoryLog.take(8).forEach { log ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HudObsidian,
                            border = BorderStroke(
                                1.dp,
                                when (log.status) {
                                    "EXECUTED" -> HudEmerald.copy(alpha = 0.4f)
                                    "BLOCKED" -> HudCrimson.copy(alpha = 0.5f)
                                    else -> HudAmber.copy(alpha = 0.5f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.actionTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HudIceWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = log.detail,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = HudMutedText,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                StatusBadge(
                                    text = log.status,
                                    color = when (log.status) {
                                        "EXECUTED" -> HudEmerald
                                        "BLOCKED" -> HudCrimson
                                        else -> HudAmber
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneControlCardFrame(
    title: String,
    icon: ImageVector,
    badgeText: String,
    badgeColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HudPanelSurface),
        border = BorderStroke(1.dp, HudCyanDim.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = HudCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = HudCyan,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusBadge(text = badgeText, color = badgeColor)
            }
            content()
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.7f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun VolumeSliderRow(
    label: String,
    value: Float,
    max: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = HudIceWhite
        )
        Slider(
            value = value.coerceIn(0f, max),
            onValueChange = onValueChange,
            valueRange = 0f..max,
            colors = SliderDefaults.colors(
                thumbColor = HudCyan,
                activeTrackColor = HudCyan,
                inactiveTrackColor = HudDeepNavy
            )
        )
    }
}
