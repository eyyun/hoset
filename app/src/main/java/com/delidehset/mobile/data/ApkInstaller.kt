package com.delidehset.mobile.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import com.delidehset.mobile.ui.tr
import java.io.File

/**
 * Hands a downloaded APK to Android's own installer.
 *
 * A session rather than "open this file with a viewer": the session installs *over* the
 * running app (same package, same signing key), so the update keeps the session and the
 * settings, and the person only sees the system's own confirmation sheet — no file
 * browser, no "unknown sources" toggle to hunt for.
 */
class ApkInstaller(private val context: Context) {

    /**
     * True when Android will refuse this APK because it is signed with another key.
     *
     * The platform only says "package conflicts with an existing package", which reads
     * like the app is broken instead of explaining that the installed copy came from a
     * different signing key (a debug build, say). Asking first means the updater can say
     * what actually happened — and that the answer is one uninstall, not a broken update.
     */
    fun signingKeyDiffers(apk: File): Boolean {
        val installed = certDigests(installedInfo())
        val incoming = certDigests(archiveInfo(apk))
        if (installed == null || incoming == null) return false
        return installed != incoming
    }

    private fun signatureFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        }

    @Suppress("DEPRECATION")
    private fun installedInfo(): PackageInfo? = runCatching {
        context.packageManager.getPackageInfo(context.packageName, signatureFlags())
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun archiveInfo(apk: File): PackageInfo? = runCatching {
        context.packageManager.getPackageArchiveInfo(apk.absolutePath, signatureFlags())
    }.getOrNull()

    /** The SHA-256 of every signer certificate, sorted, so two sets can be compared. */
    @Suppress("DEPRECATION")
    private fun certDigests(info: PackageInfo?): List<String>? = runCatching {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info?.signingInfo?.apkContentsSigners
        } else {
            info?.signatures
        }
        signatures?.map { sha256(it.toByteArray()) }?.sorted()
    }.getOrNull()

    private fun sha256(bytes: ByteArray): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    fun install(apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(context.packageName)

        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("hoset.apk", 0, apk.length()).use { output ->
                    input.copyTo(output, 64 * 1024)
                    session.fsync(output)
                }
            }

            // Mutable, because the installer writes the result back into this intent.
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val result = Intent(context, InstallResultReceiver::class.java)
                .setAction(InstallResultReceiver.ACTION_RESULT)
            val pending = PendingIntent.getBroadcast(context, sessionId, result, flags)
            session.commit(pending.intentSender)
        }
    }
}

/**
 * The installer's answer, delivered as a broadcast.
 *
 * `STATUS_PENDING_USER_ACTION` is the ordinary case: the system is asking the person to
 * confirm, and hands back the activity that asks. Everything else is a yes or a no, and
 * the only place left to say so is a toast — the app that just installed itself is being
 * restarted at that moment.
 */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RESULT) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmation = intent.parcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirmation?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(it) }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> toast(context, tr("Güncelleme kuruldu."))
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                toast(context, if (message.isNullOrBlank()) tr("Güncelleme kurulamadı.") else message)
            }
        }
    }

    private fun toast(context: Context, text: String) {
        android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_LONG).show()
    }

    @Suppress("DEPRECATION")
    private inline fun <reified T : Any> Intent.parcelableExtra(name: String): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(name, T::class.java)
        } else {
            getParcelableExtra(name) as? T
        }

    companion object {
        const val ACTION_RESULT = "com.delidehset.mobile.INSTALL_RESULT"
    }
}
