package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CadModel
import com.example.data.model.MemoryItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveMemory_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser("alice@adav2.test")
        val repository = AdaRepository(firestore)

        val createResult = withTimeout(5000L) {
            repository.createMemory(
                MemoryItem(
                    category = "project",
                    key = "Arc Core v2",
                    content = "Magnetic containment field calibrated at 3.2 GJ/s",
                    importance = 5L
                )
            )
        }
        assertTrue("Expected memory creation to succeed", createResult.isSuccess)
        val memoryId = createResult.getOrThrow()

        val emitted = withTimeout(3000L) {
            repository.observeMemories(aliceUid).first { list -> list.any { it.id == memoryId } }
        }
        assertTrue(emitted.any { it.id == memoryId && it.key == "Arc Core v2" })
    }

    @Test
    fun createCadModel_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser("alice@adav2.test")
        val repository = AdaRepository(firestore)

        val createResult = withTimeout(5000L) {
            repository.createCadModel(
                CadModel(
                    name = "Rotor Bracket",
                    prompt = "CNC aluminum rotor bracket",
                    shapeType = "bracket",
                    scriptCode = "Cylinder(30, 10)",
                    dimensionsSummary = "60x60x10mm",
                    paramRadius = 30L,
                    paramHeight = 10L,
                    paramSegments = 16L
                )
            )
        }
        assertTrue(createResult.isSuccess)
        val modelId = createResult.getOrThrow()

        val models = withTimeout(3000L) {
            repository.observeCadModels(aliceUid).first { list -> list.any { it.id == modelId } }
        }
        assertTrue(models.any { it.id == modelId })
    }

    @Test
    fun getMemoryById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice@adav2.test")
        val aliceRepo = AdaRepository(firestore)
        val memoryId = withTimeout(5000L) {
            aliceRepo.createMemory(
                MemoryItem(
                    category = "personal",
                    key = "Vault Pin",
                    content = "Classified Stark Protocol",
                    importance = 5L
                )
            ).getOrThrow()
        }

        signInTestUser("bob@adav2.test")
        val bobRepo = AdaRepository(firestore)
        val crossRead = withTimeout(5000L) {
            bobRepo.getMemoryById(aliceUid, memoryId)
        }
        assertTrue("Expected cross-user read to fail", crossRead.isFailure)
        val rootEx = crossRead.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", rootEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, rootEx?.code)
    }

    @Test
    fun observeMemories_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice@adav2.test")
        auth.signOut()
        val repository = AdaRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(3000L) {
                repository.observeMemories(aliceUid).first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull("Expected unauthenticated observeMemories to throw", caught)
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
}
