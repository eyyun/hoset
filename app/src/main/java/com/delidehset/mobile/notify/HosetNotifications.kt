package com.delidehset.mobile.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.delidehset.mobile.MainActivity
import com.delidehset.mobile.R
import com.delidehset.mobile.data.DeliNotification
import com.delidehset.mobile.data.NotificationPage

/**
 * The site's notifications, as Android notifications.
 *
 * delidehset.com has no push endpoint and no service worker, so this is a poll rather than
 * a push: [NotificationPoller] asks `/notifications` on an alarm and hands whatever it found
 * to [post]. One notification per new row, all in one channel, each tapping through to the
 * app's notifications screen.
 */
object HosetNotifications {

    const val CHANNEL_ID = "hoset.notifications"
    private const val GROUP = "hoset.notifications.group"
    private const val PREF = "hoset.notify"
    private const val KEY_SEEN = "seen"

    /** Whether the person wants the app to tell them about new rows at all. */
    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean("enabled", true)

    fun setEnabled(context: Context, on: Boolean) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean("enabled", on).apply()
        if (on) NotificationPoller.schedule(context) else {
            NotificationPoller.cancel(context)
            NotificationManagerCompat.from(context).cancelAll()
        }
    }

    /** The row ids the app has already shown, so a poll only speaks about what is new. */
    fun seen(context: Context): Set<String> =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getStringSet(KEY_SEEN, emptySet())
            .orEmpty()

    fun rememberSeen(context: Context, ids: Collection<String>) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        // Bounded on purpose: the ids of a thousand old rows are not worth remembering,
        // and their comments are years old by then anyway.
        val kept = (seen(context) + ids).toList().takeLast(500).toSet()
        prefs.edit().putStringSet(KEY_SEEN, kept).apply()
    }

    fun channel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                com.delidehset.mobile.ui.tr("Bildirimler"),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = com.delidehset.mobile.ui.tr("delidehset.com'daki yeni yorum, bahsetme ve yanıtlar")
            }
        )
    }

    /**
     * Shows the rows that are new since the last poll and are still unread.
     *
     * The number of *unread* rows (the bell's own count) becomes a one-line summary, and up
     * to four of them are shown individually so a tap lands on something specific. Nothing
     * new means nothing is posted — a poll that finds no news must not buzz.
     */
    fun post(context: Context, page: NotificationPage) {
        if (!enabled(context)) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        channel(context)

        val known = seen(context)
        val fresh = page.items.filter { it.unread && it.id !in known }
        if (fresh.isEmpty()) {
            rememberSeen(context, page.items.map { it.id })
            return
        }

        val manager = NotificationManagerCompat.from(context)
        val open = openIntent(context)

        for (row in fresh.take(4)) {
            val id = row.id.hashCode()
            val body = row.text.ifBlank { row.postTitle.orEmpty() }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(row.postTitle ?: context.getString(R.string.app_name))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setGroup(GROUP)
                .setAutoCancel(true)
                .setContentIntent(open)
                .setWhen(System.currentTimeMillis())
                .build()
            runCatching { manager.notify(id, notification) }
        }

        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(com.delidehset.mobile.ui.trf("{0} okunmamış bildirim", page.unreadCount))
            .setContentIntent(open)
            .setGroup(GROUP)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(SUMMARY_ID, summary) }

        rememberSeen(context, page.items.map { it.id })
    }

    private const val SUMMARY_ID = 4801

    private fun openIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_NOTIFICATIONS)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Forgets what was shown — a sign-out should not silence the next account. */
    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY_SEEN).apply()
        NotificationManagerCompat.from(context).cancelAll()
    }
}
