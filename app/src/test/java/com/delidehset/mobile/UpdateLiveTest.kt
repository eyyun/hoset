package com.delidehset.mobile

import com.delidehset.mobile.data.UpdateChecker
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * The updater against the real thing: the release that is actually published, the APK that
 * is actually attached to it, and the SHA-256 GitHub publishes beside it.
 *
 * What cannot be checked here is the last step — Android's installer, which needs a device.
 * Everything before it is the same code the app runs, including the download and the
 * checksum that decides whether an install is even offered.
 */
class UpdateLiveTest {

    @Test
    fun `the latest release is readable and newer than an older build`() = runBlocking {
        val info = runCatching { UpdateChecker.latest("0.0.1") }.getOrNull()
        assumeTrue("GitHub'a ulaşılamadı; canlı test atlandı", info != null)
        info!!

        assertTrue("sürüm boş: \"${info.version}\"", info.version.isNotBlank())
        assertTrue("APK bağlantısı yok", info.apkUrl.startsWith("https://"))
        assertTrue("APK boyutu okunmadı", info.sizeBytes > 0)
        assertNotNull("sha256 yayınlanmamış", info.sha256)
        assertTrue("0.0.1 güncel sayıldı", UpdateChecker.isNewer(info.version, "0.0.1"))
    }

    @Test
    fun `the published apk downloads and matches its own checksum`() = runBlocking {
        val info = runCatching { UpdateChecker.latest("0.0.1") }.getOrNull()
        assumeTrue("GitHub'a ulaşılamadı; canlı test atlandı", info != null)
        info!!

        val progress = ArrayList<Float>()
        val target = File.createTempFile("hoset-release", ".apk")
        try {
            UpdateChecker.download(info, target, "0.0.1") { progress += it }

            assertEquals("indirilen dosya eksik", info.sizeBytes, target.length())
            assertTrue("hiç ilerleme bildirilmedi", progress.isNotEmpty())
            assertEquals("son ilerleme %100 değil", 1f, progress.last(), 0.0001f)
        } finally {
            target.delete()
        }
    }

    /**
     * A swapped asset must be refused: the digest the release advertises is the only thing
     * standing between a tampered download and the installer.
     */
    @Test
    fun `a wrong checksum rejects the download`() = runBlocking<Unit> {
        val info = runCatching { UpdateChecker.latest("0.0.1") }.getOrNull()
        assumeTrue("GitHub'a ulaşılamadı; canlı test atlandı", info != null)
        info!!

        val tampered = info.copy(sha256 = "0".repeat(64))
        val target = File.createTempFile("hoset-tampered", ".apk")
        val failure = runCatching {
            UpdateChecker.download(tampered, target, "0.0.1") {}
        }.exceptionOrNull()

        assertNotNull("bozuk indirme kabul edildi", failure)
        assertTrue("bozuk dosya diskte kaldı", !target.exists() || target.length() == 0L)
        target.delete()
    }
}
