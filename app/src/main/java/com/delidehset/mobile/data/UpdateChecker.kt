package com.delidehset.mobile.data

import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/** What the latest release on GitHub is, and which file in it is the app. */
data class UpdateInfo(
    /** `1.1.0`, with the tag's `v` stripped: the same shape as `BuildConfig.VERSION_NAME`. */
    val version: String,
    val apkUrl: String,
    val sizeBytes: Long,
    /** The `sha256:…` digest GitHub publishes for the asset, without its prefix. */
    val sha256: String?,
    val pageUrl: String,
)

/**
 * The app's own updater source: the releases of its GitHub repository.
 *
 * The app is not on Play, so Play's in-app update API has nothing to offer here — but the
 * repository already publishes a signed APK on every release, and GitHub publishes its
 * size and SHA-256 next to it. That is everything an updater needs, and it keeps working
 * as long as the APKs are signed with the same key, which is what makes the install an
 * update rather than a second app.
 */
object UpdateChecker {
    const val OWNER = "eyyun"
    const val REPO = "hoset"

    private const val LATEST = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /** The latest published release, or null when there is none to be had. */
    suspend fun latest(versionName: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", userAgent(versionName))
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val release = json.decodeFromString<Release>(body)
                val asset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                    ?: return@use null
                UpdateInfo(
                    version = release.tag.removePrefix("v").trim(),
                    apkUrl = asset.downloadUrl,
                    sizeBytes = asset.size,
                    sha256 = asset.digest?.substringAfter("sha256:", "")?.takeIf { it.isNotBlank() },
                    pageUrl = release.pageUrl,
                )
            }
        }.getOrNull()
    }

    /**
     * Downloads the APK to [target], reporting the fraction done as it goes.
     *
     * The file is checked against the SHA-256 the release advertises before it is handed
     * back: a truncated download or a swapped asset would otherwise reach the installer,
     * and the installer's own check (a signature) only speaks about the signer.
     */
    suspend fun download(
        info: UpdateInfo,
        target: File,
        versionName: String,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(info.apkUrl)
            .header("User-Agent", userAgent(versionName))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw UpdateException("HTTP ${response.code}")
            val body = response.body ?: throw UpdateException("empty response")
            val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
            val digest = MessageDigest.getInstance("SHA-256")

            target.parentFile?.mkdirs()
            target.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        done += read
                        if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }

            val sha = digest.digest().joinToString("") { "%02x".format(it) }
            val expected = info.sha256
            if (expected != null && !sha.equals(expected, ignoreCase = true)) {
                target.delete()
                throw UpdateException("checksum")
            }
            onProgress(1f)
            target
        }
    }

    /**
     * Whether [candidate] is a later version than [current].
     *
     * Compared number by number, not as text: "1.10.0" is later than "1.9.0", which string
     * ordering gets wrong. A pre-release suffix (`1.2.0-beta1`) is ignored, since the
     * latest-release endpoint does not serve pre-releases anyway.
     */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = numbersOf(candidate)
        val b = numbersOf(current)
        if (a.isEmpty()) return false
        for (index in 0 until maxOf(a.size, b.size)) {
            val next = a.getOrElse(index) { 0 }
            val here = b.getOrElse(index) { 0 }
            if (next != here) return next > here
        }
        return false
    }

    private fun numbersOf(version: String): List<Int> =
        Regex("\\d+")
            .findAll(version.substringBefore('-').substringBefore('+'))
            .mapNotNull { it.value.toIntOrNull() }
            .toList()

    private fun userAgent(versionName: String) = "Hoset/$versionName (+https://github.com/$OWNER/$REPO)"

    @Serializable
    private data class Release(
        @SerialName("tag_name") val tag: String = "",
        @SerialName("body") val notes: String = "",
        @SerialName("html_url") val pageUrl: String = "",
        val assets: List<Asset> = emptyList(),
    )

    @Serializable
    private data class Asset(
        val name: String = "",
        @SerialName("browser_download_url") val downloadUrl: String = "",
        val size: Long = 0,
        val digest: String? = null,
    )
}

/* ------------------------------------------------------------------ */
/*  What a check means                                                  */
/* ------------------------------------------------------------------ */

/** The whole answer a check can have, kept out of the ViewModel so it can be tested. */
enum class UpdateDecision {
    /** Said nothing: nothing to show, and nothing to remember. */
    NONE,
    AVAILABLE,
    UP_TO_DATE,
    /** The check did not complete — which is not the same as being up to date. */
    FAILED,
}

/**
 * What to do with a check's outcome.
 *
 * Two failures lived here. A check that could not be made used to be written down as
 * "checked just now", which kept the app from asking again for a whole day — from the
 * outside, an app that never looks is an app that believes it is current. And a version
 * the person had already put off was skipped even when they asked about it themselves,
 * so a manual check answered "you are on the latest version" while a newer release sat
 * on GitHub. Neither is decided in the ViewModel any more.
 *
 * @param latestVersion the tag the server returned, or null when nothing came back
 * @param skippedVersion the version the person last put off, if any
 */
internal fun decideUpdate(
    latestVersion: String?,
    currentVersion: String,
    auto: Boolean,
    skippedVersion: String?,
): UpdateDecision = when {
    latestVersion == null -> if (auto) UpdateDecision.NONE else UpdateDecision.FAILED
    UpdateChecker.isNewer(latestVersion, currentVersion) -> {
        // "Later" silences the automatic check, never a person who asked.
        if (auto && skippedVersion == latestVersion) UpdateDecision.NONE else UpdateDecision.AVAILABLE
    }
    auto -> UpdateDecision.NONE
    else -> UpdateDecision.UP_TO_DATE
}

/** A download that did not arrive intact, or did not arrive at all. */
class UpdateException(message: String) : Exception(message)
