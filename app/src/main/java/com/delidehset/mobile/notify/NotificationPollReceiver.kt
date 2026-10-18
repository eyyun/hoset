package com.delidehset.mobile.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The alarm's landing point: one poll, then out.
 *
 * `goAsync` is what lets the network call finish after the receiver returns — Android kills
 * an ordinary receiver the moment `onReceive` is done, which is exactly how a background
 * fetch ends up doing nothing at all.
 */
class NotificationPollReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                NotificationPoller.poll(app)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Boot forgets every alarm, so the schedule is put back on the way up. */
class NotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationPoller.schedule(context.applicationContext)
        }
    }
}
