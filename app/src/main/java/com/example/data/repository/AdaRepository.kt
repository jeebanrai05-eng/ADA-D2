package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CadModel
import com.example.data.model.MemoryItem
import com.example.data.model.PrinterNode
import com.example.data.model.SmartDevice
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AdaRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- PERMANENT MEMORY ---

    fun observeMemories(userId: String = requireUserId()): Flow<List<MemoryItem>> = flow {
        val path = "users/$userId/memories"
        emitAll(
            db.collection("users").document(userId).collection("memories")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MemoryItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.importance }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createMemory(item: MemoryItem): Result<String> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("memories")
        return try {
            val docRef = colRef.add(item.toCreateMap(uid)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun getMemoryById(ownerUid: String, memoryId: String): Result<MemoryItem?> {
        val docRef = db.collection("users").document(ownerUid).collection("memories").document(memoryId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    MemoryItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateMemory(memoryId: String, category: String, key: String, content: String, importance: Long): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("memories").document(memoryId)
        return try {
            docRef.update(
                mapOf(
                    "category" to category,
                    "key" to key,
                    "content" to content,
                    "importance" to importance.coerceIn(1L, 5L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMemory(memoryId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("memories").document(memoryId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- PARAMETRIC CAD MODELS ---

    fun observeCadModels(userId: String = requireUserId()): Flow<List<CadModel>> = flow {
        val path = "users/$userId/cad_models"
        emitAll(
            db.collection("users").document(userId).collection("cad_models")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CadModel::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createCadModel(model: CadModel): Result<String> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("cad_models")
        return try {
            val docRef = colRef.add(model.toCreateMap(uid)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCadModel(modelId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("cad_models").document(modelId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- KASA SMART HOME DEVICES ---

    fun observeSmartDevices(userId: String = requireUserId()): Flow<List<SmartDevice>> = flow {
        val path = "users/$userId/smart_devices"
        emitAll(
            db.collection("users").document(userId).collection("smart_devices")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SmartDevice::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createSmartDevice(device: SmartDevice): Result<String> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("smart_devices")
        return try {
            val docRef = colRef.add(device.toCreateMap(uid)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateSmartDeviceState(
        deviceId: String,
        isOn: Boolean,
        brightness: Long,
        powerWatts: Long,
        colorHex: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("smart_devices").document(deviceId)
        return try {
            docRef.update(
                mapOf(
                    "isOn" to isOn,
                    "brightness" to brightness.coerceIn(0L, 100L),
                    "powerWatts" to powerWatts.coerceIn(0L, 5000L),
                    "colorHex" to colorHex,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteSmartDevice(deviceId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("smart_devices").document(deviceId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 3D PRINTER NODES ---

    fun observePrinters(userId: String = requireUserId()): Flow<List<PrinterNode>> = flow {
        val path = "users/$userId/printers"
        emitAll(
            db.collection("users").document(userId).collection("printers")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PrinterNode::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createPrinter(printer: PrinterNode): Result<String> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("printers")
        return try {
            val docRef = colRef.add(printer.toCreateMap(uid)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePrinterTelemetry(
        printerId: String,
        status: String,
        activeJobName: String,
        progressPercent: Long,
        nozzleTemp: Long,
        nozzleTarget: Long,
        bedTemp: Long,
        bedTarget: Long
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("printers").document(printerId)
        return try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "activeJobName" to activeJobName,
                    "progressPercent" to progressPercent.coerceIn(0L, 100L),
                    "nozzleTemp" to nozzleTemp.coerceIn(0L, 350L),
                    "nozzleTarget" to nozzleTarget.coerceIn(0L, 350L),
                    "bedTemp" to bedTemp.coerceIn(0L, 150L),
                    "bedTarget" to bedTarget.coerceIn(0L, 150L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedInitialWorkshopIfEmpty(userId: String) {
        try {
            val memSnap = db.collection("users").document(userId).collection("memories")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            if (memSnap.isEmpty) {
                createMemory(
                    MemoryItem(
                        category = "project",
                        key = "ADA v2 Neural HUD",
                        content = "Multimodal AI assistant with Permanent Memory, Emotion AI, CAD Lab, Kasa IoT, and 3D Printer telemetry.",
                        importance = 5L
                    )
                )
                createMemory(
                    MemoryItem(
                        category = "preference",
                        key = "Communication Protocol",
                        content = "Prefers concise tactical telemetry with Half-Duplex Echo Guard enabled during voice synthesis.",
                        importance = 4L
                    )
                )
                createMemory(
                    MemoryItem(
                        category = "technical",
                        key = "Fabrication Stack",
                        content = "Uses build123d / OpenSCAD for parametric CAD models and Moonraker/Klipper for CoreXY 3D printing.",
                        importance = 4L
                    )
                )
            }

            val devSnap = db.collection("users").document(userId).collection("smart_devices")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            if (devSnap.isEmpty) {
                createSmartDevice(
                    SmartDevice(
                        name = "Arc Workbench LED Array",
                        room = "Main Lab",
                        deviceType = "strip",
                        ipAddress = "192.168.1.104",
                        isOn = true,
                        brightness = 90L,
                        powerWatts = 38L,
                        colorHex = "#00E5FF"
                    )
                )
                createSmartDevice(
                    SmartDevice(
                        name = "Soldering & Reflow Station",
                        room = "Main Lab",
                        deviceType = "plug",
                        ipAddress = "192.168.1.108",
                        isOn = true,
                        brightness = 100L,
                        powerWatts = 145L,
                        colorHex = "#F59E0B"
                    )
                )
                createSmartDevice(
                    SmartDevice(
                        name = "Ambient Halo Dimmer",
                        room = "Command Deck",
                        deviceType = "dimmer",
                        ipAddress = "192.168.1.112",
                        isOn = true,
                        brightness = 65L,
                        powerWatts = 18L,
                        colorHex = "#38BDF8"
                    )
                )
            }

            val prtSnap = db.collection("users").document(userId).collection("printers")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            if (prtSnap.isEmpty) {
                createPrinter(
                    PrinterNode(
                        name = "Voron 2.4 R2 CoreXY",
                        hostUrl = "http://192.168.1.150:7125",
                        status = "printing",
                        activeJobName = "arc_reactor_stator_v2.gcode",
                        progressPercent = 68L,
                        nozzleTemp = 245L,
                        nozzleTarget = 245L,
                        bedTemp = 105L,
                        bedTarget = 105L
                    )
                )
                createPrinter(
                    PrinterNode(
                        name = "Bambu X1-Carbon Lab",
                        hostUrl = "http://192.168.1.155:80",
                        status = "idle",
                        activeJobName = "planetary_gear_m3.gcode",
                        progressPercent = 100L,
                        nozzleTemp = 32L,
                        nozzleTarget = 0L,
                        bedTemp = 28L,
                        bedTarget = 0L
                    )
                )
            }

            val cadSnap = db.collection("users").document(userId).collection("cad_models")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            if (cadSnap.isEmpty) {
                createCadModel(
                    CadModel(
                        name = "Mark II Arc Stator Ring",
                        prompt = "High-tolerance concentric stator housing with 12 magnetic coil slots and M3 mounting tabs",
                        shapeType = "reactor",
                        scriptCode = """
                            from build123d import *
                            with BuildPart() as stator:
                                Cylinder(radius=55, height=18)
                                Cylinder(radius=34, height=18, mode=Mode.SUBTRACT)
                                with PolarLocations(radius=44, count=12):
                                    Cylinder(radius=4, height=18, mode=Mode.SUBTRACT)
                            export_stl(stator.part, "arc_reactor_stator_v2.stl")
                        """.trimIndent(),
                        dimensionsSummary = "110mm OD × 68mm ID × 18mm H (12 Coils)",
                        paramRadius = 55L,
                        paramHeight = 18L,
                        paramSegments = 12L
                    )
                )
            }
        } catch (_: Exception) {
            // Ignore seed errors if offline
        }
    }
}
