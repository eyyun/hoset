package com.delidehset.mobile.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
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
