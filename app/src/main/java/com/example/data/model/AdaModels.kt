package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class MemoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "fact", // personal, preference, project, technical, contact, fact
    val key: String = "",
    val content: String = "",
    val importance: Long = 3L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "category" to category,
        "key" to key,
        "content" to content,
        "importance" to importance,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class CadModel(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val prompt: String = "",
    val shapeType: String = "gear", // gear, enclosure, bracket, cylinder, reactor, drone_frame
    val scriptCode: String = "",
    val dimensionsSummary: String = "",
    val paramRadius: Long = 45L,
    val paramHeight: Long = 20L,
    val paramSegments: Long = 24L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "name" to name,
        "prompt" to prompt,
        "shapeType" to shapeType,
        "scriptCode" to scriptCode,
        "dimensionsSummary" to dimensionsSummary,
        "paramRadius" to paramRadius.coerceIn(5L, 500L),
        "paramHeight" to paramHeight.coerceIn(5L, 500L),
        "paramSegments" to paramSegments.coerceIn(3L, 128L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class SmartDevice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val room: String = "",
    val deviceType: String = "bulb", // bulb, plug, strip, dimmer, climate
    val ipAddress: String = "192.168.1.100",
    val isOn: Boolean = true,
    val brightness: Long = 80L,
    val powerWatts: Long = 15L,
    val colorHex: String = "#00E5FF",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "name" to name,
        "room" to room,
        "deviceType" to deviceType,
        "ipAddress" to ipAddress,
        "isOn" to isOn,
        "brightness" to brightness.coerceIn(0L, 100L),
        "powerWatts" to powerWatts.coerceIn(0L, 5000L),
        "colorHex" to colorHex,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class PrinterNode(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val hostUrl: String = "",
    val status: String = "idle", // idle, printing, paused, heating, complete
    val activeJobName: String = "None",
    val progressPercent: Long = 0L,
    val nozzleTemp: Long = 25L,
    val nozzleTarget: Long = 0L,
    val bedTemp: Long = 24L,
    val bedTarget: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "name" to name,
        "hostUrl" to hostUrl,
        "status" to status,
        "activeJobName" to activeJobName,
        "progressPercent" to progressPercent.coerceIn(0L, 100L),
        "nozzleTemp" to nozzleTemp.coerceIn(0L, 350L),
        "nozzleTarget" to nozzleTarget.coerceIn(0L, 350L),
        "bedTemp" to bedTemp.coerceIn(0L, 150L),
        "bedTarget" to bedTarget.coerceIn(0L, 150L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

enum class EmotionMood(
    val id: String,
    val label: String,
    val adapterTone: String,
    val promptGuidance: String
) {
    NEUTRAL("neutral", "NEUTRAL // BALANCED", "Tactical & Clear", "Maintain a balanced, crisp, Stark-style HUD assistant tone."),
    FOCUSED("focused", "FOCUSED // DEEP WORK", "Concise & High-Precision", "Be ultra-concise, technical, and minimize filler."),
    HAPPY("happy", "ELEVATED // POSITIVE", "Warm & Energetic", "Match the user's enthusiasm with witty, upbeat synergy."),
    STRESSED("stressed", "ELEVATED CORTISOL // STRESSED", "Calm & De-escalating", "Prioritize step-by-step clarity, reassurance, and cognitive offloading."),
    TIRED("tired", "LOW ENERGY // FATIGUED", "Gentle & Executive Summary", "Provide brief executive summaries and offer to automate tasks."),
    CURIOUS("curious", "INQUISITIVE // EXPLORING", "Analytical & Detailed", "Provide deep architectural insights, connections, and proactive follow-ups.")
}

data class EmotionState(
    val mood: EmotionMood = EmotionMood.FOCUSED,
    val confidence: Int = 92,
    val valence: Float = 0.65f,
    val arousal: Float = 0.58f,
    val source: String = "Voice + Neural Context"
)

data class ChatMessage(
    val id: String,
    val sender: String, // "USER", "ADA", "SYSTEM"
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val emotionTag: String? = null,
    val suggestedMemory: MemorySuggestion? = null,
    val pendingAction: PendingSystemAction? = null
)

data class MemorySuggestion(
    val category: String,
    val key: String,
    val content: String,
    val importance: Long = 4L
)

data class PendingSystemAction(
    val id: String,
    val actionType: String, // YOUTUBE_PLAY, WHATSAPP_SEND, WEB_SEARCH, KASA_SCENE, PRINTER_START
    val title: String,
    val description: String,
    val targetOrQuery: String,
    val secondaryParam: String = ""
)
