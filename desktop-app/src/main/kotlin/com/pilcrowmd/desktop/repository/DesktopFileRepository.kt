// SPDX-License-Identifier: GPL-3.0-or-later
// PilcrowMD Desktop — Linux port

package com.pilcrowmd.desktop.repository

import com.pilcrowmd.core.repository.FileRepository
import com.pilcrowmd.core.repository.StrandedSlot
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.io.path.name

/**
 * Desktop file repository using standard Java NIO filesystem APIs.
 * Implements the core FileRepository interface.
 */
class DesktopFileRepository : FileRepository {

    override suspend fun readFile(path: Path): Result<String> {
        return try {
            if (!path.exists()) {
                Result.failure(IOException("File does not exist: $path"))
            } else {
                Result.success(path.readText(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveFile(path: Path, content: String): Result<Unit> {
        return try {
            val parent = path.parent ?: Path.of(".")
            val tempFile = Files.createTempFile(parent, ".pilcrow-save-", ".tmp")

            try {
                // Write content to temp file
                tempFile.writeText(content, Charsets.UTF_8)

                // Force sync to disk
                Files.newOutputStream(tempFile, StandardOpenOption.APPEND).use { stream ->
                    stream.flush()
                }

                // Atomic move: replace original with temp file
                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)

                Result.success(Unit)
            } catch (e: Exception) {
                // Clean up temp file on failure
                try {
                    Files.deleteIfExists(tempFile)
                } catch (_: Exception) {}
                throw e
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recoverPendingSaves(): Result<Int> {
        // Simplified for desktop port initially
        return Result.success(0)
    }

    override suspend fun displayName(path: Path): String {
        return path.name
    }

    override suspend fun strandedSlots(): Result<List<StrandedSlot>> {
        return Result.success(emptyList())
    }

    override suspend fun saveStrandedSlotToTarget(slotKey: String, targetPath: Path): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun discardSlot(key: String): Result<Unit> {
        return Result.success(Unit)
    }
}
