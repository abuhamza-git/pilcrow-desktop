// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 pleree

package com.pilcrowmd.core.repository

import java.nio.file.Path

/**
 * A WAL save that could not be committed and remains stranded after launch recovery — the target Path
 * became permanently inaccessible so [recoverPendingSaves] can never
 * land it. Surfaced so the user can rescue the bytes elsewhere or discard them (never auto-discarded).
 *
 * @param key   the journal slot key (stable per target).
 * @param path   the original (now-inaccessible) target Path.
 * @param displayName best-effort name for the original target (for the user to recognise it).
 */
data class StrandedSlot(val key: String, val path: Path, val displayName: String)

/**
 * Abstraction for file access. 
 */
interface FileRepository {
    /**
     * Read the content of a file by Path. 
     */
    suspend fun readFile(path: Path): Result<String>

    /**
     * Save content back to a file at the given Path. Crash-safe (Safeguard 1, no data loss):
     * the full new content is staged durably on disk and fsynced BEFORE the target is truncated,
     * so process death in the write window never loses the user's content — it is recovered by
     * [recoverPendingSaves] on the next launch.
     */
    suspend fun saveFile(path: Path, content: String): Result<Unit>

    /**
     * Re-apply any save that was interrupted before it could complete (Safeguard 1).
     * Called once per process at launch. Idempotent and never throws to the caller;
     * returns the number of pending saves successfully recovered.
     */
    suspend fun recoverPendingSaves(): Result<Int>

    /** Best-effort display name for a Path. */
    suspend fun displayName(path: Path): String

    /**
     * The WAL slots still pending after launch recovery — i.e. saves that could not be committed and
     * are stranded (Safeguard 1: their content is preserved but not yet actionable). Acquires the
     * same journal lock as [saveFile]/[recoverPendingSaves], so a call suspends until launch-time
     * recovery finishes; whatever it returns is, by definition, stranded. Read-only — no slot is
     * discarded and no write-ordering is touched.
     */
    suspend fun strandedSlots(): Result<List<StrandedSlot>>

    /**
     * Rescue a stranded slot ([slotKey]) by writing its **raw staged bytes verbatim** to a new
     * user-chosen [targetPath] (the bytes were already line-ending-processed when staged, so they are
     * NOT re-processed — Safeguard 2), then discarding the slot on success. The currently-open
     * document is never read or modified. Reuses the unchanged file-commit primitive; on failure the
     * slot is kept (never auto-discarded).
     */
    suspend fun saveStrandedSlotToTarget(slotKey: String, targetPath: Path): Result<Unit>

    /**
     * Explicitly discard a stranded slot ([key]) at the user's request (the "Discard" action). Only
     * ever user-initiated — recovery/error paths must never auto-discard (Safeguard 1).
     */
    suspend fun discardSlot(key: String): Result<Unit>
}
