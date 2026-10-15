package com.delidehset.mobile.data

import android.content.Context
import android.util.LruCache
import java.io.File

/**
 * Disk + memory cache for page bodies.
 *
 * Every successful page load is stored keyed by its path, so reopening a menu renders
 * instantly from disk and then refreshes. When the network fails, the stale copy is
 * served instead of an error screen.
 */
class DeliCache(context: Context) : BodyCache {

    private companion object {
        const val MAX_MEMORY_ENTRIES = 120
        const val MAX_DISK_BYTES = 12L * 1024 * 1024 // 12 MB
    }

    private val dir = File(context.cacheDir, "deli_pages").apply { mkdirs() }

    private val memory = object : LruCache<String, CachedBody>(MAX_MEMORY_ENTRIES) {
        override fun sizeOf(key: String, value: CachedBody): Int = value.body.length.coerceAtLeast(1)
    }

    private fun fileFor(key: String): File {
        val safe = key.hashCode().toUInt().toString(16)
        return File(dir, "$safe.cache")
    }

    @Synchronized
    override fun read(key: String): CachedBody? {
        memory.get(key)?.let { return it }
        val f = fileFor(key)
        if (!f.exists()) return null
        return runCatching {
            val lines = f.readLines()
            if (lines.size < 2) return null
            val storedAt = lines[0].toLongOrNull() ?: return null
            val body = lines.drop(1).joinToString("\n")
            CachedBody(body, storedAt).also { memory.put(key, it) }
        }.getOrNull()
    }

    @Synchronized
    override fun write(key: String, body: String) {
        val entry = CachedBody(body, System.currentTimeMillis())
        memory.put(key, entry)
        runCatching {
            fileFor(key).writeText(entry.storedAtMs.toString() + "\n" + body)
            trimDiskIfNeeded()
        }
    }

    @Synchronized
    override fun clear() {
        memory.evictAll()
        runCatching { dir.listFiles()?.forEach { it.delete() } }
    }

    /** Keeps the on-disk cache bounded; drops the oldest entries first. */
    private fun trimDiskIfNeeded() {
        val files = dir.listFiles() ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_DISK_BYTES) return
        files.sortedBy { it.lastModified() }.forEach { f ->
            if (total <= MAX_DISK_BYTES) return
            total -= f.length()
            f.delete()
        }
    }
}
