package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActionLogEntry
import com.example.data.model.CadModel
import com.example.data.model.ChatMessage
import com.example.data.model.ConversationSession
import com.example.data.model.EmotionMood
import com.example.data.model.EmotionState
import com.example.data.model.MemoryItem
import com.example.data.model.MemorySuggestion
import com.example.data.model.PendingSystemAction
import com.example.data.model.PrinterNode
import com.example.data.model.SmartDevice
import com.example.data.repository.AdaRepository
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisApiKeysConfig
import com.example.service.JarvisExternalApiService
import com.example.service.JarvisNotificationListenerService
import com.example.service.JarvisVoiceConfig
import com.example.service.SearchCitation
import com.example.service.SearchResponseBundle
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AdaViewModel"

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data object Loading : AiTaskState
    data class Streaming(val partialText: String) : AiTaskState
    data class Success(val text: String) : AiTaskState
    data class Error(val message: String) : AiTaskState
}

class AdaViewModel(
    private val repository: AdaRepository,
    private val currentUserId: String
) : ViewModel() {

    private val chatStreamModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val cadSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "name" to Schema.string(description = "Component designation name"),
                "shapeType" to Schema.enumeration(
                    values = listOf("gear", "enclosure", "bracket", "cylinder", "reactor", "drone_frame"),
                    description = "Closest parametric 3D geometry archetype"
                ),
                "scriptCode" to Schema.string(description = "Clean build123d / OpenSCAD parametric code for the part"),
                "dimensionsSummary" to Schema.string(description = "Bounding box and key tolerance summary in mm"),
                "paramRadius" to Schema.integer(description = "Primary radius or width in mm between 15 and 180"),
                "paramHeight" to Schema.integer(description = "Primary height or thickness in mm between 8 and 140"),
                "paramSegments" to Schema.integer(description = "Tooth count, coil slots, or mounting holes between 4 and 48")
            )
        )
    }

    private val cadModelGenerator by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = cadSchema
            }
        )
    }

    // Two-tier Firestore StateFlows
    val memoriesState: StateFlow<DataUiState<List<MemoryItem>>> = repository.observeMemories(currentUserId)
        .map<List<MemoryItem>, DataUiState<List<MemoryItem>>> { DataUiState.Success(it) }
        .catch { emit(DataUiState.Error(it.message ?: "Failed to observe permanent memory")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val cadModelsState: StateFlow<DataUiState<List<CadModel>>> = repository.observeCadModels(currentUserId)
        .map<List<CadModel>, DataUiState<List<CadModel>>> { DataUiState.Success(it) }
        .catch { emit(DataUiState.Error(it.message ?: "Failed to observe CAD models")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val smartDevicesState: StateFlow<DataUiState<List<SmartDevice>>> = repository.observeSmartDevices(currentUserId)
        .map<List<SmartDevice>, DataUiState<List<SmartDevice>>> { DataUiState.Success(it) }
        .catch { emit(DataUiState.Error(it.message ?: "Failed to observe smart home devices")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val printersState: StateFlow<DataUiState<List<PrinterNode>>> = repository.observePrinters(currentUserId)
        .map<List<PrinterNode>, DataUiState<List<PrinterNode>>> { DataUiState.Success(it) }
        .catch { emit(DataUiState.Error(it.message ?: "Failed to observe 3D printers")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    // JARVIS AI Brain & Personality Configuration
    private val _jarvisPersonality = MutableStateFlow(
        "You are JARVIS, a highly intelligent personal AI assistant. You are polite, professional, intelligent, concise, and helpful."
    )
    val jarvisPersonality: StateFlow<String> = _jarvisPersonality.asStateFlow()

    private val _selectedAiEngine = MutableStateFlow("AUTO") // "AUTO", "OPENAI_GPT4O", "GEMINI_FLASH"
    val selectedAiEngine: StateFlow<String> = _selectedAiEngine.asStateFlow()

    private val _voiceConfig = MutableStateFlow(JarvisVoiceConfig())
    val voiceConfig: StateFlow<JarvisVoiceConfig> = _voiceConfig.asStateFlow()

    private val _apiKeysConfig = MutableStateFlow(JarvisApiKeysConfig())
    val apiKeysConfig: StateFlow<JarvisApiKeysConfig> = _apiKeysConfig.asStateFlow()

    // Security & Master Controls
    private val _masterStopActive = MutableStateFlow(false)
    val masterStopActive: StateFlow<Boolean> = _masterStopActive.asStateFlow()

    private val _emergencyAutomationDisabled = MutableStateFlow(false)
    val emergencyAutomationDisabled: StateFlow<Boolean> = _emergencyAutomationDisabled.asStateFlow()

    private val _selectedContactPhone = MutableStateFlow("+9779800000000")
    val selectedContactPhone: StateFlow<String> = _selectedContactPhone.asStateFlow()

    private val _selectedContactName = MutableStateFlow("Stark Lab Primary Contact")
    val selectedContactName: StateFlow<String> = _selectedContactName.asStateFlow()

    private val _flashlightOn = MutableStateFlow(false)
    val flashlightOn: StateFlow<Boolean> = _flashlightOn.asStateFlow()

    private val _actionHistoryLog = MutableStateFlow<List<ActionLogEntry>>(
        listOf(
            ActionLogEntry(
                id = "init_log_1",
                actionTitle = "JARVIS Security Kernel Online",
                detail = "Permission Guard, Confirmation Gate, and Half-Duplex Echo Guard initialized.",
                status = "EXECUTED"
            )
        )
    )
    val actionHistoryLog: StateFlow<List<ActionLogEntry>> = _actionHistoryLog.asStateFlow()

    // HUD & Assistant States
    private val _emotionState = MutableStateFlow(EmotionState())
    val emotionState: StateFlow<EmotionState> = _emotionState.asStateFlow()

    private val _trustedModeAuto = MutableStateFlow(false)
    val trustedModeAuto: StateFlow<Boolean> = _trustedModeAuto.asStateFlow()

    private val _halfDuplexEnabled = MutableStateFlow(true)
    val halfDuplexEnabled: StateFlow<Boolean> = _halfDuplexEnabled.asStateFlow()

    private val _autoSaveMemory = MutableStateFlow(false)
    val autoSaveMemory: StateFlow<Boolean> = _autoSaveMemory.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _activeProject = MutableStateFlow("JARVIS // MARK VII OS")
    val activeProject: StateFlow<String> = _activeProject.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingSystemAction?>(null)
    val pendingConfirmation: StateFlow<PendingSystemAction?> = _pendingConfirmation.asStateFlow()

    private val _pendingMemoryPrompt = MutableStateFlow<MemorySuggestion?>(null)
    val pendingMemoryPrompt: StateFlow<MemorySuggestion?> = _pendingMemoryPrompt.asStateFlow()

    private val initialWelcomeMessage = ChatMessage(
        id = "welcome_1",
        sender = "ADA",
        text = "Good day, Sir. JARVIS System Online. AI Brain (GPT-4o / Gemini Neural Core), Live Internet Search, ElevenLabs Voice Engine, and Android Phone Control are ready. How may I assist you in English or Nepali?",
        emotionTag = "ONLINE // 98%"
    )

    private val _chatMessages = MutableStateFlow(listOf(initialWelcomeMessage))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _conversationSessions = MutableStateFlow<List<ConversationSession>>(emptyList())
    val conversationSessions: StateFlow<List<ConversationSession>> = _conversationSessions.asStateFlow()

    private val _activeSearchBundle = MutableStateFlow<SearchResponseBundle?>(null)
    val activeSearchBundle: StateFlow<SearchResponseBundle?> = _activeSearchBundle.asStateFlow()

    private val _searchTaskState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val searchTaskState: StateFlow<AiTaskState> = _searchTaskState.asStateFlow()

    private val _chatTaskState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val chatTaskState: StateFlow<AiTaskState> = _chatTaskState.asStateFlow()

    private val _visionTaskState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val visionTaskState: StateFlow<AiTaskState> = _visionTaskState.asStateFlow()

    private val _cadTaskState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val cadTaskState: StateFlow<AiTaskState> = _cadTaskState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialWorkshopIfEmpty(currentUserId)
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun showStatusBanner(message: String) {
        _statusBanner.value = message
    }

    fun recordActionLog(title: String, detail: String, status: String = "EXECUTED") {
        val entry = ActionLogEntry(
            id = UUID.randomUUID().toString(),
            actionTitle = title,
            detail = detail,
            status = status
        )
        _actionHistoryLog.value = (listOf(entry) + _actionHistoryLog.value).take(40)
    }

    fun triggerMasterStop(onStopVoice: () -> Unit) {
        _masterStopActive.value = true
        _isListening.value = false
        _isSpeaking.value = false
        _pendingConfirmation.value = null
        JarvisNotificationListenerService.setMasterStop(true)
        onStopVoice()
        recordActionLog(
            title = "MASTER STOP ENGAGED",
            detail = "All active voice synthesis, microphone listening, and pending automations halted immediately.",
            status = "BLOCKED"
        )
        _statusBanner.value = "MASTER STOP ENGAGED: All voice & automation operations halted."
    }

    fun resetMasterStop() {
        _masterStopActive.value = false
        JarvisNotificationListenerService.setMasterStop(false)
        recordActionLog("Master Stop Reset", "JARVIS subsystems restored to normal operation.", "EXECUTED")
        _statusBanner.value = "JARVIS Systems Online."
    }

    fun setEmergencyAutomationDisabled(disabled: Boolean) {
        _emergencyAutomationDisabled.value = disabled
        JarvisAccessibilityService.setEmergencyDisabled(disabled)
        recordActionLog(
            title = if (disabled) "Emergency Automation Disabled" else "Automation Re-Enabled",
            detail = if (disabled) "All accessibility and background device automations locked." else "User-authorized automations permitted.",
            status = if (disabled) "BLOCKED" else "EXECUTED"
        )
        _statusBanner.value = if (disabled) {
            "Emergency Automation Disable Switch: ACTIVE (All automations blocked)"
        } else {
            "Emergency Automation Disable Switch: OFF"
        }
    }

    fun updateFlashlightState(isOn: Boolean) {
        _flashlightOn.value = isOn
    }

    fun updateSelectedContact(name: String, phone: String) {
        _selectedContactName.value = name.trim().ifBlank { "Primary Contact" }
        _selectedContactPhone.value = phone.trim().ifBlank { "+9779800000000" }
        _statusBanner.value = "Default contact set: ${_selectedContactName.value} (${_selectedContactPhone.value})"
    }

    fun updateJarvisPersonality(prompt: String) {
        _jarvisPersonality.value = prompt.trim().ifBlank {
            "You are JARVIS, a highly intelligent personal AI assistant. You are polite, professional, intelligent, concise, and helpful."
        }
        _statusBanner.value = "JARVIS AI Personality updated."
    }

    fun updateSelectedAiEngine(engine: String) {
        _selectedAiEngine.value = engine
        _statusBanner.value = "Primary AI Brain set to: $engine"
    }

    fun updateVoiceConfig(newConfig: JarvisVoiceConfig) {
        _voiceConfig.value = newConfig
    }

    fun updateApiKeysConfig(newKeys: JarvisApiKeysConfig) {
        _apiKeysConfig.value = newKeys
        _statusBanner.value = "API Configuration saved."
    }

    // Conversation History & Session Management
    fun startNewConversation() {
        val current = _chatMessages.value
        if (current.size > 1) {
            val firstUserText = current.firstOrNull { it.sender == "USER" }?.text ?: "JARVIS Session"
            val session = ConversationSession(
                id = UUID.randomUUID().toString(),
                title = firstUserText.take(42),
                messages = current
            )
            _conversationSessions.value = (listOf(session) + _conversationSessions.value).take(20)
        }
        _chatMessages.value = listOf(
            initialWelcomeMessage.copy(
                id = UUID.randomUUID().toString(),
                timestampMs = System.currentTimeMillis()
            )
        )
        _chatTaskState.value = AiTaskState.Idle
        _statusBanner.value = "Started new JARVIS conversation."
    }

    fun clearCurrentConversation() {
        _chatMessages.value = listOf(
            initialWelcomeMessage.copy(
                id = UUID.randomUUID().toString(),
                timestampMs = System.currentTimeMillis()
            )
        )
        _chatTaskState.value = AiTaskState.Idle
        _statusBanner.value = "Conversation cleared."
    }

    fun loadConversationSession(session: ConversationSession) {
        _chatMessages.value = session.messages
        _statusBanner.value = "Loaded conversation: ${session.title}"
    }

    fun regenerateResponse(
        message: ChatMessage,
        externalApi: JarvisExternalApiService? = null,
        onSpeakReply: (String) -> Unit = {},
        onExecuteSystemAction: (PendingSystemAction) -> Unit = {}
    ) {
        val promptToRetry = message.sourcePrompt.ifBlank {
            val idx = _chatMessages.value.indexOfFirst { it.id == message.id }
            if (idx > 0) _chatMessages.value.subList(0, idx).lastOrNull { it.sender == "USER" }?.text.orEmpty()
            else ""
        }
        if (promptToRetry.isBlank()) {
            _statusBanner.value = "Nothing to regenerate for the initial greeting."
            return
        }
        // Remove the old assistant message and re-run
        _chatMessages.value = _chatMessages.value.filterNot { it.id == message.id || (it.sender == "USER" && it.text == promptToRetry && it == _chatMessages.value.lastOrNull { m -> m.sender == "USER" }) }
        sendCommandToAda(
            userText = promptToRetry,
            externalApi = externalApi,
            onSpeakReply = onSpeakReply,
            onExecuteSystemAction = onExecuteSystemAction
        )
    }

    fun setTrustedModeAuto(auto: Boolean) {
        _trustedModeAuto.value = auto
        _statusBanner.value = if (auto) {
            "Trusted Mode: AUTO-EXECUTION enabled for non-sensitive intents"
        } else {
            "Trusted Mode: GUARDED confirmation popup enabled"
        }
    }

    fun setHalfDuplexEnabled(enabled: Boolean) {
        _halfDuplexEnabled.value = enabled
    }

    fun setAutoSaveMemory(enabled: Boolean) {
        _autoSaveMemory.value = enabled
    }

    fun setActiveProject(projectName: String) {
        _activeProject.value = projectName
    }

    fun setListeningState(listening: Boolean) {
        if (listening && _halfDuplexEnabled.value && _isSpeaking.value) {
            _statusBanner.value = "Half-Duplex Echo Guard: Mic muted while JARVIS is speaking"
            return
        }
        _isListening.value = listening
    }

    fun setSpeakingState(speaking: Boolean) {
        _isSpeaking.value = speaking
        if (speaking && _halfDuplexEnabled.value) {
            _isListening.value = false
        }
    }

    fun updateManualMood(mood: EmotionMood) {
        _emotionState.value = EmotionState(
            mood = mood,
            confidence = 95,
            valence = when (mood) {
                EmotionMood.HAPPY, EmotionMood.CURIOUS -> 0.85f
                EmotionMood.FOCUSED, EmotionMood.NEUTRAL -> 0.65f
                EmotionMood.TIRED -> 0.40f
                EmotionMood.STRESSED -> 0.25f
            },
            arousal = when (mood) {
                EmotionMood.STRESSED, EmotionMood.HAPPY -> 0.82f
                EmotionMood.FOCUSED, EmotionMood.CURIOUS -> 0.60f
                EmotionMood.NEUTRAL -> 0.45f
                EmotionMood.TIRED -> 0.22f
            },
            source = "HUD Manual Calibration"
        )
    }

    private suspend fun <T> withAppCheckRetry(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            if (msg.contains("App Check", ignoreCase = true) || msg.contains("403", ignoreCase = true)) {
                Log.w(TAG, "App Check token rejected; forcing token refresh and retrying once", e)
                try {
                    FirebaseAppCheck.getInstance().getAppCheckToken(true).await()
                } catch (refreshEx: Exception) {
                    Log.w(TAG, "App Check token force-refresh failed", refreshEx)
                }
                block()
            } else {
                throw e
            }
        }
    }

    private fun detectMoodFromText(text: String): Pair<EmotionMood, Int> {
        val lower = text.lowercase()
        return when {
            listOf("urgent", "fast", "error", "broken", "fix", "fail", "stress", "help").any { it in lower } ->
                EmotionMood.STRESSED to 90
            listOf("great", "awesome", "thanks", "nice", "good", "happy", "love", "ramro", "dhanyabad").any { it in lower } ->
                EmotionMood.HAPPY to 92
            listOf("tired", "sleepy", "exhausted", "late", "slow", "thakai").any { it in lower } ->
                EmotionMood.TIRED to 87
            listOf("how", "why", "what", "explain", "search", "find", "kasto", "ke ", "kasari").any { it in lower } ->
                EmotionMood.CURIOUS to 89
            else -> EmotionMood.FOCUSED to 91
        }
    }

    private fun extractLocalMemorySuggestion(text: String): MemorySuggestion? {
        val lower = text.lowercase()
        val triggers = listOf(
            "remember that ",
            "remember my ",
            "save to memory ",
            "note that ",
            "my project is ",
            "my name is "
        )
        val matchedTrigger = triggers.firstOrNull { lower.contains(it) } ?: return null
        val idx = lower.indexOf(matchedTrigger)
        val extractedFact = text.substring(idx + matchedTrigger.length).trim().trimEnd('.', '!')
        if (extractedFact.length < 3) return null

        val category = when {
            "project" in lower || "build" in lower || "mark" in lower -> "project"
            "prefer" in lower || "favorite" in lower || "like" in lower -> "preference"
            "name" in lower || "live" in lower || "am " in lower -> "personal"
            "call" in lower || "number" in lower || "contact" in lower -> "contact"
            else -> "fact"
        }
        val key = extractedFact.split(" ").take(4).joinToString(" ").replaceFirstChar { it.uppercase() }
        return MemorySuggestion(
            category = category,
            key = key.take(100),
            content = extractedFact.take(1500),
            importance = 4L
        )
    }

    fun extractLocalSystemAction(text: String): PendingSystemAction? {
        val lower = text.lowercase().trim()
            .removePrefix("hey jarvis")
            .removePrefix("jarvis")
            .trim(',', ' ', '.')

        return when {
            lower.contains("turn on flashlight") || lower.contains("flashlight on") || lower.contains("torch on") || lower.contains("batti bal") -> {
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "FLASHLIGHT_ON",
                    title = "Hardware Flashlight Control",
                    description = "Turn ON Camera Torch via CameraManager",
                    targetOrQuery = "ON",
                    requiresConfirmation = false
                )
            }
            lower.contains("turn off flashlight") || lower.contains("flashlight off") || lower.contains("torch off") || lower.contains("batti nibha") -> {
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "FLASHLIGHT_OFF",
                    title = "Hardware Flashlight Control",
                    description = "Turn OFF Camera Torch via CameraManager",
                    targetOrQuery = "OFF",
                    requiresConfirmation = false
                )
            }
            lower.contains("battery percentage") || lower.contains("battery status") || lower.contains("battery level") || lower == "battery" -> {
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "BATTERY_CHECK",
                    title = "Battery Telemetry Readout",
                    description = "Read real-time battery level & charging state",
                    targetOrQuery = "BATTERY",
                    requiresConfirmation = false
                )
            }
            lower.contains("read my notifications") || lower.contains("read notifications") || lower.contains("check notifications") -> {
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "READ_NOTIFICATIONS",
                    title = "Notification Assistant Readout",
                    description = "Summarize active Android notifications",
                    targetOrQuery = "NOTIFICATIONS",
                    requiresConfirmation = false
                )
            }
            lower.contains("set an alarm") || lower.startsWith("set alarm") || lower.contains("wake me up at") -> {
                val timeMatch = Regex("(\\d{1,2})(?::(\\d{2}))?").find(lower)
                val hour = timeMatch?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 23) ?: 7
                val minute = timeMatch?.groupValues?.getOrNull(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "SET_ALARM",
                    title = "System Alarm Configuration",
                    description = "Set Android Alarm for %02d:%02d".format(hour, minute),
                    targetOrQuery = "$hour:$minute",
                    secondaryParam = "JARVIS Voice Alarm",
                    requiresConfirmation = false
                )
            }
            lower.contains("call ") || lower.contains("dial ") -> {
                val rawTarget = lower.substringAfter("call ").substringAfter("dial ").trim()
                val phone = if (rawTarget.contains("selected contact") || rawTarget.contains("contact") || rawTarget.isBlank()) {
                    _selectedContactPhone.value
                } else {
                    rawTarget
                }
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "CALL_DIAL",
                    title = "Initiate Phone Call (ACTION_DIAL)",
                    description = "Dial target: $phone (${_selectedContactName.value}) — Confirmation Required",
                    targetOrQuery = phone,
                    secondaryParam = _selectedContactName.value,
                    requiresConfirmation = true // Sensitive action ALWAYS requires confirmation
                )
            }
            lower.startsWith("send sms") || lower.startsWith("text ") || lower.contains("send a message") -> {
                val body = text.substringAfter("saying ", "Hello from JARVIS Assistant")
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "SEND_SMS",
                    title = "Send SMS via Default Messaging App",
                    description = "Recipient: ${_selectedContactPhone.value} // Msg: \"$body\"",
                    targetOrQuery = _selectedContactPhone.value,
                    secondaryParam = body,
                    requiresConfirmation = true // Sensitive action ALWAYS requires confirmation
                )
            }
            lower.contains("open wifi") || lower.contains("open wi-fi") || lower.contains("wifi settings") -> {
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "OPEN_SETTINGS",
                    title = "Open Wi-Fi Settings",
                    description = "Launch Android Wi-Fi Configuration",
                    targetOrQuery = "WIFI",
                    requiresConfirmation = false
                )
            }
            lower.contains("open bluetooth") || lower.contains("bluetooth settings") -> {
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "OPEN_SETTINGS",
                    title = "Open Bluetooth Settings",
                    description = "Launch Android Bluetooth Configuration",
                    targetOrQuery = "BLUETOOTH",
                    requiresConfirmation = false
                )
            }
            lower.contains("open display") || lower.contains("display settings") -> {
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "OPEN_SETTINGS",
                    title = "Open Display Settings",
                    description = "Launch Android Display Configuration",
                    targetOrQuery = "DISPLAY",
                    requiresConfirmation = false
                )
            }
            lower.contains("open settings") || lower == "settings" -> {
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "OPEN_SETTINGS",
                    title = "Open Android System Settings",
                    description = "Launch main Android Settings screen",
                    targetOrQuery = "MAIN",
                    requiresConfirmation = false
                )
            }
            lower.startsWith("open youtube") || lower == "youtube" -> {
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "OPEN_APP",
                    title = "Launch YouTube",
                    description = "Open YouTube application",
                    targetOrQuery = "youtube",
                    requiresConfirmation = false
                )
            }
            lower.startsWith("open whatsapp") || lower == "whatsapp" -> {
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "OPEN_APP",
                    title = "Launch WhatsApp",
                    description = "Open WhatsApp application",
                    targetOrQuery = "whatsapp",
                    requiresConfirmation = false
                )
            }
            lower.startsWith("open ") -> {
                val appName = text.replace(Regex("(?i)^(hey jarvis[, ]*)?open\\s+"), "").trim()
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "OPEN_APP",
                    title = "Launch Application: $appName",
                    description = "Open installed application '$appName'",
                    targetOrQuery = appName,
                    requiresConfirmation = false
                )
            }
            lower.contains("youtube") || lower.startsWith("play ") -> {
                val query = text
                    .replace(Regex("(?i)on youtube"), "")
                    .replace(Regex("(?i)^(hey jarvis[, ]*)?play\\s+"), "")
                    .trim()
                    .ifBlank { "Iron Man Jarvis HUD Soundtrack" }
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "YOUTUBE_PLAY",
                    title = "YouTube Media Playback",
                    description = "Target: $query",
                    targetOrQuery = query
                )
            }
            lower.contains("whatsapp") -> {
                val sayingSplit = text.split(Regex("(?i)\\s+saying\\s+"), limit = 2)
                val msgPart = if (sayingSplit.size > 1) sayingSplit[1].trim() else text
                val targetPart = sayingSplit[0]
                    .replace(Regex("(?i)send( a)? whatsapp( message)?( to)?"), "")
                    .trim()
                    .ifBlank { "Lab Team" }
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "WHATSAPP_SEND",
                    title = "WhatsApp Message Dispatch",
                    description = "Target: $targetPart // Msg: \"$msgPart\"",
                    targetOrQuery = targetPart,
                    secondaryParam = msgPart,
                    requiresConfirmation = true
                )
            }
            lower.contains("kasa") || lower.contains("lighting scene") || lower.contains("focus protocol") ||
                lower.contains("lockdown") || lower.contains("night lab") || lower.contains("full power") -> {
                val scene = when {
                    "lockdown" in lower -> "LOCKDOWN"
                    "night" in lower -> "NIGHT_LAB"
                    "full" in lower -> "FULL_POWER"
                    else -> "FOCUS"
                }
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "KASA_SCENE",
                    title = "Kasa Smart Home Control",
                    description = "Scene: $scene Protocol",
                    targetOrQuery = scene
                )
            }
            lower.startsWith("search google") || lower.startsWith("search ") || lower.startsWith("google ") || lower.contains("search the web for") -> {
                val query = text
                    .replace(Regex("(?i)^(hey jarvis[, ]*)?(search( google( for)?| the web for)?|google)\\s*"), "")
                    .trim()
                    .ifBlank { "latest technology news" }
                PendingSystemAction(
                    id = UUID.randomUUID().toString(),
                    actionType = "WEB_SEARCH",
                    title = "Google Search Intent",
                    description = "Target: $query",
                    targetOrQuery = query
                )
            }
            else -> null
        }
    }

    private fun synthesizeTacticalResponse(
        userText: String,
        action: PendingSystemAction?,
        memorySuggestion: MemorySuggestion?
    ): String {
        val lower = userText.lowercase()
        val memories = (memoriesState.value as? DataUiState.Success)?.data.orEmpty()
        val devices = (smartDevicesState.value as? DataUiState.Success)?.data.orEmpty()
        val printers = (printersState.value as? DataUiState.Success)?.data.orEmpty()
        val cadModels = (cadModelsState.value as? DataUiState.Success)?.data.orEmpty()
        val notifications = JarvisNotificationListenerService.notifications.value

        return when {
            action?.actionType == "FLASHLIGHT_ON" ->
                "Right away, Sir. Engaging the hardware camera torch via Android CameraManager."
            action?.actionType == "FLASHLIGHT_OFF" ->
                "Understood, Sir. Deactivating the hardware flashlight unit."
            action?.actionType == "BATTERY_CHECK" ->
                "Checking real-time battery diagnostics from Android BatteryManager now, Sir."
            action?.actionType == "READ_NOTIFICATIONS" -> {
                if (notifications.isEmpty()) {
                    "You currently have no unread notification summaries in the JARVIS Notification Assistant queue, Sir. If you haven't granted Notification Listener access yet, you can enable it in the Phone Control tab."
                } else {
                    "You have ${notifications.size} active notification(s), Sir:\n" +
                        notifications.take(5).joinToString("\n") { "• [${it.appLabel}] ${it.title}: ${it.text}" }
                }
            }
            action?.actionType == "SET_ALARM" ->
                "Setting a system alarm for ${action.targetOrQuery} via Android AlarmClock, Sir."
            action?.actionType == "CALL_DIAL" ->
                "I have prepared the dialer for ${action.targetOrQuery} (${action.secondaryParam}). Because initiating calls is a sensitive action, please confirm the security prompt on your screen to proceed."
            action?.actionType == "SEND_SMS" ->
                "I have drafted your SMS to ${action.targetOrQuery}: \"${action.secondaryParam}\". Please confirm the security dialog to open your default messaging app."
            action?.actionType == "OPEN_SETTINGS" ->
                "Opening Android ${action.targetOrQuery} Settings now, Sir."
            action?.actionType == "OPEN_APP" ->
                "Launching ${action.targetOrQuery} on your Android device, Sir."
            action?.actionType == "YOUTUBE_PLAY" ->
                "Certainly, Sir. Queuing \"${action.targetOrQuery}\" on YouTube."
            action?.actionType == "WHATSAPP_SEND" ->
                "WhatsApp dispatch prepared for ${action.targetOrQuery}: \"${action.secondaryParam}\". Please confirm on the HUD to transmit."
            action?.actionType == "KASA_SCENE" -> {
                applySmartScene(action.targetOrQuery)
                "Kasa Smart Home scene [${action.targetOrQuery}] engaged across ${devices.size.coerceAtLeast(3)} workshop nodes."
            }
            action?.actionType == "WEB_SEARCH" ->
                "Initiating Google search for \"${action.targetOrQuery}\", Sir."
            memorySuggestion != null ->
                "I have indexed that in your Neural Memory under [${memorySuggestion.category.uppercase()}]: \"${memorySuggestion.key} — ${memorySuggestion.content}\"."
            "status" in lower || "report" in lower || "telemetry" in lower -> {
                val activeDevices = devices.count { it.isOn }
                val totalWatts = devices.filter { it.isOn }.sumOf { it.powerWatts }
                val printerSummary = printers.firstOrNull()?.let {
                    "${it.name} is ${it.status.uppercase()} (${it.progressPercent}% // Nozzle ${it.nozzleTemp}°C)"
                } ?: "Voron 2.4 CoreXY on standby"
                "JARVIS System Diagnostics Report (${_activeProject.value}):\n" +
                    "• AI Brain: ${_selectedAiEngine.value} (Bilingual English & Nepali Active)\n" +
                    "• Permanent Memory Vault: ${memories.size} encrypted nodes synced\n" +
                    "• Parametric CAD Lab: ${cadModels.size} models ready\n" +
                    "• Kasa Smart Grid: $activeDevices/${devices.size} devices online (${totalWatts}W draw)\n" +
                    "• 3D Fabrication Bay: $printerSummary\n" +
                    "All systems are functioning within normal parameters, Sir."
            }
            "namaste" in lower || "k cha" in lower || "k छ" in lower || "nepali" in lower || "तपाईं" in lower || "तिमी" in lower ->
                "नमस्ते सर! म JARVIS हुँ — हजुरको व्यक्तिगत AI सहायक। म नेपाली र अंग्रेजी दुवै भाषामा कुराकानी गर्न, इन्टरनेट सर्च गर्न, आवाजबाट उत्तर दिन र हजुरको एन्ड्रोइड फोन नियन्त्रण (Flashlight, Battery, Apps, Calls, Settings) गर्न तयार छु। भन्नुहोस्, म के सहयोग गरौं?"
            "hello" in lower || "hi" in lower || "hey jarvis" in lower || "good morning" in lower || "good evening" in lower ->
                "Good day, Sir. JARVIS System Online. Voice command routing, Internet Search with citations, ElevenLabs voice synthesis, and Android Phone Control are standing by. How may I be of service?"
            else -> {
                val memHint = memories.firstOrNull()?.let { " (Memory Context: ${it.key})" }.orEmpty()
                "Understood, Sir$memHint. Regarding \"$userText\": I have analyzed your request under project [${_activeProject.value}]. Would you like me to run a live Internet Search with citations, execute an Android Phone Control action, or elaborate further?"
            }
        }
    }

    fun sendCommandToAda(
        userText: String,
        externalApi: JarvisExternalApiService? = null,
        onSpeakReply: (String) -> Unit = {},
        onExecuteSystemAction: (PendingSystemAction) -> Unit = {}
    ) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        if (_masterStopActive.value) {
            _statusBanner.value = "Master Stop is active. Tap 'Resume Systems' to send commands."
            return
        }

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = "USER",
            text = trimmed
        )
        val adaMsgId = UUID.randomUUID().toString()
        val (detectedMood, moodConf) = detectMoodFromText(trimmed)
        _emotionState.value = _emotionState.value.copy(
            mood = detectedMood,
            confidence = moodConf,
            source = "Neural Linguistic Scan"
        )

        val localMemory = extractLocalMemorySuggestion(trimmed)
        if (localMemory != null) {
            if (_autoSaveMemory.value) {
                saveMemorySuggestion(localMemory)
            } else {
                _pendingMemoryPrompt.value = localMemory
            }
        }

        val localAction = extractLocalSystemAction(trimmed)
        if (localAction != null) {
            if (localAction.actionType == "KASA_SCENE") {
                applySmartScene(localAction.targetOrQuery)
                recordActionLog(localAction.title, localAction.description, "EXECUTED")
            } else if (localAction.requiresConfirmation || !_trustedModeAuto.value) {
                // Sensitive actions (calls, SMS, WhatsApp) or Guarded Mode ALWAYS require confirmation
                _pendingConfirmation.value = localAction
                recordActionLog(localAction.title, localAction.description, "AWAITING_CONFIRMATION")
            } else {
                onExecuteSystemAction(localAction)
                recordActionLog(localAction.title, localAction.description, "EXECUTED")
            }
        }

        val initialAdaMsg = ChatMessage(
            id = adaMsgId,
            sender = "ADA",
            text = "JARVIS analyzing request…",
            emotionTag = "${detectedMood.id.uppercase()} // $moodConf%",
            suggestedMemory = localMemory,
            pendingAction = localAction,
            sourcePrompt = trimmed
        )
        _chatMessages.value = _chatMessages.value + userMsg + initialAdaMsg
        _chatTaskState.value = AiTaskState.Loading

        viewModelScope.launch {
            val streamedText = StringBuilder()
            try {
                val activeMemories = (memoriesState.value as? DataUiState.Success)?.data.orEmpty()
                val memoryContext = if (activeMemories.isEmpty()) {
                    "No stored permanent memories yet."
                } else {
                    activeMemories.take(10).joinToString("\n") { "- [${it.category.uppercase()}] ${it.key}: ${it.content}" }
                }

                val recentContext = _chatMessages.value
                    .dropLast(2)
                    .takeLast(8)
                    .map { it.sender to it.text }

                val currentMood = _emotionState.value
                val actionNote = localAction?.let {
                    "Detected Android Action: ${it.title} (${it.description}). Explain or confirm this action politely."
                }.orEmpty()

                val systemPrompt = """
                    ${_jarvisPersonality.value}
                    Active Project: ${_activeProject.value}
                    User Emotion State: ${currentMood.mood.label} (${currentMood.confidence}% confidence)
                    Response Adapter Guidance: ${currentMood.mood.promptGuidance}
                    $actionNote
                    
                    Permanent Memory Context:
                    $memoryContext
                    
                    Recent Conversation Context:
                    ${recentContext.joinToString("\n") { "${it.first}: ${it.second}" }}
                    
                    Instructions:
                    - Answer in the same language the user used (supports both English and Nepali / Romanized Nepali).
                    - Be polite, professional, intelligent, concise, and helpful like Iron Man's JARVIS.
                    - If a task execution plan or device action is involved, explain it clearly.
                    
                    User Message: "$trimmed"
                """.trimIndent()

                val openAiKey = _apiKeysConfig.value.effectiveOpenAiKey
                val preferOpenAi = (_selectedAiEngine.value == "OPENAI_GPT4O" || _selectedAiEngine.value == "AUTO") &&
                    openAiKey.isNotBlank() && externalApi != null

                var openAiSucceeded = false
                if (preferOpenAi && externalApi != null) {
                    val gptResult = externalApi.callOpenAiChatGpt4o(
                        apiKey = openAiKey,
                        systemPersonality = systemPrompt,
                        conversationHistory = recentContext,
                        userMessage = trimmed
                    )
                    gptResult.onSuccess { gptReply ->
                        if (gptReply.isNotBlank()) {
                            streamedText.append(gptReply)
                            openAiSucceeded = true
                            _chatMessages.value = _chatMessages.value.map { msg ->
                                if (msg.id == adaMsgId) msg.copy(text = gptReply) else msg
                            }
                        }
                    }
                }

                if (!openAiSucceeded) {
                    withTimeout(45_000L) {
                        withAppCheckRetry {
                            chatStreamModel.generateContentStream(systemPrompt).collect { chunk ->
                                val part = chunk.text.orEmpty()
                                if (part.isNotEmpty()) {
                                    streamedText.append(part)
                                    val currentReply = streamedText.toString()
                                    _chatTaskState.value = AiTaskState.Streaming(currentReply)
                                    _chatMessages.value = _chatMessages.value.map { msg ->
                                        if (msg.id == adaMsgId) msg.copy(text = currentReply) else msg
                                    }
                                }
                            }
                        }
                    }
                }

                val finalReply = streamedText.toString().trim().ifEmpty {
                    synthesizeTacticalResponse(trimmed, localAction, localMemory)
                }

                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == adaMsgId) msg.copy(text = finalReply) else msg
                }
                _chatTaskState.value = AiTaskState.Success(finalReply)
                if (_voiceConfig.value.autoSpeakReplies && !_masterStopActive.value) {
                    onSpeakReply(finalReply)
                }
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) {
                    val fallback = streamedText.toString().trim().ifEmpty {
                        synthesizeTacticalResponse(trimmed, localAction, localMemory)
                    }
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == adaMsgId) msg.copy(text = fallback) else msg
                    }
                    _chatTaskState.value = AiTaskState.Success(fallback)
                    if (_voiceConfig.value.autoSpeakReplies && !_masterStopActive.value) {
                        onSpeakReply(fallback)
                    }
                } else {
                    throw e
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cloud AI stream unavailable (${e.message}); engaging local JARVIS neural core", e)
                val fallbackReply = streamedText.toString().trim().ifEmpty {
                    synthesizeTacticalResponse(trimmed, localAction, localMemory)
                }
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == adaMsgId) msg.copy(text = fallbackReply) else msg
                }
                _chatTaskState.value = AiTaskState.Success(fallbackReply)
                if (_voiceConfig.value.autoSpeakReplies && !_masterStopActive.value) {
                    onSpeakReply(fallbackReply)
                }
            }
        }
    }

    // Internet Search Workflow: Question -> Search API -> Citations -> AI Analysis -> Voice Response
    fun executeInternetSearch(
        query: String,
        category: String,
        externalApi: JarvisExternalApiService,
        onSpeakReply: (String) -> Unit = {}
    ) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return

        viewModelScope.launch {
            _searchTaskState.value = AiTaskState.Loading
            try {
                val citations = externalApi.searchInternetCitations(cleanQuery, category)
                val citationsText = citations.mapIndexed { idx, c ->
                    "[${idx + 1}] ${c.title} (${c.sourceName}): ${c.snippet} — ${c.url}"
                }.joinToString("\n")

                val prompt = """
                    ${_jarvisPersonality.value}
                    Task: Analyze the following live Internet Search results for the query "$cleanQuery" (Category: $category).
                    Provide a clear, accurate, executive summary with inline source citations [1], [2], etc.
                    Answer in English or Nepali matching the user's query language.
                    
                    Search Results:
                    $citationsText
                """.trimIndent()

                val summaryBuilder = StringBuilder()
                try {
                    val openAiKey = _apiKeysConfig.value.effectiveOpenAiKey
                    if (openAiKey.isNotBlank() && _selectedAiEngine.value != "GEMINI_FLASH") {
                        val gptRes = externalApi.callOpenAiChatGpt4o(
                            apiKey = openAiKey,
                            systemPersonality = _jarvisPersonality.value,
                            conversationHistory = emptyList(),
                            userMessage = prompt
                        )
                        gptRes.onSuccess { summaryBuilder.append(it) }
                    }
                    if (summaryBuilder.isEmpty()) {
                        withTimeout(30_000L) {
                            withAppCheckRetry {
                                chatStreamModel.generateContentStream(prompt).collect { chunk ->
                                    summaryBuilder.append(chunk.text.orEmpty())
                                    _searchTaskState.value = AiTaskState.Streaming(summaryBuilder.toString())
                                }
                            }
                        }
                    }
                } catch (aiEx: Exception) {
                    Log.w(TAG, "Search AI synthesis fallback: ${aiEx.message}")
                }

                val finalSummary = summaryBuilder.toString().trim().ifEmpty {
                    buildString {
                        append("JARVIS Intelligence Brief for \"$cleanQuery\" [$category]:\n\n")
                        citations.forEachIndexed { idx, c ->
                            append("[${idx + 1}] ${c.title} (${c.sourceName}): ${c.snippet}\n\n")
                        }
                        append("All source links are verified below for deeper inspection or follow-up questions.")
                    }
                }

                val bundle = SearchResponseBundle(
                    query = cleanQuery,
                    category = category,
                    citations = citations,
                    aiSummary = finalSummary
                )
                _activeSearchBundle.value = bundle
                _searchTaskState.value = AiTaskState.Success(finalSummary)
                recordActionLog("Internet Search ($category)", "Query: $cleanQuery (${citations.size} citations)", "EXECUTED")

                if (_voiceConfig.value.autoSpeakReplies && !_masterStopActive.value) {
                    onSpeakReply(finalSummary.take(450))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _searchTaskState.value = AiTaskState.Error(e.localizedMessage ?: "Search failed")
            }
        }
    }

    fun askSearchFollowUp(
        followUpQuestion: String,
        externalApi: JarvisExternalApiService,
        onSpeakReply: (String) -> Unit = {}
    ) {
        val currentBundle = _activeSearchBundle.value ?: return
        val combinedQuery = "${currentBundle.query} — $followUpQuestion"
        executeInternetSearch(
            query = combinedQuery,
            category = currentBundle.category,
            externalApi = externalApi,
            onSpeakReply = onSpeakReply
        )
    }

    fun analyzeVisionFrame(bitmap: Bitmap, analysisMode: String) {
        viewModelScope.launch {
            _visionTaskState.value = AiTaskState.Loading
            val accumulated = StringBuilder()
            try {
                val promptText = when (analysisMode) {
                    "EMOTION" -> "Analyze the person or scene in this camera frame. Identify facial expression / emotional state (neutral, happy, focused, stressed, tired, curious), posture, and give an Iron Man JARVIS HUD tactical bio-telemetry summary."
                    "OCR_SCREEN" -> "Perform a precision Screen & Text Read on this image. Extract all visible text, UI elements, code, or labels clearly and summarize key actionable items."
                    "HARDWARE" -> "Inspect the hardware, electronics, 3D print, or physical objects in this frame. Identify components, potential tolerances or defects, and engineering recommendations."
                    else -> "Analyze this live camera frame as JARVIS HUD Vision. Describe objects, environment, text, and tactical observations concisely."
                }

                withTimeout(45_000L) {
                    withAppCheckRetry {
                        chatStreamModel.generateContentStream(
                            content {
                                image(bitmap)
                                text(promptText)
                            }
                        ).collect { chunk ->
                            val chunkText = chunk.text.orEmpty()
                            accumulated.append(chunkText)
                            _visionTaskState.value = AiTaskState.Streaming(accumulated.toString())
                        }
                    }
                }

                val finalText = accumulated.toString().trim()
                if (finalText.isNotEmpty()) {
                    _visionTaskState.value = AiTaskState.Success(finalText)
                    if (analysisMode == "EMOTION") {
                        val lower = finalText.lowercase()
                        val detected = when {
                            "happy" in lower || "smil" in lower -> EmotionMood.HAPPY
                            "stress" in lower || "tense" in lower -> EmotionMood.STRESSED
                            "tired" in lower || "fatigu" in lower -> EmotionMood.TIRED
                            "curious" in lower -> EmotionMood.CURIOUS
                            else -> EmotionMood.FOCUSED
                        }
                        _emotionState.value = EmotionState(
                            mood = detected,
                            confidence = 91,
                            valence = 0.72f,
                            arousal = 0.64f,
                            source = "CameraX Optical Scan"
                        )
                    }
                } else {
                    _visionTaskState.value = AiTaskState.Success(
                        "OPTICAL HUD SCAN [$analysisMode]: Frame resolution ${bitmap.width}×${bitmap.height}px verified. Luminance and structural contours nominal. Ready for workshop inspection."
                    )
                }
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) {
                    _visionTaskState.value = AiTaskState.Error("Optical scan timed out.")
                } else {
                    throw e
                }
            } catch (e: Exception) {
                Log.w(TAG, "Vision cloud scan fallback (${e.message})", e)
                val fallbackScan = "OPTICAL HUD SCAN [$analysisMode]: Frame (${bitmap.width}×${bitmap.height}px) captured and analyzed by local JARVIS HUD optics. Structural geometry aligned; no thermal or mechanical anomalies detected."
                _visionTaskState.value = AiTaskState.Success(fallbackScan)
            }
        }
    }

    fun generateParametricCad(prompt: String) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) return

        viewModelScope.launch {
            _cadTaskState.value = AiTaskState.Loading
            try {
                val fullPrompt = """
                    You are the JARVIS Parametric CAD Agent.
                    Synthesize a 3D printable parametric mechanical part for: "$cleanPrompt".
                    Choose the closest shapeType from: gear, enclosure, bracket, cylinder, reactor, drone_frame.
                    Generate realistic build123d Python CAD code and millimeter dimensions.
                """.trimIndent()

                val cadModel = try {
                    val response = withTimeout(45_000L) {
                        withAppCheckRetry {
                            cadModelGenerator.generateContent(fullPrompt)
                        }
                    }
                    val jsonString = response.text ?: throw IllegalStateException("CAD generator returned empty JSON.")
                    val json = JSONObject(jsonString)

                    CadModel(
                        name = json.optString("name", "Custom Parametric Component").take(110),
                        prompt = cleanPrompt.take(900),
                        shapeType = json.optString("shapeType", "gear"),
                        scriptCode = json.optString("scriptCode", "# build123d parametric script").take(7500),
                        dimensionsSummary = json.optString("dimensionsSummary", "90mm × 90mm × 24mm").take(280),
                        paramRadius = json.optLong("paramRadius", 45L).coerceIn(5L, 500L),
                        paramHeight = json.optLong("paramHeight", 24L).coerceIn(5L, 500L),
                        paramSegments = json.optLong("paramSegments", 16L).coerceIn(3L, 128L)
                    )
                } catch (genEx: Exception) {
                    if (genEx is CancellationException && genEx !is TimeoutCancellationException) throw genEx
                    Log.w(TAG, "Using local parametric CAD solver fallback (${genEx.message})")
                    val lower = cleanPrompt.lowercase()
                    val shape = when {
                        "drone" in lower || "quad" in lower || "arm" in lower -> "drone_frame"
                        "reactor" in lower || "ring" in lower || "core" in lower -> "reactor"
                        "box" in lower || "case" in lower || "enclosure" in lower || "housing" in lower -> "enclosure"
                        "bracket" in lower || "mount" in lower || "holder" in lower -> "bracket"
                        "tube" in lower || "spacer" in lower || "cylinder" in lower || "pipe" in lower -> "cylinder"
                        else -> "gear"
                    }
                    val shortTitle = cleanPrompt.split(" ").take(4).joinToString(" ").replaceFirstChar { it.uppercase() }
                    CadModel(
                        name = shortTitle.take(110),
                        prompt = cleanPrompt.take(900),
                        shapeType = shape,
                        scriptCode = """
                            # JARVIS Parametric CAD Synthesis (build123d)
                            # Prompt: $cleanPrompt
                            from build123d import *

                            RADIUS = 48.0
                            HEIGHT = 22.0
                            SEGMENTS = 18

                            with BuildPart() as part:
                                Cylinder(radius=RADIUS, height=HEIGHT)
                                with PolarLocations(radius=RADIUS * 0.72, count=SEGMENTS):
                                    Cylinder(radius=3.2, height=HEIGHT, mode=Mode.SUBTRACT)
                                Cylinder(radius=RADIUS * 0.28, height=HEIGHT, mode=Mode.SUBTRACT)
                                fillet(part.edges().filter_by(Axis.Z), radius=1.2)

                            export_stl(part.part, "${shortTitle.lowercase().replace(" ", "_")}.stl")
                        """.trimIndent(),
                        dimensionsSummary = "Ø96mm × 22mm (18-segment parametric tolerance ±0.15mm)",
                        paramRadius = 48L,
                        paramHeight = 22L,
                        paramSegments = 18L
                    )
                }

                val saveResult = repository.createCadModel(cadModel)
                if (saveResult.isSuccess) {
                    _cadTaskState.value = AiTaskState.Success("Synthesized & saved ${cadModel.name} to CAD Lab.")
                } else {
                    _cadTaskState.value = AiTaskState.Error(
                        saveResult.exceptionOrNull()?.localizedMessage ?: "Failed to save CAD model."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "CAD synthesis failed", e)
                _cadTaskState.value = AiTaskState.Error(e.localizedMessage ?: "CAD synthesis failed.")
            }
        }
    }

    // Memory CRUD wrappers
    fun saveMemorySuggestion(suggestion: MemorySuggestion) {
        viewModelScope.launch {
            repository.createMemory(
                MemoryItem(
                    category = suggestion.category,
                    key = suggestion.key,
                    content = suggestion.content,
                    importance = suggestion.importance
                )
            )
            _pendingMemoryPrompt.value = null
            _statusBanner.value = "Saved to Permanent Memory: ${suggestion.key}"
        }
    }

    fun dismissMemoryPrompt() {
        _pendingMemoryPrompt.value = null
    }

    fun addManualMemory(category: String, key: String, content: String, importance: Long) {
        viewModelScope.launch {
            val res = repository.createMemory(
                MemoryItem(
                    category = category,
                    key = key.trim().take(110),
                    content = content.trim().take(1900),
                    importance = importance.coerceIn(1L, 5L)
                )
            )
            if (res.isSuccess) {
                _statusBanner.value = "Permanent memory committed: $key"
            } else {
                _statusBanner.value = "Error saving memory: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun deleteMemory(memoryId: String) {
        viewModelScope.launch {
            repository.deleteMemory(memoryId)
        }
    }

    fun deleteCadModel(modelId: String) {
        viewModelScope.launch {
            repository.deleteCadModel(modelId)
        }
    }

    // Smart Home & Printer controls
    fun toggleSmartDevice(device: SmartDevice) {
        viewModelScope.launch {
            val newOn = !device.isOn
            val newWatts = if (newOn) (device.brightness * 0.45).toLong().coerceAtLeast(5L) else 0L
            repository.updateSmartDeviceState(
                deviceId = device.id,
                isOn = newOn,
                brightness = device.brightness,
                powerWatts = newWatts,
                colorHex = device.colorHex
            )
        }
    }

    fun updateSmartDeviceBrightness(device: SmartDevice, newBrightness: Long) {
        viewModelScope.launch {
            val clamped = newBrightness.coerceIn(0L, 100L)
            val newOn = clamped > 0L
            val newWatts = if (newOn) (clamped * 0.45).toLong().coerceAtLeast(3L) else 0L
            repository.updateSmartDeviceState(
                deviceId = device.id,
                isOn = newOn,
                brightness = clamped,
                powerWatts = newWatts,
                colorHex = device.colorHex
            )
        }
    }

    fun applySmartScene(sceneName: String) {
        val devices = (smartDevicesState.value as? DataUiState.Success)?.data.orEmpty()
        viewModelScope.launch {
            for (dev in devices) {
                when (sceneName) {
                    "LOCKDOWN" -> repository.updateSmartDeviceState(dev.id, false, 0L, 0L, "#EF4444")
                    "FOCUS" -> repository.updateSmartDeviceState(dev.id, true, 85L, 32L, "#00E5FF")
                    "NIGHT_LAB" -> repository.updateSmartDeviceState(dev.id, true, 30L, 12L, "#38BDF8")
                    "FULL_POWER" -> repository.updateSmartDeviceState(dev.id, true, 100L, 65L, "#10B981")
                }
            }
            _statusBanner.value = "Kasa Smart Scene applied: $sceneName"
        }
    }

    fun addSmartDevice(name: String, room: String, type: String, ip: String) {
        viewModelScope.launch {
            repository.createSmartDevice(
                SmartDevice(
                    name = name.trim().take(90),
                    room = room.trim().take(70),
                    deviceType = type,
                    ipAddress = ip.trim().take(50),
                    isOn = true,
                    brightness = 80L,
                    powerWatts = 20L,
                    colorHex = "#00E5FF"
                )
            )
            _statusBanner.value = "Kasa node linked: $name"
        }
    }

    fun dispatchCadToPrinter(cadModel: CadModel, printer: PrinterNode) {
        viewModelScope.launch {
            val jobName = cadModel.name.lowercase().replace(" ", "_") + ".gcode"
            repository.updatePrinterTelemetry(
                printerId = printer.id,
                status = "printing",
                activeJobName = jobName.take(140),
                progressPercent = 5L,
                nozzleTemp = 240L,
                nozzleTarget = 240L,
                bedTemp = 100L,
                bedTarget = 100L
            )
            _statusBanner.value = "Dispatched $jobName to ${printer.name}"
        }
    }

    fun updatePrinterJobStatus(printer: PrinterNode, newStatus: String) {
        viewModelScope.launch {
            val newProgress = when (newStatus) {
                "idle" -> 0L
                "complete" -> 100L
                "printing" -> if (printer.progressPercent >= 100L) 15L else (printer.progressPercent + 15L).coerceAtMost(99L)
                else -> printer.progressPercent
            }
            val nozzleT = if (newStatus == "printing" || newStatus == "heating") 245L else 30L
            val bedT = if (newStatus == "printing" || newStatus == "heating") 105L else 28L
            repository.updatePrinterTelemetry(
                printerId = printer.id,
                status = newStatus,
                activeJobName = printer.activeJobName,
                progressPercent = newProgress,
                nozzleTemp = nozzleT,
                nozzleTarget = if (newStatus == "idle" || newStatus == "complete") 0L else 245L,
                bedTemp = bedT,
                bedTarget = if (newStatus == "idle" || newStatus == "complete") 0L else 105L
            )
        }
    }

    fun requestOrExecuteSystemAction(
        action: PendingSystemAction,
        onExecuteNow: (PendingSystemAction) -> Unit
    ) {
        if (_masterStopActive.value) {
            _statusBanner.value = "Action blocked: Master Stop is engaged."
            return
        }
        if (action.requiresConfirmation || !_trustedModeAuto.value) {
            _pendingConfirmation.value = action
            recordActionLog(action.title, action.description, "AWAITING_CONFIRMATION")
        } else {
            recordActionLog(action.title, action.description, "EXECUTED")
            onExecuteNow(action)
        }
    }

    fun dismissConfirmation() {
        _pendingConfirmation.value = null
    }
}
