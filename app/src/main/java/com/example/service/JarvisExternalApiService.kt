package com.example.service

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "JarvisExternalApi"

data class SearchCitation(
    val title: String,
    val snippet: String,
    val url: String,
    val sourceName: String,
    val category: String
)

data class SearchResponseBundle(
    val query: String,
    val category: String,
    val citations: List<SearchCitation>,
    val aiSummary: String
)

data class JarvisVoiceConfig(
    val voiceId: String = "JBFqnCBsd6RMkjVDRZzb", // George - Calm British Butler
    val voicePersonaName: String = "JARVIS // Classic British Butler",
    val speechRate: Float = 1.0f,
    val pitch: Float = 0.95f,
    val autoSpeakReplies: Boolean = true,
    val conversationVoiceMode: Boolean = false,
    val preferElevenLabs: Boolean = true
)

data class JarvisApiKeysConfig(
    val openAiKeyOverride: String = "",
    val searchApiKeyOverride: String = "",
    val elevenLabsKeyOverride: String = "",
    val elevenLabsVoiceIdOverride: String = ""
) {
    private fun sanitizeSecret(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("MY_") || trimmed.equals("PLACEHOLDER", ignoreCase = true)) "" else trimmed
    }

    val effectiveOpenAiKey: String
        get() = openAiKeyOverride.trim().ifEmpty { sanitizeSecret(BuildConfig.OPENAI_API_KEY) }

    val effectiveSearchKey: String
        get() = searchApiKeyOverride.trim().ifEmpty { sanitizeSecret(BuildConfig.SEARCH_API_KEY) }

    val effectiveElevenLabsKey: String
        get() = elevenLabsKeyOverride.trim().ifEmpty { sanitizeSecret(BuildConfig.ELEVENLABS_API_KEY) }

    val effectiveElevenLabsVoiceId: String
        get() = elevenLabsVoiceIdOverride.trim()
            .ifEmpty { sanitizeSecret(BuildConfig.ELEVENLABS_VOICE_ID) }
            .ifEmpty { "JBFqnCBsd6RMkjVDRZzb" }
}

class JarvisExternalApiService(private val context: Context) {

    private var activeMediaPlayer: MediaPlayer? = null

    fun stopElevenLabsPlayback() {
        try {
            activeMediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        } catch (_: Exception) {
        } finally {
            activeMediaPlayer = null
        }
    }

    suspend fun callOpenAiChatGpt4o(
        apiKey: String,
        systemPersonality: String,
        conversationHistory: List<Pair<String, String>>,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("OPENAI_API_KEY not configured"))
        }
        try {
            val url = URL("https://api.openai.com/v1/chat/completions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }

            val messagesArray = JSONArray()
            messagesArray.put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", systemPersonality)
                }
            )
            conversationHistory.takeLast(8).forEach { (sender, text) ->
                messagesArray.put(
                    JSONObject().apply {
                        put("role", if (sender == "USER") "user" else "assistant")
                        put("content", text)
                    }
                )
            }
            messagesArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                }
            )

            val payload = JSONObject().apply {
                put("model", "gpt-4o")
                put("messages", messagesArray)
                put("temperature", 0.6)
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val content = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
                Result.success(content)
            } else {
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                Result.failure(IllegalStateException("OpenAI HTTP $code: ${errBody.take(160)}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "OpenAI GPT-4o call failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun searchInternetCitations(
        query: String,
        category: String
    ): List<SearchCitation> = withContext(Dispatchers.IO) {
        val citations = mutableListOf<SearchCitation>()
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return@withContext emptyList()

        // 1. If Weather category or weather query, query Open-Meteo geocoding + forecast
        if (category.equals("WEATHER", ignoreCase = true) ||
            cleanQuery.contains("weather", ignoreCase = true) ||
            cleanQuery.contains("temperature", ignoreCase = true) ||
            cleanQuery.contains("mausam", ignoreCase = true)
        ) {
            try {
                val cityCandidate = cleanQuery
                    .replace(Regex("(?i)(weather|temperature|forecast|in|for|what is|the|today|mausam)"), "")
                    .trim()
                    .ifBlank { "Kathmandu" }
                val encodedCity = URLEncoder.encode(cityCandidate, "UTF-8")
                val geoUrl = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encodedCity&count=1&language=en&format=json")
                val geoJsonStr = (geoUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                }.inputStream.bufferedReader().use { it.readText() }
                val geoJson = JSONObject(geoJsonStr)
                val results = geoJson.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val first = results.getJSONObject(0)
                    val lat = first.getDouble("latitude")
                    val lon = first.getDouble("longitude")
                    val name = first.optString("name", cityCandidate)
                    val country = first.optString("country", "")

                    val wxUrl = URL(
                        "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,wind_speed_10m"
                    )
                    val wxStr = (wxUrl.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                    }.inputStream.bufferedReader().use { it.readText() }
                    val wxJson = JSONObject(wxStr).optJSONObject("current")
                    if (wxJson != null) {
                        val temp = wxJson.optDouble("temperature_2m", 22.0)
                        val hum = wxJson.optInt("relative_humidity_2m", 55)
                        val wind = wxJson.optDouble("wind_speed_10m", 8.0)
                        citations.add(
                            SearchCitation(
                                title = "Live Atmospheric Telemetry — $name, $country",
                                snippet = "Current Temperature: ${temp}°C | Relative Humidity: $hum% | Wind Speed: $wind km/h (Coordinates: $lat, $lon).",
                                url = "https://open-meteo.com/",
                                sourceName = "Open-Meteo Weather Satellite",
                                category = "WEATHER"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Weather lookup fallback: ${e.message}")
            }
        }

        // 2. DuckDuckGo Instant Answer API
        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val ddgUrl = URL("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1")
            val conn = (ddgUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val abstractText = json.optString("AbstractText", "").trim()
            val abstractUrl = json.optString("AbstractURL", "").trim()
            val heading = json.optString("Heading", cleanQuery).trim()
            val abstractSource = json.optString("AbstractSource", "DuckDuckGo Knowledge Graph")

            if (abstractText.isNotEmpty()) {
                citations.add(
                    SearchCitation(
                        title = heading.ifEmpty { cleanQuery },
                        snippet = abstractText,
                        url = abstractUrl.ifEmpty { "https://duckduckgo.com/?q=$encoded" },
                        sourceName = abstractSource,
                        category = category
                    )
                )
            }

            val related = json.optJSONArray("RelatedTopics")
            if (related != null) {
                for (i in 0 until minOf(related.length(), 4)) {
                    val item = related.optJSONObject(i) ?: continue
                    val text = item.optString("Text", "").trim()
                    val firstUrl = item.optString("FirstURL", "").trim()
                    if (text.isNotEmpty() && firstUrl.isNotEmpty()) {
                        citations.add(
                            SearchCitation(
                                title = text.take(68) + if (text.length > 68) "…" else "",
                                snippet = text,
                                url = firstUrl,
                                sourceName = "DuckDuckGo Index",
                                category = category
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DuckDuckGo API fallback: ${e.message}")
        }

        // 3. Wikipedia Search API for comprehensive encyclopedic/news/tech/educational citations
        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val wikiUrl = URL(
                "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&utf8=&format=json&srlimit=4"
            )
            val conn = (wikiUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "JarvisAndroidAssistant/2.0")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val searchArr = JSONObject(body).optJSONObject("query")?.optJSONArray("search")
            if (searchArr != null) {
                for (i in 0 until searchArr.length()) {
                    val item = searchArr.getJSONObject(i)
                    val title = item.optString("title", "")
                    val rawSnippet = item.optString("snippet", "")
                        .replace(Regex("<[^>]*>"), "")
                        .replace("&quot;", "\"")
                        .replace("&amp;", "&")
                        .trim()
                    if (title.isNotEmpty() && rawSnippet.isNotEmpty()) {
                        val articleUrl = "https://en.wikipedia.org/wiki/${URLEncoder.encode(title.replace(" ", "_"), "UTF-8")}"
                        citations.add(
                            SearchCitation(
                                title = title,
                                snippet = rawSnippet,
                                url = articleUrl,
                                sourceName = "Wikipedia Knowledge Base",
                                category = category
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wikipedia search fallback: ${e.message}")
        }

        if (citations.isEmpty()) {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            citations.add(
                SearchCitation(
                    title = "Google Live Index: $cleanQuery",
                    snippet = "Direct web index reference for '$cleanQuery' under category [$category]. Tap to open full browser results.",
                    url = "https://www.google.com/search?q=$encoded",
                    sourceName = "Google Web Search",
                    category = category
                )
            )
        }

        citations.distinctBy { it.url }.take(6)
    }

    suspend fun speakWithElevenLabs(
        apiKey: String,
        voiceId: String,
        text: String,
        speechRate: Float,
        onStart: () -> Unit,
        onComplete: () -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || text.isBlank()) return@withContext false
        try {
            val cleanVoiceId = voiceId.trim().ifEmpty { "JBFqnCBsd6RMkjVDRZzb" }
            val url = URL("https://api.elevenlabs.io/v1/text-to-speech/$cleanVoiceId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 25_000
                doOutput = true
                setRequestProperty("Accept", "audio/mpeg")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("xi-api-key", apiKey)
            }

            val payload = JSONObject().apply {
                put("text", text.take(1200))
                put("model_id", "eleven_multilingual_v2")
                put(
                    "voice_settings",
                    JSONObject().apply {
                        put("stability", 0.65)
                        put("similarity_boost", 0.80)
                    }
                )
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode !in 200..299) {
                Log.w(TAG, "ElevenLabs HTTP ${conn.responseCode}")
                return@withContext false
            }

            val tempFile = File(context.cacheDir, "jarvis_elevenlabs_tts.mp3")
            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            withContext(Dispatchers.Main) {
                stopElevenLabsPlayback()
                val mp = MediaPlayer()
                activeMediaPlayer = mp
                mp.setDataSource(tempFile.absolutePath)
                mp.setOnPreparedListener { player ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            player.playbackParams = PlaybackParams().setSpeed(speechRate.coerceIn(0.5f, 2.0f))
                        } catch (_: Exception) {
                        }
                    }
                    onStart()
                    player.start()
                }
                mp.setOnCompletionListener { player ->
                    player.release()
                    activeMediaPlayer = null
                    onComplete()
                }
                mp.setOnErrorListener { player, _, _ ->
                    player.release()
                    activeMediaPlayer = null
                    onComplete()
                    true
                }
                mp.prepareAsync()
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "ElevenLabs playback fallback to Android TTS: ${e.message}")
            false
        }
    }
}
