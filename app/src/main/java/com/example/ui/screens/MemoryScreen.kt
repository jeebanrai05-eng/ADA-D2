package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MemoryItem
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudCrimson
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

@Composable
fun MemoryScreen(
    memoriesState: DataUiState<List<MemoryItem>>,
    autoSaveMemory: Boolean,
    onToggleAutoSave: (Boolean) -> Unit,
    onAddMemory: (String, String, String, Long) -> Unit,
    onDeleteMemory: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf("ALL", "project", "preference", "technical", "personal", "contact", "fact")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header & Auto-Save Toggle
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = HudCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "PERMANENT MEMORY VAULT",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = HudCyan
                                )
                                Text(
                                    text = "Injected into every ADA v2 neural session",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HudMutedText
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AUTO-COMMIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (autoSaveMemory) HudEmerald else HudMutedText
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = autoSaveMemory,
                                onCheckedChange = onToggleAutoSave,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = HudObsidian,
                                    checkedTrackColor = HudEmerald
                                ),
                                modifier = Modifier.testTag("auto_save_memory_switch")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = HudSkyBlue
                            )
                        },
                        placeholder = {
                            Text("Search stored facts, preferences, or projects…", color = HudMutedText.copy(alpha = 0.6f))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HudCyan,
                            unfocusedBorderColor = HudCyanDim.copy(alpha = 0.4f),
                            focusedContainerColor = HudObsidian,
                            unfocusedContainerColor = HudObsidian
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("memory_search_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = {
                                    Text(
                                        text = cat.uppercase(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HudCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = HudCyan
                                )
                            )
                        }
                    }
                }
            }

            // Memories List
            when (memoriesState) {
                is DataUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = HudCyan)
                    }
                }
                is DataUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = HudCrimson.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, HudCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = memoriesState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = HudCrimson,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is DataUiState.Success -> {
                    val filtered = memoriesState.data.filter { item ->
                        (selectedCategory == "ALL" || item.category.equals(selectedCategory, ignoreCase = true)) &&
                            (searchQuery.isBlank() ||
                                item.key.contains(searchQuery, ignoreCase = true) ||
                                item.content.contains(searchQuery, ignoreCase = true))
                    }

                    if (filtered.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = HudPanelSurface.copy(alpha = 0.7f)),
                            border = BorderStroke(1.dp, HudCyanDim.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = HudSkyBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No matching memories in this sector",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = HudIceWhite
                                )
                                Text(
                                    text = "Tap + to commit a new fact, preference, or project specification to ADA v2's cloud vault.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HudMutedText
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("memories_lazy_list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(filtered, key = { it.id }) { item ->
                                MemoryCardItem(
                                    item = item,
                                    onDelete = { onDeleteMemory(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = HudCyan,
            contentColor = HudObsidian,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("add_memory_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.memory_add_title)
            )
        }
    }

    if (showAddDialog) {
        AddMemoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { cat, key, content, imp ->
                onAddMemory(cat, key, content, imp)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MemoryCardItem(
    item: MemoryItem,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HudPanelSurface),
        border = BorderStroke(1.dp, HudSkyBlue.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("memory_item_card_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = HudPanelElevated,
                        border = BorderStroke(1.dp, HudCyan.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = HudCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Row {
                        repeat(item.importance.toInt().coerceIn(1, 5)) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = HudAmber,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_memory_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete memory",
                        tint = HudCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = item.key,
                style = MaterialTheme.typography.titleMedium,
                color = HudIceWhite,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                color = HudMutedText
            )
        }
    }
}

@Composable
private fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Long) -> Unit
) {
    var category by remember { mutableStateOf("project") }
    var keyText by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }
    var importance by remember { mutableFloatStateOf(4f) }

    val categories = listOf("project", "preference", "technical", "personal", "contact", "fact")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HudPanelSurface,
        title = {
            Text(
                text = stringResource(R.string.memory_add_title),
                style = MaterialTheme.typography.titleLarge,
                color = HudCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.uppercase(), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = { Text("Memory Key / Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_memory_key_input")
                )

                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    label = { Text("Detailed Fact or Preference") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_memory_content_input")
                )

                Text(
                    text = "Priority Weight: ${importance.toInt()} / 5",
                    style = MaterialTheme.typography.labelSmall,
                    color = HudAmber
                )
                Slider(
                    value = importance,
                    onValueChange = { importance = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = HudAmber,
                        activeTrackColor = HudAmber
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (keyText.isNotBlank() && contentText.isNotBlank()) {
                        onConfirm(category, keyText, contentText, importance.toLong())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HudCyan,
                    contentColor = HudObsidian
                ),
                modifier = Modifier.testTag("dialog_save_memory_button")
            ) {
                Text("Commit")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
