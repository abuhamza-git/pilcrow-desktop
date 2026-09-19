package com.pilcrowmd.desktop.repository

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopFileRepositoryTest {

    private lateinit var repo: DesktopFileRepository
    private lateinit var testDir: Path
    private lateinit var walDir: Path

    @BeforeEach
    fun setup() {
        testDir = Files.createTempDirectory("pilcrow-test")
        walDir = testDir.resolve("wal")
        repo = DesktopFileRepository(walDir)
    }

    @AfterEach
    fun teardown() {
        testDir.toFile().deleteRecursively()
    }

    @Test
    fun `saveFile atomicity and disk sync works`() = runTest {
        val file = testDir.resolve("doc.md")
        val content = "Hello World"
        
        val result = repo.saveFile(file, content)
        assertTrue(result.isSuccess)
        assertTrue(file.exists())
        assertEquals(content, file.readText())
        
        // WAL should be clean
        assertEquals(0, repo.strandedSlots().getOrNull()?.size)
    }

    @Test
    fun `stranded WAL slots are recovered correctly`() = runTest {
        // Simulate a crash during save
        val file = testDir.resolve("target.md")
        val slotKey = "crash-123"
        
        Files.createDirectories(walDir)
        val metaFile = walDir.resolve("$slotKey.meta")
        metaFile.writeText(file.toAbsolutePath().toString())
        
        // Create the stranded temp file
        val tempFile = testDir.resolve(".pilcrow-save-$slotKey.tmp")
        val strandedContent = "This was saved but not moved"
        tempFile.writeText(strandedContent)
        
        // At this point, the file doesn't exist, but the temp and meta do
        assertTrue(!file.exists())
        
        // Launch recovery
        val recovered = repo.recoverPendingSaves().getOrThrow()
        
        assertEquals(1, recovered)
        assertTrue(file.exists())
        assertEquals(strandedContent, file.readText())
        
        // Meta and temp should be gone
        assertTrue(!metaFile.exists())
        assertTrue(!tempFile.exists())
    }
}
