package com.delidehset.mobile.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.delidehset.mobile.DeliApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asks the site for `/notifications` now and then.
 *
 * There is no push to subscribe to, so this is the honest alternative: an inexact repeating
 * alarm (Android batches it, which keeps the battery cost where the system wants it) that
 * fetches the page with the app's own session and posts whatever is new. The alarm is only
 * armed while the person is signed in and has left notifications on.
 */
object NotificationPoller {

    private const val REQUEST = 7411
    private const val INTERVAL_MS = 15L * 60L * 1000L

    fun schedule(context: Context) {
        if (!HosetNotifications.enabled(context)) return
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = alarmIntent(context)
        // `setInexactRepeating` has no exact-time promise to break, so no permission is
        // needed and no doze exemption is asked for.
        manager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + INTERVAL_MS,
            INTERVAL_MS,
            pending,
        )
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        manager.cancel(alarmIntent(context))
    }

    private fun alarmIntent(context: Context): PendingIntent {
        val intent = Intent(context, NotificationPollReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * One poll. Returns the page it managed to read, or null when there is nothing to read
     * (signed out, offline, or a site that answered with a login redirect).
     */
    suspend fun poll(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? DeliApp
            ?: return@withContext Result.failure(IllegalStateException("no container"))
        val container = app.container
        if (!container.cookies.hasSessionCookie()) return@withContext Result.success(Unit)
        runCatching {
            val page = container.repository.notifications(force = true)
            HosetNotifications.post(app, page)
        }
    }
}
