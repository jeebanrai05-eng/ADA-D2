package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CadModel
import com.example.data.model.ChatMessage
import com.example.data.model.EmotionMood
import com.example.data.model.EmotionState
import com.example.data.model.MemoryItem
import com.example.data.model.MemorySuggestion
import com.example.data.model.PendingSystemAction
import com.example.data.model.PrinterNode
import com.example.data.model.SmartDevice
import com.example.data.repository.AdaRepository
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

    // Lazy Firebase AI Logic models (gemini-3.8-flash)
    private val chatStreamModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val assistantStructuredSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "reply" to Schema.string(description = "ADA v2's tactical, Emotion-AI-adapted response to the user"),
                "detectedMood" to Schema.enumeration(
                    values = listOf("neutral", "focused", "happy", "stressed", "tired", "curious"),
                    description = "Detected emotional state of the user from their phrasing"
                ),
                "moodConfidence" to Schema.integer(description = "Confidence percentage 60-99"),
                "shouldSaveMemory" to Schema.boolean(description = "True if the user shared a durable personal fact, preference, project, or contact"),
                "memoryCategory" to Schema.enumeration(
                    values = listOf("personal", "preference", "project", "technical", "contact", "fact"),
                    description = "Category of the extracted memory"
                ),
                "memoryKey" to Schema.string(description = "Short title of the memory if shouldSaveMemory is true, else empty"),
                "memoryContent" to Schema.string(description = "Detailed fact to remember if shouldSaveMemory is true, else empty"),
                "systemIntent" to Schema.enumeration(
                    values = listOf("NONE", "YOUTUBE_PLAY", "WHATSAPP_SEND", "WEB_SEARCH", "KASA_SCENE", "PRINTER_STATUS"),
                    description = "System control action requested by the user, or NONE"
                ),
                "intentTarget" to Schema.string(description = "Search query, song/video name, contact name, or scene name if systemIntent != NONE"),
                "intentMessage" to Schema.string(description = "Message body if WHATSAPP_SEND, else empty")
            )
        )
    }

    private val structuredChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = assistantStructuredSchema
            }
        )
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

    // HUD & Assistant States
    private val _emotionState = MutableStateFlow(EmotionState())
    val emotionState: StateFlow<EmotionState> = _emotionState.asStateFlow()

    private val _trustedModeAuto = MutableStateFlow(false) // false = Guarded (Confirmation Popup), true = Auto-Execute
    val trustedModeAuto: StateFlow<Boolean> = _trustedModeAuto.asStateFlow()

    private val _halfDuplexEnabled = MutableStateFlow(true)
    val halfDuplexEnabled: StateFlow<Boolean> = _halfDuplexEnabled.asStateFlow()

    private val _autoSaveMemory = MutableStateFlow(false)
    val autoSaveMemory: StateFlow<Boolean> = _autoSaveMemory.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _activeProject = MutableStateFlow("MARK II // CORE LAB")
    val activeProject: StateFlow<String> = _activeProject.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingSystemAction?>(null)
    val pendingConfirmation: StateFlow<PendingSystemAction?> = _pendingConfirmation.asStateFlow()

    private val _pendingMemoryPrompt = MutableStateFlow<MemorySuggestion?>(null)
    val pendingMemoryPrompt: StateFlow<MemorySuggestion?> = _pendingMemoryPrompt.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "welcome_1",
                sender = "ADA",
                text = "ADA v2 Online. Neural HUD, Permanent Memory, Emotion AI Adapter, and Half-Duplex Echo Guard initialized. How shall we proceed?",
                emotionTag = "FOCUSED // 92%"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

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

    fun setTrustedModeAuto(auto: Boolean) {
        _trustedModeAuto.value = auto
        _statusBanner.value = if (auto) {
            "Trusted Mode: AUTO-EXECUTION enabled for system intents"
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
        // Half-Duplex Echo Protection: block mic activation while TTS is actively speaking
        if (listening && _halfDuplexEnabled.value && _isSpeaking.value) {
            _statusBanner.value = "Half-Duplex Echo Guard: Mic muted while ADA is speaking"
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

    fun sendCommandToAda(
        userText: String,
        onSpeakReply: (String) -> Unit = {},
        onExecuteSystemAction: (PendingSystemAction) -> Unit = {}
    ) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = "USER",
            text = trimmed
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _chatTaskState.value = AiTaskState.Loading

        viewModelScope.launch {
            try {
                val activeMemories = (memoriesState.value as? DataUiState.Success)?.data.orEmpty()
                val memoryContext = if (activeMemories.isEmpty()) {
                    "No stored permanent memories yet."
                } else {
                    activeMemories.take(10).joinToString("\n") { "- [${it.category.uppercase()}] ${it.key}: ${it.content}" }
                }

                val currentMood = _emotionState.value
                val systemPrompt = """
                    You are ADA v2 (Advanced Digital Assistant Mark II), a futuristic Iron Man HUD AI assistant.
                    Active Project: ${_activeProject.value}
                    User Current Emotion State: ${currentMood.mood.label} (${currentMood.confidence}% confidence)
                    Response Adapter Guidance: ${currentMood.mood.promptGuidance}
                    
                    User Permanent Memory Context:
                    $memoryContext
                    
                    Instructions:
                    1. Respond directly and helpfully to the user's message following the Response Adapter Guidance.
                    2. Detect the user's emotional mood from their message.
                    3. If the user states a new personal fact, preference, project detail, technical stack, or contact, set shouldSaveMemory=true and populate memoryCategory, memoryKey, and memoryContent.
                    4. If the user asks to play a video/song on YouTube (e.g. "play ... on YouTube"), set systemIntent="YOUTUBE_PLAY" and intentTarget to the search query.
                    5. If the user asks to send a WhatsApp message (e.g. "send WhatsApp to ... saying ..."), set systemIntent="WHATSAPP_SEND", intentTarget to recipient/phone, and intentMessage to the text.
                    6. If the user asks to search the web or open a website, set systemIntent="WEB_SEARCH" and intentTarget to the query/URL.
                    7. If the user asks to toggle workshop lights or Kasa smart home scenes, set systemIntent="KASA_SCENE" and intentTarget to the scene/device command.
                    8. Otherwise set systemIntent="NONE".
                    
                    User Message: "$trimmed"
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    withAppCheckRetry {
                        structuredChatModel.generateContent(systemPrompt)
                    }
                }

                val jsonText = response.text ?: throw IllegalStateException("Model returned empty response.")
                val json = JSONObject(jsonText)

                val reply = json.optString("reply", "Command processed.")
                val detectedMoodStr = json.optString("detectedMood", "focused")
                val moodConf = json.optInt("moodConfidence", 88).coerceIn(50, 99)
                val shouldSaveMemory = json.optBoolean("shouldSaveMemory", false)
                val memCat = json.optString("memoryCategory", "fact")
                val memKey = json.optString("memoryKey", "")
                val memContent = json.optString("memoryContent", "")
                val systemIntent = json.optString("systemIntent", "NONE")
                val intentTarget = json.optString("intentTarget", "")
                val intentMessage = json.optString("intentMessage", "")

                val matchedMood = EmotionMood.entries.find { it.id.equals(detectedMoodStr, ignoreCase = true) }
                    ?: EmotionMood.FOCUSED
                _emotionState.value = _emotionState.value.copy(
                    mood = matchedMood,
                    confidence = moodConf,
                    source = "Neural Linguistic Scan"
                )

                var memorySuggestion: MemorySuggestion? = null
                if (shouldSaveMemory && memKey.isNotBlank() && memContent.isNotBlank()) {
                    val suggestion = MemorySuggestion(
                        category = memCat,
                        key = memKey.take(100),
                        content = memContent.take(1500),
                        importance = 4L
                    )
                    if (_autoSaveMemory.value) {
                        saveMemorySuggestion(suggestion)
                    } else {
                        memorySuggestion = suggestion
                        _pendingMemoryPrompt.value = suggestion
                    }
                }

                var pendingAction: PendingSystemAction? = null
                if (systemIntent != "NONE" && intentTarget.isNotBlank()) {
                    val action = PendingSystemAction(
                        id = UUID.randomUUID().toString(),
                        actionType = systemIntent,
                        title = when (systemIntent) {
                            "YOUTUBE_PLAY" -> "YouTube Media Playback"
                            "WHATSAPP_SEND" -> "WhatsApp Message Dispatch"
                            "WEB_SEARCH" -> "Web Agent Research"
                            "KASA_SCENE" -> "Kasa Smart Home Control"
                            else -> "3D Printer Telemetry Check"
                        },
                        description = "Target: $intentTarget" + if (intentMessage.isNotBlank()) " // Msg: \"$intentMessage\"" else "",
                        targetOrQuery = intentTarget,
                        secondaryParam = intentMessage
                    )
                    pendingAction = action
                    if (_trustedModeAuto.value) {
                        onExecuteSystemAction(action)
                    } else {
                        _pendingConfirmation.value = action
                    }
                }

                val adaMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = "ADA",
                    text = reply,
                    emotionTag = "${matchedMood.id.uppercase()} // $moodConf%",
                    suggestedMemory = memorySuggestion,
                    pendingAction = pendingAction
                )
                _chatMessages.value = _chatMessages.value + adaMessage
                _chatTaskState.value = AiTaskState.Success(reply)
                onSpeakReply(reply)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "ADA inference timed out", e)
                _chatTaskState.value = AiTaskState.Error("Neural link timed out after 60s. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "ADA inference failed", e)
                val errMsg = e.localizedMessage ?: "Neural inference failed."
                _chatTaskState.value = AiTaskState.Error(errMsg)
            }
        }
    }

    fun analyzeVisionFrame(bitmap: Bitmap, analysisMode: String) {
        viewModelScope.launch {
            _visionTaskState.value = AiTaskState.Loading
            val accumulated = StringBuilder()
            try {
                val promptText = when (analysisMode) {
                    "EMOTION" -> "Analyze the person or scene in this camera frame. Identify facial expression / emotional state (neutral, happy, focused, stressed, tired, curious), posture, and give an Iron Man HUD tactical bio-telemetry summary."
                    "OCR_SCREEN" -> "Perform a precision Screen & Text Read on this image. Extract all visible text, UI elements, code, or labels clearly and summarize key actionable items."
                    "HARDWARE" -> "Inspect the hardware, electronics, 3D print, or physical objects in this frame. Identify components, potential tolerances or defects, and engineering recommendations."
                    else -> "Analyze this live camera frame as ADA v2 HUD Vision. Describe objects, environment, text, and tactical observations concisely."
                }

                withTimeout(60_000L) {
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
                    _visionTaskState.value = AiTaskState.Error("Optical scan returned empty analysis.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Vision scan timed out", e)
                _visionTaskState.value = AiTaskState.Error("Optical scan timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Vision scan failed", e)
                _visionTaskState.value = AiTaskState.Error(e.localizedMessage ?: "Optical scan failed.")
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
                    You are the ADA v2 Parametric CAD Agent.
                    Synthesize a 3D printable parametric mechanical part for: "$cleanPrompt".
                    Choose the closest shapeType from: gear, enclosure, bracket, cylinder, reactor, drone_frame.
                    Generate realistic build123d Python CAD code and millimeter dimensions.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    withAppCheckRetry {
                        cadModelGenerator.generateContent(fullPrompt)
                    }
                }
                val jsonString = response.text ?: throw IllegalStateException("CAD generator returned empty JSON.")
                val json = JSONObject(jsonString)

                val cadModel = CadModel(
                    name = json.optString("name", "Custom Parametric Component").take(110),
                    prompt = cleanPrompt.take(900),
                    shapeType = json.optString("shapeType", "gear"),
                    scriptCode = json.optString("scriptCode", "# build123d parametric script").take(7500),
                    dimensionsSummary = json.optString("dimensionsSummary", "90mm × 90mm × 24mm").take(280),
                    paramRadius = json.optLong("paramRadius", 45L).coerceIn(5L, 500L),
                    paramHeight = json.optLong("paramHeight", 24L).coerceIn(5L, 500L),
                    paramSegments = json.optLong("paramSegments", 16L).coerceIn(3L, 128L)
                )

                val saveResult = repository.createCadModel(cadModel)
                if (saveResult.isSuccess) {
                    _cadTaskState.value = AiTaskState.Success("Synthesized & saved ${cadModel.name} to CAD Lab.")
                } else {
                    _cadTaskState.value = AiTaskState.Error(
                        saveResult.exceptionOrNull()?.localizedMessage ?: "Failed to save CAD model."
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "CAD synthesis timed out", e)
                _cadTaskState.value = AiTaskState.Error("CAD synthesis timed out after 60 seconds.")
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
        if (_trustedModeAuto.value) {
            onExecuteNow(action)
        } else {
            _pendingConfirmation.value = action
        }
    }

    fun dismissConfirmation() {
        _pendingConfirmation.value = null
    }
}
