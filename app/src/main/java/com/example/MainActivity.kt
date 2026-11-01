package com.example

import android.Manifest
import android.app.SearchManager
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.ui.screens.CadPrinterScreen
import com.example.ui.screens.HudCoreScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.SystemsKasaScreen
import com.example.ui.screens.VisionScreen
import com.example.ui.theme.HudAmber
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
import java.util.Locale

enum class AdaTab(val route: String, val labelRes: Int, val icon: ImageVector) {
    HUD("hud_core", R.string.nav_hud, Icons.Default.GraphicEq),
    VISION("vision_deck", R.string.nav_vision, Icons.Default.Visibility),
    MEMORY("memory_vault", R.string.nav_memory, Icons.Default.Memory),
    CAD("cad_lab", R.string.nav_cad, Icons.Default.Architecture),
    SYSTEMS("systems_iot", R.string.nav_systems, Icons.Default.SettingsInputComponent)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
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
    val chatTaskState by viewModel.chatTaskState.collectAsStateWithLifecycle()
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

    var currentTab by rememberSaveable { mutableStateOf(AdaTab.HUD) }

    // BackHandler for secondary tabs returning to HUD Core
    BackHandler(enabled = currentTab != AdaTab.HUD) {
        currentTab = AdaTab.HUD
    }

    // TextToSpeech with Half-Duplex Echo Guard
    val ttsRef = remember { mutableStateOf<TextToSpeech?>(null) }
    val speechRecognizerRef = remember { mutableStateOf<SpeechRecognizer?>(null) }

    DisposableEffect(context) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.US
                ttsInstance?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        // Half-Duplex Echo Protection: stop mic while ADA speaks
                        speechRecognizerRef.value?.cancel()
                        viewModel.setSpeakingState(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        viewModel.setSpeakingState(false)
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
                                onSpeakReply = { replyText ->
                                    ttsRef.value?.speak(
                                        replyText,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "ada_reply_${System.currentTimeMillis()}"
                                    )
                                },
                                onExecuteSystemAction = { action ->
                                    executeAndroidSystemAction(context, action, viewModel)
                                }
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
            Toast.makeText(context, "Microphone permission required for Voice Link", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AdaTopHudBar(
                activeProject = activeProject,
                userDisplayName = userDisplayName,
                statusBanner = statusBanner,
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
                AdaTab.entries.forEach { tab ->
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
                                style = MaterialTheme.typography.labelSmall
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
                        ttsRef.value?.stop()
                        viewModel.setSpeakingState(false)
                    },
                    onToggleTrustedMode = { viewModel.setTrustedModeAuto(it) },
                    onToggleHalfDuplex = { viewModel.setHalfDuplexEnabled(it) },
                    onSelectMood = { viewModel.updateManualMood(it) },
                    onSendCommand = { text ->
                        viewModel.sendCommandToAda(
                            userText = text,
                            onSpeakReply = { replyText ->
                                ttsRef.value?.speak(
                                    replyText,
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "ada_reply_${System.currentTimeMillis()}"
                                )
                            },
                            onExecuteSystemAction = { action ->
                                executeAndroidSystemAction(context, action, viewModel)
                            }
                        )
                    },
                    onConfirmMemory = { viewModel.saveMemorySuggestion(it) },
                    onDismissMemory = { viewModel.dismissMemoryPrompt() },
                    onTriggerAction = { action ->
                        viewModel.requestOrExecuteSystemAction(action) {
                            executeAndroidSystemAction(context, it, viewModel)
                        }
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
                            executeAndroidSystemAction(context, it, viewModel)
                        }
                    }
                )
            }
        }
    }

    // Trusted Mode Confirmation Popup (Guarded Mode)
    pendingConfirmation?.let { action ->
        TrustedActionConfirmationDialog(
            action = action,
            onConfirm = {
                viewModel.dismissConfirmation()
                executeAndroidSystemAction(context, action, viewModel)
            },
            onCancel = {
                viewModel.dismissConfirmation()
            }
        )
    }
}

@Composable
private fun AdaTopHudBar(
    activeProject: String,
    userDisplayName: String,
    statusBanner: String?,
    onDismissBanner: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        color = HudPanelSurface,
        border = BorderStroke(0.5.dp, HudCyanDim.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ADA v2",
                            style = MaterialTheme.typography.titleLarge,
                            color = HudCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "// $activeProject",
                            style = MaterialTheme.typography.labelSmall,
                            color = HudSkyBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "OP: $userDisplayName",
                        style = MaterialTheme.typography.labelSmall,
                        color = HudMutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                text = "TRUSTED MODE GUARD // CONFIRM ACTION",
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
                        text = "Tip: Switch to 'TRUSTED MODE: AUTO' on the HUD to execute verified intents without confirmation.",
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
    viewModel: AdaViewModel
) {
    try {
        when (action.actionType) {
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
                    // Fallback to generic share chooser or wa.me web link
                    val shareIntent = Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "[To: ${action.targetOrQuery}] $msg")
                        },
                        "Dispatch Message via"
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(shareIntent)
                }
            }

            "WEB_SEARCH" -> {
                val raw = action.targetOrQuery.trim()
                val url = if (raw.startsWith("http://") || raw.startsWith("https://")) {
                    raw
                } else {
                    "https://www.google.com/search?q=${Uri.encode(raw)}"
                }
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    putExtra(SearchManager.QUERY, raw)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
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
        Toast.makeText(context, "Could not launch external intent: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
