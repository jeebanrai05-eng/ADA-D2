package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.service.SearchCitation
import com.example.service.SearchResponseBundle
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

@Composable
fun InternetSearchScreen(
    searchTaskState: AiTaskState,
    activeSearchBundle: SearchResponseBundle?,
    onExecuteSearch: (query: String, category: String) -> Unit,
    onAskFollowUp: (String) -> Unit,
    onSpeakSummary: (String) -> Unit,
    onOpenGoogleBrowser: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var followUpInput by remember { mutableStateOf("") }

    val categories = listOf(
        "ALL" to "All Web",
        "NEWS" to "News",
        "TECHNOLOGY" to "Technology",
        "WEATHER" to "Weather",
        "EDUCATION" to "Education"
    )

    val quickQueries = listOf(
        "Kathmandu Weather" to ("Kathmandu weather forecast today" to "WEATHER"),
        "AI & Robotics News" to ("Artificial intelligence and robotics breakthroughs" to "TECHNOLOGY"),
        "Quantum Computing" to ("How quantum computing qubits work" to "EDUCATION"),
        "Global Space Missions" to ("NASA Artemis and SpaceX Starship updates" to "NEWS")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HudPanelSurface),
            border = BorderStroke(1.dp, HudCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = HudCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "JARVIS LIVE INTERNET SEARCH & CITATIONS",
                        style = MaterialTheme.typography.titleMedium,
                        color = HudCyan
                    )
                }

                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { (catId, label) ->
                        FilterChip(
                            selected = selectedCategory == catId,
                            onClick = { selectedCategory = catId },
                            label = {
                                Text(text = label, style = MaterialTheme.typography.labelSmall)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HudCyan.copy(alpha = 0.25f),
                                selectedLabelColor = HudCyan
                            )
                        )
                    }
                }

                // Search Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search news, weather, technology, or educational topics…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HudMutedText.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    onExecuteSearch(searchQuery, selectedCategory)
                                }
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HudCyan,
                            unfocusedBorderColor = HudCyanDim.copy(alpha = 0.5f),
                            focusedContainerColor = HudObsidian,
                            unfocusedContainerColor = HudObsidian,
                            focusedTextColor = HudIceWhite,
                            unfocusedTextColor = HudIceWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("internet_search_input")
                    )

                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                onExecuteSearch(searchQuery, selectedCategory)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HudCyan,
                            contentColor = HudObsidian
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("execute_internet_search_button")
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Search", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Quick Topic Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickQueries.forEach { (chipLabel, pair) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HudPanelElevated,
                            border = BorderStroke(1.dp, HudSkyBlue.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                searchQuery = pair.first
                                selectedCategory = pair.second
                                onExecuteSearch(pair.first, pair.second)
                            }
                        ) {
                            Text(
                                text = chipLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = HudSkyBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        if (searchTaskState is AiTaskState.Loading || searchTaskState is AiTaskState.Streaming) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = HudCyan,
                trackColor = HudPanelElevated
            )
        }

        // Results + Citations + Follow-Up List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            if (activeSearchBundle != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = HudDeepNavy),
                        border = BorderStroke(1.dp, HudCyan)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AI ANALYSIS // [${activeSearchBundle.category}]",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HudEmerald
                                    )
                                    Text(
                                        text = activeSearchBundle.query,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = HudIceWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { onSpeakSummary(activeSearchBundle.aiSummary) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Speak search summary",
                                            tint = HudCyan
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cb?.setPrimaryClip(ClipData.newPlainText("Search Summary", activeSearchBundle.aiSummary))
                                            Toast.makeText(context, "Summary copied", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy summary",
                                            tint = HudSkyBlue
                                        )
                                    }
                                }
                            }

                            Text(
                                text = activeSearchBundle.aiSummary,
                                style = MaterialTheme.typography.bodyLarge,
                                color = HudIceWhite
                            )

                            OutlinedButton(
                                onClick = { onOpenGoogleBrowser(activeSearchBundle.query) },
                                border = BorderStroke(1.dp, HudSkyBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = HudSkyBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Full Google Results in Browser", color = HudSkyBlue)
                            }
                        }
                    }
                }

                // Follow-Up Question Input Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = HudPanelSurface),
                        border = BorderStroke(1.dp, HudSkyBlue.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ASK FOLLOW-UP ABOUT THESE RESULTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = HudSkyBlue
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = followUpInput,
                                    onValueChange = { followUpInput = it },
                                    placeholder = {
                                        Text(
                                            text = "Ask a follow-up question…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = HudMutedText.copy(alpha = 0.6f)
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        if (followUpInput.isNotBlank()) {
                                            onAskFollowUp(followUpInput)
                                            followUpInput = ""
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send follow-up",
                                        tint = HudCyan
                                    )
                                }
                            }
                        }
                    }
                }

                // Source Citations Header
                item {
                    Text(
                        text = "VERIFIED SOURCE CITATIONS (${activeSearchBundle.citations.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = HudCyan,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                itemsIndexed(activeSearchBundle.citations) { index, citation ->
                    CitationCard(
                        index = index + 1,
                        citation = citation,
                        onOpenUrl = { url ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open URL: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = HudPanelSurface),
                        border = BorderStroke(1.dp, HudCyanDim.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "WORKFLOW: QUESTION → SEARCH API → AI ANALYSIS → VOICE",
                                style = MaterialTheme.typography.labelLarge,
                                color = HudCyan
                            )
                            Text(
                                text = "Enter any question above or tap a quick topic chip. JARVIS queries live web sources (Open-Meteo Weather, DuckDuckGo Instant Answer, and Wikipedia Knowledge Graph), synthesizes an executive summary with GPT-4o / Gemini 3.8, provides clickable source citations, and speaks the answer aloud.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HudMutedText
                            )
                        }
                    }
                }
            }

            if (searchTaskState is AiTaskState.Error) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = HudCrimson.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, HudCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "SEARCH ALERT: ${searchTaskState.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HudCrimson,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CitationCard(
    index: Int,
    citation: SearchCitation,
    onOpenUrl: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUrl(citation.url) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HudPanelSurface),
        border = BorderStroke(1.dp, HudCyanDim.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[$index] ${citation.sourceName.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = HudAmber
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open source link",
                    tint = HudCyan,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = citation.title,
                style = MaterialTheme.typography.titleMedium,
                color = HudIceWhite,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = citation.snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = HudMutedText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = citation.url,
                style = MaterialTheme.typography.labelSmall,
                color = HudSkyBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
