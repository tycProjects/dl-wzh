package com.example.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteFullException
import android.os.StatFs
import android.system.ErrnoException
import android.system.OsConstants
import android.util.Log
import java.io.IOException

/**
 * Protects internal storage on low-space devices (such as Xiaomi Redmi 10C) by checking
 * available bytes on the internal app data partition before performing persistent writes,
 * and detecting OS/SQLite out-of-space errors without ever deleting or duplicating user media.
 */
object StorageGuard {
    private const val TAG = "JNMF_StorageGuard"

    /**
     * Minimum free internal storage (256 KB) required to safely commit small Room / DataStore
     * metadata updates without risking partial writes or SQLite journal page exhaustion.
     */
    const val MIN_SAFE_METADATA_WRITE_BYTES: Long = 256L * 1024L

    /**
     * Test-only hooks for deterministic simulation of nearly-full / 0-byte storage and I/O errors.
     */
    @Volatile
    internal var availableBytesOverrideForTesting: Long? = null

    @Volatile
    internal var simulateWriteExceptionForTesting: Throwable? = null

    internal fun resetTestingOverrides() {
        availableBytesOverrideForTesting = null
        simulateWriteExceptionForTesting = null
    }

    fun getAvailableInternalBytes(context: Context): Long {
        availableBytesOverrideForTesting?.let { return it }
        return try {
            val dir = context.applicationContext.filesDir
            val stat = StatFs(dir.absolutePath)
            if (stat.totalBytes > 0L) {
                stat.availableBytes
            } else if (dir.totalSpace > 0L) {
                dir.usableSpace
            } else {
                Long.MAX_VALUE
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query StatFs availableBytes: ${e.message}")
            Long.MAX_VALUE
        }
    }

    fun hasEnoughSpaceForWrite(
        context: Context,
        requiredBytes: Long = MIN_SAFE_METADATA_WRITE_BYTES
    ): Boolean {
        val available = getAvailableInternalBytes(context)
        return available >= requiredBytes.coerceAtLeast(0L)
    }

    fun throwIfSimulatedFailure() {
        simulateWriteExceptionForTesting?.let { throw it }
    }

    fun isOutOfSpaceError(throwable: Throwable?): Boolean {
        var current: Throwable? = throwable
        var depth = 0
        while (current != null && depth < 10) {
            when (current) {
                is SQLiteFullException -> return true
                is SQLiteDiskIOException -> {
                    val msg = current.message.orEmpty().lowercase()
                    if (msg.contains("full") || msg.contains("enospc") || msg.contains("no space")) {
                        return true
                    }
                }
                is ErrnoException -> {
                    if (current.errno == OsConstants.ENOSPC) return true
                }
                is IOException -> {
                    val msg = current.message.orEmpty().lowercase()
                    if (msg.contains("enospc") || msg.contains("no space left") || msg.contains("disk full")) {
                        return true
                    }
                }
            }
            val msg = current.message.orEmpty().lowercase()
            if (msg.contains("database or disk is full") || msg.contains("no space left on device")) {
                return true
            }
            current = current.cause
            depth++
        }
        return false
    }
}
