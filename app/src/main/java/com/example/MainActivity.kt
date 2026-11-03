package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AdaAuthLockScreen
import com.example.auth.authStateFlow
import com.example.auth.configureAppCheck
import com.example.auth.signOut
import com.example.data.model.PendingSystemAction
import com.example.data.repository.AdaRepository
import com.example.service.JarvisExternalApiService
import com.example.service.JarvisNotificationListenerService
import com.example.service.PhoneControlManager
import com.example.ui.screens.CadPrinterScreen
import com.example.ui.screens.HudCoreScreen
import com.example.ui.screens.InternetSearchScreen
import com.example.ui.screens.JarvisSettingsDialog
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.PhoneControlScreen
import com.example.ui.screens.SystemsKasaScreen
import com.example.ui.screens.VisionScreen
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanDim
import com.example.ui.theme.HudDeepNavy
import com.example.ui.theme.HudEmerald
import com.example.ui.theme.HudIceWhite
import com.example.ui.theme.HudMutedText
import com.example.ui.theme.HudObsidian
import com.example.ui.theme.HudPanelSurface
import com.example.ui.theme.HudSkyBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AdaViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AdaTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val showInBottomBar: Boolean = true
) {
    HUD("hud_core", R.string.nav_hud, Icons.Default.GraphicEq, true),
    SEARCH("internet_search", R.string.nav_search, Icons.Default.Public, true),
    PHONE("phone_control", R.string.nav_phone, Icons.Default.PhoneAndroid, true),
    VISION("vision_deck", R.string.nav_vision, Icons.Default.Visibility, true),
    MEMORY("memory_vault", R.string.nav_memory, Icons.Default.Memory, true),
    CAD("cad_lab", R.string.nav_cad, Icons.Default.Architecture, false),
    SYSTEMS("systems_iot", R.string.nav_systems, Icons.Default.SettingsInputComponent, false)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                AdaRootNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }
}

@Composable
fun AdaRootNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AdaAuthLockScreen(onAuthSuccess = {})
    } else {
        AdaAuthenticatedHudApp(
            currentUserId = user.uid,
            userDisplayName = user.displayName ?: user.email ?: "Commander"
        )
    }
}

@Composable
fun AdaAuthenticatedHudApp(
    currentUserId: String,
    userDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val phoneManager = remember { PhoneControlManager(context) }
    val externalApi = remember { JarvisExternalApiService(context) }

    val viewModel: AdaViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AdaViewModel(AdaRepository(db), currentUserId)
            }
        }
    )

    val emotionState by viewModel.emotionState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val conversationSessions by viewModel.conversationSessions.collectAsStateWithLifecycle()
    val chatTaskState by viewModel.chatTaskState.collectAsStateWithLifecycle()
    val searchTaskState by viewModel.searchTaskState.collectAsStateWithLifecycle()
    val activeSearchBundle by viewModel.activeSearchBundle.collectAsStateWithLifecycle()
    val visionTaskState by viewModel.visionTaskState.collectAsStateWithLifecycle()
    val cadTaskState by viewModel.cadTaskState.collectAsStateWithLifecycle()
    val memoriesState by viewModel.memoriesState.collectAsStateWithLifecycle()
    val cadModelsState by viewModel.cadModelsState.collectAsStateWithLifecycle()
    val smartDevicesState by viewModel.smartDevicesState.collectAsStateWithLifecycle()
    val printersState by viewModel.printersState.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val halfDuplexEnabled by viewModel.halfDuplexEnabled.collectAsStateWithLifecycle()
    val trustedModeAuto by viewModel.trustedModeAuto.collectAsStateWithLifecycle()
    val autoSaveMemory by viewModel.autoSaveMemory.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val pendingMemoryPrompt by viewModel.pendingMemoryPrompt.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val voiceConfig by viewModel.voiceConfig.collectAsStateWithLifecycle()
    val selectedAiEngine by viewModel.selectedAiEngine.collectAsStateWithLifecycle()
    val jarvisPersonality by viewModel.jarvisPersonality.collectAsStateWithLifecycle()
    val apiKeysConfig by viewModel.apiKeysConfig.collectAsStateWithLifecycle()
    val masterStopActive by viewModel.masterStopActive.collectAsStateWithLifecycle()
    val emergencyAutomationDisabled by viewModel.emergencyAutomationDisabled.collectAsStateWithLifecycle()
    val flashlightOn by viewModel.flashlightOn.collectAsStateWithLifecycle()
    val selectedContactName by viewModel.selectedContactName.collectAsStateWithLifecycle()
    val selectedContactPhone by viewModel.selectedContactPhone.collectAsStateWithLifecycle()
    val actionHistoryLog by viewModel.actionHistoryLog.collectAsStateWithLifecycle()

    val notifications by JarvisNotificationListenerService.notifications.collectAsStateWithLifecycle()
    val allowedPackages by JarvisNotificationListenerService.allowedPackageFilter.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(AdaTab.HUD) }
    var settingsDialogSection by rememberSaveable { mutableStateOf<String?>(null) }
    var hasCameraPerm by remember { mutableStateOf(phoneManager.hasCameraPermission()) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    BackHandler(enabled = currentTab != AdaTab.HUD) {
        currentTab = AdaTab.HUD
    }

    val ttsRef = remember { mutableStateOf<TextToSpeech?>(null) }
    val speechRecognizerRef = remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun speakJarvisResponse(text: String) {
        if (masterStopActive || text.isBlank()) return
        scope.launch {
            val elevenKey = apiKeysConfig.effectiveElevenLabsKey
            val voiceId = apiKeysConfig.effectiveElevenLabsVoiceId
            var usedElevenLabs = false
            if (voiceConfig.preferElevenLabs && elevenKey.isNotBlank()) {
                usedElevenLabs = externalApi.speakWithElevenLabs(
                    apiKey = elevenKey,
                    voiceId = voiceId,
                    text = text,
                    speechRate = voiceConfig.speechRate,
                    onStart = {
                        speechRecognizerRef.value?.cancel()
                        viewModel.setSpeakingState(true)
                    },
                    onComplete = {
                        viewModel.setSpeakingState(false)
                        if (voiceConfig.conversationVoiceMode && !masterStopActive) {
                            startVoiceListening(
                                context,
                                speechRecognizerRef.value,
                                ttsRef.value,
                                halfDuplexEnabled,
                                viewModel
                            )
                        }
                    }
                )
            }
            if (!usedElevenLabs) {
                ttsRef.value?.setSpeechRate(voiceConfig.speechRate.coerceIn(0.5f, 2.0f))
                ttsRef.value?.setPitch(voiceConfig.pitch.coerceIn(0.6f, 1.4f))
                ttsRef.value?.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "jarvis_reply_${System.currentTimeMillis()}"
                )
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPerm = granted
        if (granted) {
            val res = phoneManager.setFlashlight(true)
            res.onSuccess {
                viewModel.updateFlashlightState(true)
                viewModel.showStatusBanner("Flashlight turned ON")
                viewModel.recordActionLog("Flashlight Control", "Torch turned ON after permission grant", "EXECUTED")
            }.onFailure {
                viewModel.showStatusBanner(it.localizedMessage ?: "Flashlight unavailable")
            }
        } else {
            viewModel.showStatusBanner("Camera permission denied by user — cannot toggle hardware flashlight.")
            viewModel.recordActionLog("Flashlight Blocked", "Android CAMERA permission denied", "BLOCKED")
        }
    }

    fun handleExecuteAction(action: PendingSystemAction) {
        executeAndroidSystemAction(
            context = context,
            action = action,
            phoneManager = phoneManager,
            viewModel = viewModel,
            onRequestCameraPerm = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
            onSpeak = { speakJarvisResponse(it) },
            onSwitchTab = { currentTab = it }
        )
    }

    DisposableEffect(context) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.US
                ttsInstance?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        speechRecognizerRef.value?.cancel()
                        viewModel.setSpeakingState(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        viewModel.setSpeakingState(false)
                        if (viewModel.voiceConfig.value.conversationVoiceMode && !viewModel.masterStopActive.value) {
                            scope.launch {
                                startVoiceListening(
                                    context,
                                    speechRecognizerRef.value,
                                    ttsRef.value,
                                    viewModel.halfDuplexEnabled.value,
                                    viewModel
                                )
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        viewModel.setSpeakingState(false)
                    }
                })
            }
        }
        ttsRef.value = ttsInstance

        val recognizer = if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        viewModel.setListeningState(true)
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        viewModel.setListeningState(false)
                    }

                    override fun onError(error: Int) {
                        viewModel.setListeningState(false)
                    }

                    override fun onResults(results: Bundle?) {
                        viewModel.setListeningState(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull().orEmpty()
                        if (spokenText.isNotBlank()) {
                            viewModel.sendCommandToAda(
                                userText = spokenText,
                                externalApi = externalApi,
                                onSpeakReply = { replyText -> speakJarvisResponse(replyText) },
                                onExecuteSystemAction = { action -> handleExecuteAction(action) }
                            )
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } else null
        speechRecognizerRef.value = recognizer

        onDispose {
            externalApi.stopElevenLabsPlayback()
            recognizer?.destroy()
            ttsInstance?.stop()
            ttsInstance?.shutdown()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceListening(context, speechRecognizerRef.value, ttsRef.value, halfDuplexEnabled, viewModel)
        } else {
            Toast.makeText(context, "Microphone permission required for JARVIS Voice Link", Toast.LENGTH_SHORT).show()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = HudPanelSurface,
                drawerContentColor = HudIceWhite,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "JARVIS v1.1.1 // SIDEBAR",
                        style = MaterialTheme.typography.titleLarge,
                        color = HudCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "OP: $userDisplayName",
                        style = MaterialTheme.typography.labelSmall,
                        color = HudMutedText
                    )
                    HorizontalDivider(color = HudCyanDim.copy(alpha = 0.35f), modifier = Modifier.padding(vertical = 6.dp))

                    // 1. New Conversation
                    NavigationDrawerItem(
                        label = { Text("New Conversation") },
                        selected = false,
                        onClick = {
                            viewModel.startNewConversation()
                            currentTab = AdaTab.HUD
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.AddComment, contentDescription = null, tint = HudCyan) },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = HudObsidian.copy(alpha = 0.5f))
                    )

                    // 2. Internet Search
                    NavigationDrawerItem(
                        label = { Text("Internet Search") },
                        selected = currentTab == AdaTab.SEARCH,
                        onClick = {
                            currentTab = AdaTab.SEARCH
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Public, contentDescription = null, tint = HudSkyBlue) }
                    )

                    // 3. Phone Control
                    NavigationDrawerItem(
                        label = { Text("Phone Control Dashboard") },
                        selected = currentTab == AdaTab.PHONE,
                        onClick = {
                            currentTab = AdaTab.PHONE
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = HudEmerald) }
                    )

                    // 4. CAD Lab & 3D Printer
                    NavigationDrawerItem(
                        label = { Text("Parametric CAD & 3D Printer") },
                        selected = currentTab == AdaTab.CAD,
                        onClick = {
                            currentTab = AdaTab.CAD
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Architecture, contentDescription = null, tint = HudCyan) }
                    )

                    // 5. Kasa Smart Home & IoT
                    NavigationDrawerItem(
                        label = { Text("Smart Home & Workshop IoT") },
                        selected = currentTab == AdaTab.SYSTEMS,
                        onClick = {
                            currentTab = AdaTab.SYSTEMS
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.SettingsInputComponent, contentDescription = null, tint = HudSkyBlue) }
                    )

                    HorizontalDivider(color = HudCyanDim.copy(alpha = 0.35f), modifier = Modifier.padding(vertical = 6.dp))
                    Text(
                        text = "SYSTEM CONFIGURATION & OTA",
                        style = MaterialTheme.typography.labelSmall,
                        color = HudSkyBlue
                    )

                    // 6. Self-Update & Signing Key (v1.1.1)
                    NavigationDrawerItem(
                        label = { Text("Self-Update & Signing Key (v1.1.1)") },
                        selected = false,
                        onClick = {
                            settingsDialogSection = "UPDATE"
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = HudEmerald) }
                    )

                    // 7. Voice Settings
                    NavigationDrawerItem(
                        label = { Text("Voice Settings (ElevenLabs / TTS)") },
                        selected = false,
                        onClick = {
                            settingsDialogSection = "VOICE"
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = HudCyan) }
                    )

                    // 8. AI Settings
                    NavigationDrawerItem(
                        label = { Text("AI Settings (GPT-4o / Personality)") },
                        selected = false,
                        onClick = {
                            settingsDialogSection = "AI"
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Psychology, contentDescription = null, tint = HudEmerald) }
                    )

                    // 9. API Configuration
                    NavigationDrawerItem(
                        label = { Text("API Configuration & Secrets") },
                        selected = false,
                        onClick = {
                            settingsDialogSection = "API"
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Key, contentDescription = null, tint = HudAmber) }
                    )

                    // 9. Conversation History
                    if (conversationSessions.isNotEmpty()) {
                        HorizontalDivider(color = HudCyanDim.copy(alpha = 0.35f), modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = HudCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = "CONVERSATION HISTORY (${conversationSessions.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = HudCyan
                            )
                        }
                        conversationSessions.forEach { session ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = HudObsidian,
                                border = BorderStroke(1.dp, HudCyanDim.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.loadConversationSession(session)
                                        currentTab = AdaTab.HUD
                                        scope.launch { drawerState.close() }
                                    }
                            ) {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HudIceWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                AdaTopHudBar(
                    phoneManager = phoneManager,
                    activeProject = activeProject,
                    userDisplayName = userDisplayName,
                    masterStopActive = masterStopActive,
                    statusBanner = statusBanner,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenSettings = { settingsDialogSection = "VOICE" },
                    onToggleMasterStop = {
                        if (masterStopActive) {
                            viewModel.resetMasterStop()
                        } else {
                            viewModel.triggerMasterStop {
                                externalApi.stopElevenLabsPlayback()
                                ttsRef.value?.stop()
                                speechRecognizerRef.value?.cancel()
                            }
                        }
                    },
                    onDismissBanner = { viewModel.clearStatusBanner() },
                    onSignOut = {
                        signOut(context, credentialManager, {}, scope)
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = HudPanelSurface,
                    tonalElevation = 8.dp
                ) {
                    AdaTab.entries.filter { it.showInBottomBar }.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = stringResource(tab.labelRes)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HudObsidian,
                                selectedTextColor = HudCyan,
                                indicatorColor = HudCyan,
                                unselectedIconColor = HudMutedText,
                                unselectedTextColor = HudMutedText
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(HudObsidian, HudDeepNavy, HudObsidian)
                        )
                    )
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    AdaTab.HUD -> HudCoreScreen(
                        emotionState = emotionState,
                        chatMessages = chatMessages,
                        chatTaskState = chatTaskState,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        halfDuplexEnabled = halfDuplexEnabled,
                        trustedModeAuto = trustedModeAuto,
                        conversationVoiceMode = voiceConfig.conversationVoiceMode,
                        pendingMemoryPrompt = pendingMemoryPrompt,
                        onToggleVoiceListen = {
                            if (isListening) {
                                speechRecognizerRef.value?.stopListening()
                                viewModel.setListeningState(false)
                            } else {
                                val hasMic = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasMic) {
                                    startVoiceListening(
                                        context,
                                        speechRecognizerRef.value,
                                        ttsRef.value,
                                        halfDuplexEnabled,
                                        viewModel
                                    )
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        onStopSpeaking = {
                            externalApi.stopElevenLabsPlayback()
                            ttsRef.value?.stop()
                            viewModel.setSpeakingState(false)
                        },
                        onToggleTrustedMode = { viewModel.setTrustedModeAuto(it) },
                        onToggleHalfDuplex = { viewModel.setHalfDuplexEnabled(it) },
                        onToggleConversationVoiceMode = {
                            val updated = voiceConfig.copy(conversationVoiceMode = !voiceConfig.conversationVoiceMode)
                            viewModel.updateVoiceConfig(updated)
                            viewModel.showStatusBanner(
                                if (updated.conversationVoiceMode) "Conversation Voice Mode: ON"
                                else "Conversation Voice Mode: OFF"
                            )
                        },
                        onSelectMood = { viewModel.updateManualMood(it) },
                        onSendCommand = { text ->
                            viewModel.sendCommandToAda(
                                userText = text,
                                externalApi = externalApi,
                                onSpeakReply = { replyText -> speakJarvisResponse(replyText) },
                                onExecuteSystemAction = { action -> handleExecuteAction(action) }
                            )
                        },
                        onQuickWebSearch = { query ->
                            currentTab = AdaTab.SEARCH
                            if (query.isNotBlank()) {
                                viewModel.executeInternetSearch(
                                    query = query,
                                    category = "ALL",
                                    externalApi = externalApi,
                                    onSpeakReply = { speakJarvisResponse(it) }
                                )
                            }
                        },
                        onSpeakMessage = { text -> speakJarvisResponse(text) },
                        onRegenerateMessage = { msg ->
                            viewModel.regenerateResponse(
                                message = msg,
                                externalApi = externalApi,
                                onSpeakReply = { speakJarvisResponse(it) },
                                onExecuteSystemAction = { handleExecuteAction(it) }
                            )
                        },
                        onNewConversation = { viewModel.startNewConversation() },
                        onClearConversation = { viewModel.clearCurrentConversation() },
                        onConfirmMemory = { viewModel.saveMemorySuggestion(it) },
                        onDismissMemory = { viewModel.dismissMemoryPrompt() },
                        onTriggerAction = { action ->
                            viewModel.requestOrExecuteSystemAction(action) {
                                handleExecuteAction(it)
                            }
                        }
                    )

                    AdaTab.SEARCH -> InternetSearchScreen(
                        searchTaskState = searchTaskState,
                        activeSearchBundle = activeSearchBundle,
                        onExecuteSearch = { q, cat ->
                            viewModel.executeInternetSearch(
                                query = q,
                                category = cat,
                                externalApi = externalApi,
                                onSpeakReply = { speakJarvisResponse(it) }
                            )
                        },
                        onAskFollowUp = { followUp ->
                            viewModel.askSearchFollowUp(
                                followUpQuestion = followUp,
                                externalApi = externalApi,
                                onSpeakReply = { speakJarvisResponse(it) }
                            )
                        },
                        onSpeakSummary = { speakJarvisResponse(it) },
                        onOpenGoogleBrowser = { q -> phoneManager.openGoogleWebSearch(q) }
                    )

                    AdaTab.PHONE -> PhoneControlScreen(
                        phoneManager = phoneManager,
                        flashlightOn = flashlightOn,
                        hasCameraPermission = hasCameraPerm,
                        masterStopActive = masterStopActive,
                        emergencyAutomationDisabled = emergencyAutomationDisabled,
                        selectedContactName = selectedContactName,
                        selectedContactPhone = selectedContactPhone,
                        notifications = notifications,
                        allowedPackages = allowedPackages,
                        actionHistoryLog = actionHistoryLog,
                        onToggleFlashlight = { enable ->
                            if (!phoneManager.hasCameraPermission()) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                val res = phoneManager.setFlashlight(enable)
                                res.onSuccess { state ->
                                    viewModel.updateFlashlightState(state)
                                    viewModel.showStatusBanner("Flashlight turned ${if (state) "ON" else "OFF"}")
                                    viewModel.recordActionLog("Flashlight Control", "Torch turned ${if (state) "ON" else "OFF"}", "EXECUTED")
                                }.onFailure { err ->
                                    viewModel.showStatusBanner(err.localizedMessage ?: "Flashlight error")
                                    viewModel.recordActionLog("Flashlight Error", err.localizedMessage ?: "Failed", "BLOCKED")
                                }
                            }
                        },
                        onRequestCameraPermission = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onUpdateSelectedContact = { name, phone ->
                            viewModel.updateSelectedContact(name, phone)
                        },
                        onRequestConfirmedAction = { action ->
                            viewModel.requestOrExecuteSystemAction(action) {
                                handleExecuteAction(it)
                            }
                        },
                        onTriggerMasterStop = {
                            viewModel.triggerMasterStop {
                                externalApi.stopElevenLabsPlayback()
                                ttsRef.value?.stop()
                                speechRecognizerRef.value?.cancel()
                            }
                        },
                        onResetMasterStop = { viewModel.resetMasterStop() },
                        onToggleEmergencyDisable = { viewModel.setEmergencyAutomationDisabled(it) },
                        onToggleAllowedNotificationPackage = {
                            JarvisNotificationListenerService.toggleAllowedPackage(it)
                        },
                        onSpeakText = { speakJarvisResponse(it) },
                        onRecordLog = { title, detail, status ->
                            viewModel.recordActionLog(title, detail, status)
                        }
                    )

                    AdaTab.VISION -> VisionScreen(
                        visionTaskState = visionTaskState,
                        onAnalyzeBitmap = { bmp, mode ->
                            viewModel.analyzeVisionFrame(bmp, mode)
                        }
                    )

                    AdaTab.MEMORY -> MemoryScreen(
                        memoriesState = memoriesState,
                        autoSaveMemory = autoSaveMemory,
                        onToggleAutoSave = { viewModel.setAutoSaveMemory(it) },
                        onAddMemory = { cat, key, content, imp ->
                            viewModel.addManualMemory(cat, key, content, imp)
                        },
                        onDeleteMemory = { viewModel.deleteMemory(it) }
                    )

                    AdaTab.CAD -> CadPrinterScreen(
                        cadModelsState = cadModelsState,
                        printersState = printersState,
                        cadTaskState = cadTaskState,
                        onGenerateCad = { viewModel.generateParametricCad(it) },
                        onDeleteCad = { viewModel.deleteCadModel(it) },
                        onDispatchToPrinter = { model, printer ->
                            viewModel.dispatchCadToPrinter(model, printer)
                        },
                        onUpdatePrinterStatus = { printer, status ->
                            viewModel.updatePrinterJobStatus(printer, status)
                        }
                    )

                    AdaTab.SYSTEMS -> SystemsKasaScreen(
                        smartDevicesState = smartDevicesState,
                        activeProject = activeProject,
                        onSelectProject = { viewModel.setActiveProject(it) },
                        onToggleDevice = { viewModel.toggleSmartDevice(it) },
                        onBrightnessChange = { dev, b -> viewModel.updateSmartDeviceBrightness(dev, b) },
                        onApplyScene = { viewModel.applySmartScene(it) },
                        onAddDevice = { name, room, type, ip ->
                            viewModel.addSmartDevice(name, room, type, ip)
                        },
                        onTriggerSystemAction = { action ->
                            viewModel.requestOrExecuteSystemAction(action) {
                                handleExecuteAction(it)
                            }
                        }
                    )
                }
            }
        }
    }

    // Settings Dialog (Voice / AI Brain / API Keys)
    settingsDialogSection?.let { section ->
        JarvisSettingsDialog(
            initialSection = section,
            voiceConfig = voiceConfig,
            selectedAiEngine = selectedAiEngine,
            jarvisPersonality = jarvisPersonality,
            apiKeysConfig = apiKeysConfig,
            onSaveVoiceConfig = { viewModel.updateVoiceConfig(it) },
            onSaveAiSettings = { eng, pers ->
                viewModel.updateSelectedAiEngine(eng)
                viewModel.updateJarvisPersonality(pers)
            },
            onSaveApiKeys = { viewModel.updateApiKeysConfig(it) },
            onTestVoice = { speakJarvisResponse(it) },
            onDismiss = { settingsDialogSection = null }
        )
    }

    // Trusted Mode Confirmation Popup (Guarded Mode & Sensitive Actions)
    pendingConfirmation?.let { action ->
        TrustedActionConfirmationDialog(
            action = action,
            onConfirm = {
                viewModel.dismissConfirmation()
                viewModel.recordActionLog(action.title, "User confirmed & executed: ${action.description}", "EXECUTED")
                handleExecuteAction(action)
            },
            onCancel = {
                viewModel.dismissConfirmation()
                viewModel.recordActionLog(action.title, "User cancelled sensitive action", "BLOCKED")
            }
        )
    }
}

@Composable
private fun AdaTopHudBar(
    phoneManager: PhoneControlManager,
    activeProject: String,
    userDisplayName: String,
    masterStopActive: Boolean,
    statusBanner: String?,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleMasterStop: () -> Unit,
    onDismissBanner: () -> Unit,
    onSignOut: () -> Unit
) {
    var currentTimeText by remember { mutableStateOf("") }
    var batteryPct by remember { mutableStateOf(85) }
    var isCharging by remember { mutableStateOf(false) }
    var isOnline by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            currentTimeText = fmt.format(Date())
            val bat = phoneManager.readBatteryTelemetry()
            batteryPct = bat.percentage
            isCharging = bat.isCharging
            isOnline = phoneManager.isNetworkOnline()
            delay(2000L)
        }
    }

    Surface(
        color = HudPanelSurface,
        border = BorderStroke(0.5.dp, HudCyanDim.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("open_sidebar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open JARVIS Sidebar",
                            tint = HudCyan
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "JARVIS",
                                style = MaterialTheme.typography.titleLarge,
                                color = HudCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentTimeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = HudIceWhite
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = if (isCharging) HudEmerald else HudSkyBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "$batteryPct%${if (isCharging) "⚡" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HudSkyBlue
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = if (isOnline) HudEmerald else HudCrimson,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (isOnline) "ONLINE" else "OFFLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOnline) HudEmerald else HudCrimson
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleMasterStop,
                        modifier = Modifier.testTag("top_master_stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = "Master Stop",
                            tint = if (masterStopActive) HudCrimson else HudAmber
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "JARVIS Settings",
                            tint = HudCyan
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.sign_out),
                            tint = HudSkyBlue
                        )
                    }
                }
            }

            AnimatedVisibility(visible = !statusBanner.isNullOrBlank()) {
                Surface(
                    color = HudEmerald.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusBanner.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = HudEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "OK",
                            style = MaterialTheme.typography.labelSmall,
                            color = HudIceWhite,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clickable(onClick = onDismissBanner)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrustedActionConfirmationDialog(
    action: PendingSystemAction,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = HudPanelSurface,
        icon = {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = HudAmber,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "JARVIS SECURITY GUARD // CONFIRM ACTION",
                style = MaterialTheme.typography.titleLarge,
                color = HudAmber
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = HudIceWhite
                )
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HudMutedText
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HudObsidian,
                    border = BorderStroke(1.dp, HudCyanDim.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Security Policy: Calls, SMS, WhatsApp messages, and sensitive device actions always require explicit confirmation before execution.",
                        style = MaterialTheme.typography.labelSmall,
                        color = HudSkyBlue,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HudEmerald,
                    contentColor = HudObsidian
                ),
                modifier = Modifier.testTag("confirm_system_action_button")
            ) {
                Text("Authorize & Execute")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.testTag("cancel_system_action_button")
            ) {
                Text("Abort")
            }
        }
    )
}

private fun startVoiceListening(
    context: Context,
    recognizer: SpeechRecognizer?,
    tts: TextToSpeech?,
    halfDuplexEnabled: Boolean,
    viewModel: AdaViewModel
) {
    if (halfDuplexEnabled && tts?.isSpeaking == true) {
        tts.stop()
        viewModel.setSpeakingState(false)
    }
    if (recognizer == null) {
        Toast.makeText(context, "Speech recognition service unavailable on this device", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }
    recognizer.startListening(intent)
}

private fun executeAndroidSystemAction(
    context: Context,
    action: PendingSystemAction,
    phoneManager: PhoneControlManager,
    viewModel: AdaViewModel,
    onRequestCameraPerm: () -> Unit,
    onSpeak: (String) -> Unit,
    onSwitchTab: (AdaTab) -> Unit
) {
    try {
        when (action.actionType) {
            "FLASHLIGHT_ON", "FLASHLIGHT_OFF" -> {
                val enable = action.actionType == "FLASHLIGHT_ON"
                if (!phoneManager.hasCameraPermission()) {
                    viewModel.showStatusBanner("Requesting Camera permission for Flashlight…")
                    onRequestCameraPerm()
                } else {
                    val res = phoneManager.setFlashlight(enable)
                    res.onSuccess {
                        viewModel.updateFlashlightState(enable)
                        viewModel.showStatusBanner("Flashlight turned ${if (enable) "ON" else "OFF"}")
                    }.onFailure { err ->
                        viewModel.showStatusBanner(err.localizedMessage ?: "Flashlight unavailable on this device")
                    }
                }
            }

            "BATTERY_CHECK" -> {
                val bat = phoneManager.readBatteryTelemetry()
                val msg = "Sir, your battery is at ${bat.percentage} percent, ${if (bat.isCharging) "charging via ${bat.powerSource}" else "running on battery cell"} (${bat.temperatureC}°C)."
                viewModel.showStatusBanner(msg)
                onSpeak(msg)
            }

            "READ_NOTIFICATIONS" -> {
                if (!JarvisNotificationListenerService.isNotificationAccessGranted(context)) {
                    viewModel.showStatusBanner("Notification Listener permission is not enabled yet. Opening Phone Control…")
                    onSwitchTab(AdaTab.PHONE)
                } else {
                    val list = JarvisNotificationListenerService.notifications.value
                    val msg = if (list.isEmpty()) {
                        "Sir, you have no unread notifications."
                    } else {
                        "You have ${list.size} notifications. Latest from ${list.first().appLabel}: ${list.first().title}."
                    }
                    viewModel.showStatusBanner(msg)
                }
            }

            "SET_ALARM" -> {
                val parts = action.targetOrQuery.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 7
                val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val res = phoneManager.setSystemAlarm(hour, min, action.secondaryParam)
                res.onSuccess { viewModel.showStatusBanner(it) }
                    .onFailure { viewModel.showStatusBanner(it.localizedMessage ?: "Could not set alarm") }
            }

            "CALL_DIAL" -> {
                val res = phoneManager.initiateDialCall(action.targetOrQuery)
                res.onSuccess { viewModel.showStatusBanner(it) }
                    .onFailure { viewModel.showStatusBanner(it.localizedMessage ?: "Dialer unavailable") }
            }

            "SEND_SMS" -> {
                val res = phoneManager.composeSmsMessage(action.targetOrQuery, action.secondaryParam)
                res.onSuccess { viewModel.showStatusBanner(it) }
                    .onFailure { viewModel.showStatusBanner(it.localizedMessage ?: "Messaging app unavailable") }
            }

            "OPEN_SETTINGS" -> {
                val res = phoneManager.openSystemSettings(action.targetOrQuery)
                res.onSuccess { viewModel.showStatusBanner(it) }
                    .onFailure { viewModel.showStatusBanner(it.localizedMessage ?: "Settings unavailable") }
            }

            "OPEN_APP" -> {
                val res = phoneManager.launchAppByPackageOrName(action.targetOrQuery)
                res.onSuccess { viewModel.showStatusBanner(it) }
                    .onFailure { viewModel.showStatusBanner(it.localizedMessage ?: "App not installed") }
            }

            "YOUTUBE_PLAY" -> {
                val query = Uri.encode(action.targetOrQuery)
                val ytIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/results?search_query=$query")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(ytIntent)
            }

            "WHATSAPP_SEND" -> {
                val msg = action.secondaryParam.ifBlank { action.targetOrQuery }
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, msg)
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(sendIntent)
                } catch (_: Exception) {
                    phoneManager.shareTextViaSharesheet("[To: ${action.targetOrQuery}] $msg", "Dispatch via")
                }
            }

            "WEB_SEARCH" -> {
                phoneManager.openGoogleWebSearch(action.targetOrQuery)
            }

            "KASA_SCENE" -> {
                val upper = action.targetOrQuery.uppercase()
                val scene = when {
                    "LOCK" in upper || "OFF" in upper -> "LOCKDOWN"
                    "NIGHT" in upper || "DIM" in upper -> "NIGHT_LAB"
                    "FULL" in upper || "MAX" in upper -> "FULL_POWER"
                    else -> "FOCUS"
                }
                viewModel.applySmartScene(scene)
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Could not execute action: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
