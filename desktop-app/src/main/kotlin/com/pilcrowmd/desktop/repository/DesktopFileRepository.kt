package com.pilcrowmd.desktop.repository

import com.pilcrowmd.core.repository.FileRepository
import com.pilcrowmd.core.repository.StrandedSlot
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlin.io.path.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopFileRepository(private val customWalDir: Path? = null) : FileRepository {

    private val walDir = customWalDir ?: Path.of(System.getProperty("user.home"), ".config", "pilcrow", "wal")

    init {
        Files.createDirectories(walDir)
    }

    override suspend fun readFile(path: Path): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!path.exists()) {
                Result.failure(IOException("File does not exist: $path"))
            } else {
                Result.success(path.readText(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveFile(path: Path, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val parent = path.parent ?: Path.of(".")
            val slotKey = UUID.randomUUID().toString()
            val tempFile = parent.resolve(".pilcrow-save-$slotKey.tmp")
            val metaFile = walDir.resolve("$slotKey.meta")

            // 1. Write WAL meta
            metaFile.writeText(path.toAbsolutePath().toString(), Charsets.UTF_8)

            try {
                // 2. Write content to temp file
                tempFile.writeText(content, Charsets.UTF_8)

                // 3. Force sync to physical disk
                FileOutputStream(tempFile.toFile(), true).use { stream ->
                    stream.fd.sync()
                }

                // 4. Atomic move
                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)

                // 5. Success, delete meta
                metaFile.deleteIfExists()
                
                Result.success(Unit)
            } catch (e: Exception) {
                // Keep the WAL meta and temp file around if it was a crash, but if it was just an exception here, we can clean up if we want.
                // Actually, if we get an exception before move, we clean up the temp file and meta.
                try {
                    Files.deleteIfExists(tempFile)
                    Files.deleteIfExists(metaFile)
                } catch (_: Exception) {}
                throw e
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recoverPendingSaves(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var recovered = 0
            if (!walDir.exists()) return@withContext Result.success(0)
            
            val metaFiles = Files.list(walDir).filter { it.name.endsWith(".meta") }.toList()
            for (metaFile in metaFiles) {
                val slotKey = metaFile.name.removeSuffix(".meta")
                val targetPathStr = metaFile.readText(Charsets.UTF_8)
                val targetPath = Path.of(targetPathStr)
                val parent = targetPath.parent ?: Path.of(".")
                val tempFile = parent.resolve(".pilcrow-save-$slotKey.tmp")
                
                if (tempFile.exists()) {
                    try {
                        Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                        metaFile.deleteIfExists()
                        recovered++
                    } catch (e: Exception) {
                        // Failed to move (e.g. permission denied or missing parent directory). Leave it as a stranded slot.
                    }
                } else {
                    // Temp file is gone, probably crashed before writing it, so just delete meta
                    metaFile.deleteIfExists()
                }
            }
            Result.success(recovered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun displayName(path: Path): String {
        return path.name
    }

    override suspend fun strandedSlots(): Result<List<StrandedSlot>> = withContext(Dispatchers.IO) {
        try {
            val slots = mutableListOf<StrandedSlot>()
            if (walDir.exists()) {
                val metaFiles = Files.list(walDir).filter { it.name.endsWith(".meta") }.toList()
                for (metaFile in metaFiles) {
                    val slotKey = metaFile.name.removeSuffix(".meta")
                    val targetPath = Path.of(metaFile.readText(Charsets.UTF_8))
                    val parent = targetPath.parent ?: Path.of(".")
                    val tempFile = parent.resolve(".pilcrow-save-$slotKey.tmp")
                    if (tempFile.exists()) {
                        slots.add(StrandedSlot(slotKey, targetPath, targetPath.name))
                    }
                }
            }
            Result.success(slots)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveStrandedSlotToTarget(slotKey: String, targetPath: Path): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val metaFile = walDir.resolve("$slotKey.meta")
            if (metaFile.exists()) {
                val origPath = Path.of(metaFile.readText(Charsets.UTF_8))
                val parent = origPath.parent ?: Path.of(".")
                val tempFile = parent.resolve(".pilcrow-save-$slotKey.tmp")
                if (tempFile.exists()) {
                    // Just move to the NEW target
                    Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                    metaFile.deleteIfExists()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun discardSlot(key: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val metaFile = walDir.resolve("$key.meta")
            if (metaFile.exists()) {
                val origPath = Path.of(metaFile.readText(Charsets.UTF_8))
                val parent = origPath.parent ?: Path.of(".")
                val tempFile = parent.resolve(".pilcrow-save-$key.tmp")
                Files.deleteIfExists(tempFile)
                metaFile.deleteIfExists()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
