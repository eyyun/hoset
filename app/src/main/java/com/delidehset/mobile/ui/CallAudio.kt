package com.delidehset.mobile.ui

import android.content.Context
import android.media.AudioManager
import android.os.Build

/**
 * The phone's audio path while the room's call is running.
 *
 * Android only turns on its echo canceller and noise suppression when the device is in
 * communication mode. Left in the default media mode — which is where the app used to leave
 * it — the microphone simply amplifies whatever the loudspeaker is playing, and the first
 * thing you hear after unmuting is your own voice coming back. Setting the mode for as long
 * as a call lasts is what makes the room behave like a call instead of a speakerphone.
 *
 * The route (loudspeaker or earpiece) is the user's to pick, so it is only switched on
 * request — but the mode is always restored when the call ends, or the next video, alarm or
 * ringtone would play through the call stream.
 */
object CallAudio {

    fun enter(context: Context) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        runCatching {
            audio.mode = AudioManager.MODE_IN_COMMUNICATION
            // Ear first. On the loudspeaker, an open microphone picks the room straight back
            // up, and the first thing anyone hears is their own voice; the panel offers the
            // loudspeaker for anyone who wants it.
            audio.isSpeakerphoneOn = false
        }
    }

    fun leave(context: Context) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        runCatching {
            audio.isSpeakerphoneOn = false
            audio.mode = AudioManager.MODE_NORMAL
        }
    }

    fun isSpeakerOn(context: Context): Boolean {
        val audio = context.getSystemService(AudioManager::class.java) ?: return false
        return runCatching { audio.isSpeakerphoneOn }.getOrDefault(false)
    }

    fun setSpeaker(context: Context, on: Boolean) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val wanted = if (on) {
                android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } else {
                android.media.AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }
            val device = audio.availableCommunicationDevices.firstOrNull { it.type == wanted }
            if (device != null) {
                runCatching { audio.setCommunicationDevice(device) }
                return
            }
        }
        @Suppress("DEPRECATION")
        runCatching { audio.isSpeakerphoneOn = on }
    }
}
