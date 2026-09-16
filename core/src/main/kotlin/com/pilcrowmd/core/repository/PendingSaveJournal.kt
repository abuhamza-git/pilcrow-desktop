// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 pleree

package com.pilcrowmd.core.repository

import java.nio.file.Path
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

internal class PendingSaveJournal(walBaseDir: File) {

    private val dir = File(walBaseDir, "pending_saves")

    data class Entry(val key: String, val path: Path, val file: File)

    fun stage(path: Path, bytes: ByteArray): String {
        dir.mkdirs()
        val key = keyFor(path)
        atomicWrite(File(dir, "$key.content"), bytes)
        atomicWrite(File(dir, "$key.path"), path.toString().toByteArray(Charsets.UTF_8))
        atomicWrite(File(dir, "$key.committing"), ByteArray(0))
        return key
    }

    fun recoverableContentFor(path: Path): File? {
        val key = keyFor(path)
        val content = File(dir, "$key.content")
        return if (content.exists() && File(dir, "$key.committing").exists()) content else null
    }

    fun discard(key: String) {
        listOf("$key.content", "$key.path", "$key.committing", "$key.content.tmp", "$key.path.tmp")
            .forEach { File(dir, it).delete() }
    }

    fun recoverableEntries(): List<Entry> {
        if (!dir.isDirectory) return emptyList()
        val keys = dir.listFiles()
            ?.filter { it.name.endsWith(".content") }
            ?.map { it.name.removeSuffix(".content") }
            ?: emptyList()

        val entries = ArrayList<Entry>()
        for (key in keys) {
            val pathFile = File(dir, "$key.path")
            if (!pathFile.exists() || !File(dir, "$key.committing").exists()) {
                discard(key)
                continue
            }
            entries += Entry(key, Path.of(pathFile.readText()), File(dir, "$key.content"))
        }
        sweepOrphans()
        return entries
    }

    fun strandedEntries(): List<Entry> {
        if (!dir.isDirectory) return emptyList()
        val keys = dir.listFiles()
            ?.filter { it.name.endsWith(".content") }
            ?.map { it.name.removeSuffix(".content") }
            ?: emptyList()

        val entries = ArrayList<Entry>()
        for (key in keys) {
            val pathFile = File(dir, "$key.path")
            if (pathFile.exists() && File(dir, "$key.committing").exists()) {
                entries += Entry(key, Path.of(pathFile.readText()), File(dir, "$key.content"))
            }
        }
        return entries
    }

    private fun keyFor(path: Path): String = MessageDigest.getInstance("SHA-256")
        .digest(path.toString().toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun atomicWrite(finalFile: File, bytes: ByteArray) {
        val tmp = File(finalFile.parentFile, "${finalFile.name}.tmp")
        FileOutputStream(tmp).use { fos ->
            fos.write(bytes)
            fos.flush()
            fos.fd.sync()
        }
        Files.move(
            tmp.toPath(),
            finalFile.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

    private fun sweepOrphans() {
        dir.listFiles()?.forEach { f ->
            when {
                f.name.endsWith(".tmp") -> f.delete()
                f.name.endsWith(".path") || f.name.endsWith(".committing") -> {
                    val base = f.name.substringBeforeLast('.')
                    if (!File(dir, "$base.content").exists()) f.delete()
                }
            }
        }
    }
}
