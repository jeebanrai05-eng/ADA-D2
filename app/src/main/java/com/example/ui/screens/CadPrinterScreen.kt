package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CadModel
import com.example.data.model.PrinterNode
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
import com.example.viewmodel.DataUiState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CadPrinterScreen(
    cadModelsState: DataUiState<List<CadModel>>,
    printersState: DataUiState<List<PrinterNode>>,
    cadTaskState: AiTaskState,
    onGenerateCad: (String) -> Unit,
    onDeleteCad: (String) -> Unit,
    onDispatchToPrinter: (CadModel, PrinterNode) -> Unit,
    onUpdatePrinterStatus: (PrinterNode, String) -> Unit
) {
    var cadPrompt by remember { mutableStateOf("") }
    var selectedModelId by remember { mutableStateOf<String?>(null) }
    var showScriptCode by remember { mutableStateOf(false) }

    var yawAngle by remember { mutableFloatStateOf(35f) }
    var pitchAngle by remember { mutableFloatStateOf(24f) }

    val models = (cadModelsState as? DataUiState.Success)?.data.orEmpty()
    val printers = (printersState as? DataUiState.Success)?.data.orEmpty()
    val activeModel = models.find { it.id == selectedModelId } ?: models.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Parametric CAD Synthesizer Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HudPanelSurface),
            border = BorderStroke(1.dp, HudCyan.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Architecture,
                        contentDescription = null,
                        tint = HudCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "PARAMETRIC 3D CAD STUDIO",
                            style = MaterialTheme.typography.titleLarge,
                            color = HudCyan
                        )
                        Text(
                            text = "Gemini 3.8 Flash build123d / OpenSCAD Wireframe Synthesizer",
                            style = MaterialTheme.typography.labelSmall,
                            color = HudMutedText
                        )
                    }
                }

                // Quick CAD Templates
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        "24T Planetary Gear" to "24-tooth M3 planetary gear 60mm diameter with 14mm bore",
                        "Quadcopter Frame" to "Carbon fiber 5-inch quadcopter central motor mount frame",
                        "Arc Stator Ring" to "Concentric magnetic stator housing with 16 cooling fins",
                        "Servo L-Bracket" to "Heavy-duty NEMA-17 stepper motor L-bracket with 4 M4 slots"
                    )
                    presets.forEach { (label, prompt) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HudPanelElevated,
                            border = BorderStroke(1.dp, HudSkyBlue.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                cadPrompt = prompt
                                onGenerateCad(prompt)
                            }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = HudSkyBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = cadPrompt,
                        onValueChange = { cadPrompt = it },
                        placeholder = {
                            Text("Describe 3D part, tolerances & dimensions…", color = HudMutedText.copy(alpha = 0.6f))
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
                            .weight(1f)
                            .testTag("cad_prompt_input")
                    )

                    Button(
                        onClick = {
                            if (cadPrompt.isNotBlank()) {
                                onGenerateCad(cadPrompt)
                                cadPrompt = ""
                            }
                        },
                        enabled = cadTaskState !is AiTaskState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HudCyan,
                            contentColor = HudObsidian
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("generate_cad_button")
                    ) {
                        Text("Synthesize", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (cadTaskState is AiTaskState.Loading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = HudCyan,
                        trackColor = HudPanelElevated
                    )
                } else if (cadTaskState is AiTaskState.Error) {
                    Text(
                        text = cadTaskState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = HudCrimson
                    )
                }
            }
        }

        // Interactive 3D Wireframe Viewport
        activeModel?.let { model ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HudDeepNavy),
                border = BorderStroke(1.5.dp, HudCyan.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cad_viewport_card")
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.name.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = HudIceWhite
                            )
                            Text(
                                text = "${model.shapeType.uppercase()} // ${model.dimensionsSummary}",
                                style = MaterialTheme.typography.labelSmall,
                                color = HudCyan
                            )
                        }

                        Row {
                            IconButton(onClick = { showScriptCode = !showScriptCode }) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Toggle CAD script",
                                    tint = if (showScriptCode) HudEmerald else HudSkyBlue
                                )
                            }
                            IconButton(onClick = { onDeleteCad(model.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete CAD model",
                                    tint = HudCrimson
                                )
                            }
                        }
                    }

                    // 3D Isometric / Perspective Wireframe Canvas (Drag to rotate)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HudObsidian)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    yawAngle = (yawAngle + dragAmount.x * 0.45f) % 360f
                                    pitchAngle = (pitchAngle + dragAmount.y * 0.35f).coerceIn(-75f, 75f)
                                }
                            }
                    ) {
                        Wireframe3DCanvas(
                            model = model,
                            yawDeg = yawAngle,
                            pitchDeg = pitchAngle,
                            modifier = Modifier.fillMaxSize()
                        )

                        Text(
                            text = "DRAG TO ROTATE 3D WIREFRAME // YAW:${yawAngle.toInt()}° PITCH:${pitchAngle.toInt()}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = HudCyanDim,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        )
                    }

                    if (showScriptCode) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = HudObsidian,
                            border = BorderStroke(1.dp, HudEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = model.scriptCode,
                                style = MaterialTheme.typography.labelSmall,
                                color = HudEmerald,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Model Selector Chips if multiple models exist
                    if (models.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            models.forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (item.id == model.id) HudCyan.copy(alpha = 0.25f) else HudPanelElevated,
                                    border = BorderStroke(1.dp, if (item.id == model.id) HudCyan else HudCyanDim.copy(alpha = 0.3f)),
                                    modifier = Modifier.clickable { selectedModelId = item.id }
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.id == model.id) HudCyan else HudIceWhite,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Dispatch to 3D Printer Row
                    if (printers.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            printers.forEach { printer ->
                                Button(
                                    onClick = { onDispatchToPrinter(model, printer) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HudEmerald,
                                        contentColor = HudObsidian
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dispatch_printer_${printer.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Print on ${printer.name.take(12)}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3D Printer Telemetry Fleet Section
        Text(
            text = "3D PRINTER TELEMETRY FLEET (MOONRAKER / OCTOPRINT)",
            style = MaterialTheme.typography.labelLarge,
            color = HudSkyBlue
        )

        printers.forEach { printer ->
            PrinterNodeCard(
                printer = printer,
                onStatusChange = { newStatus -> onUpdatePrinterStatus(printer, newStatus) }
            )
        }
    }
}

@Composable
private fun Wireframe3DCanvas(
    model: CadModel,
    yawDeg: Float,
    pitchDeg: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = (size.minDimension / 160f)

        // Draw HUD reference grid
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        drawLine(
            color = HudCyanDim.copy(alpha = 0.25f),
            start = Offset(0f, cy),
            end = Offset(size.width, cy),
            pathEffect = dash
        )
        drawLine(
            color = HudCyanDim.copy(alpha = 0.25f),
            start = Offset(cx, 0f),
            end = Offset(cx, size.height),
            pathEffect = dash
        )

        val yaw = Math.toRadians(yawDeg.toDouble())
        val pitch = Math.toRadians(pitchDeg.toDouble())

        fun project3D(x: Double, y: Double, z: Double): Offset {
            // Rotate around Y (yaw)
            val x1 = x * cos(yaw) - z * sin(yaw)
            val z1 = x * sin(yaw) + z * cos(yaw)
            // Rotate around X (pitch)
            val y2 = y * cos(pitch) - z1 * sin(pitch)
            return Offset(
                x = (cx + x1 * scale).toFloat(),
                y = (cy - y2 * scale).toFloat()
            )
        }

        val rOuter = model.paramRadius.coerceIn(20L, 65L).toDouble()
        val rInner = rOuter * 0.55
        val halfH = (model.paramHeight.coerceIn(10L, 45L) / 2.0)
        val segments = model.paramSegments.coerceIn(6L, 28L).toInt()

        val topOuter = ArrayList<Offset>(segments)
        val botOuter = ArrayList<Offset>(segments)
        val topInner = ArrayList<Offset>(segments)
        val botInner = ArrayList<Offset>(segments)

        for (i in 0 until segments) {
            val angle = 2.0 * Math.PI * i / segments
            val gearMod = if (model.shapeType == "gear" && i % 2 == 0) 1.15 else 1.0
            val xo = rOuter * gearMod * cos(angle)
            val zo = rOuter * gearMod * sin(angle)
            val xi = rInner * cos(angle)
            val zi = rInner * sin(angle)

            topOuter.add(project3D(xo, halfH, zo))
            botOuter.add(project3D(xo, -halfH, zo))
            topInner.add(project3D(xi, halfH, zi))
            botInner.add(project3D(xi, -halfH, zi))
        }

        for (i in 0 until segments) {
            val next = (i + 1) % segments
            // Outer rings & vertical struts
            drawLine(HudCyan, topOuter[i], topOuter[next], strokeWidth = 2.2f)
            drawLine(HudCyan.copy(alpha = 0.6f), botOuter[i], botOuter[next], strokeWidth = 1.8f)
            drawLine(HudSkyBlue.copy(alpha = 0.7f), topOuter[i], botOuter[i], strokeWidth = 1.6f)

            // Inner bore rings & radial ribs
            drawLine(HudEmerald, topInner[i], topInner[next], strokeWidth = 2f)
            drawLine(HudEmerald.copy(alpha = 0.6f), botInner[i], botInner[next], strokeWidth = 1.5f)
            if (i % 2 == 0) {
                drawLine(HudCyanDim, topInner[i], topOuter[i], strokeWidth = 1.4f)
                drawLine(HudCyanDim.copy(alpha = 0.5f), topInner[i], botInner[i], strokeWidth = 1.2f)
            }
        }
    }
}

@Composable
private fun PrinterNodeCard(
    printer: PrinterNode,
    onStatusChange: (String) -> Unit
) {
    val statusColor = when (printer.status) {
        "printing" -> HudEmerald
        "heating" -> HudAmber
        "paused" -> HudAmber
        else -> HudSkyBlue
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HudPanelSurface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("printer_card_${printer.id}")
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
                Column {
                    Text(
                        text = printer.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = HudIceWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${printer.hostUrl} // JOB: ${printer.activeJobName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = HudMutedText
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = printer.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Thermal Readout Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = HudAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "NOZZLE: ${printer.nozzleTemp}°C / ${printer.nozzleTarget}°C",
                        style = MaterialTheme.typography.labelSmall,
                        color = HudIceWhite
                    )
                }

                Text(
                    text = "BED: ${printer.bedTemp}°C / ${printer.bedTarget}°C",
                    style = MaterialTheme.typography.labelSmall,
                    color = HudSkyBlue
                )

                Text(
                    text = "${printer.progressPercent}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = HudCyan
                )
            }

            LinearProgressIndicator(
                progress = { (printer.progressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = HudPanelElevated
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onStatusChange("printing") },
                    border = BorderStroke(1.dp, HudEmerald),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = HudEmerald, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resume/Step", style = MaterialTheme.typography.labelSmall, color = HudEmerald)
                }

                OutlinedButton(
                    onClick = { onStatusChange("paused") },
                    border = BorderStroke(1.dp, HudAmber),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null, tint = HudAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pause", style = MaterialTheme.typography.labelSmall, color = HudAmber)
                }

                OutlinedButton(
                    onClick = { onStatusChange("idle") },
                    border = BorderStroke(1.dp, HudCrimson),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = HudCrimson, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Abort", style = MaterialTheme.typography.labelSmall, color = HudCrimson)
                }
            }
        }
    }
}
