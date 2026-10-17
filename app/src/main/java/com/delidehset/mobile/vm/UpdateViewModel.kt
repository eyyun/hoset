package com.delidehset.mobile.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delidehset.mobile.BuildConfig
import com.delidehset.mobile.data.ApkInstaller
import com.delidehset.mobile.data.UpdateChecker
import com.delidehset.mobile.data.UpdateException
import com.delidehset.mobile.data.UpdateInfo
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState
    /** The APK is down and Android's installer is asking whether to go ahead. */
    data class Ready(val info: UpdateInfo) : UpdateState
    data class UpToDate(val version: String) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * The app checking on itself.
 *
 * Nobody publishes this app to a store, so it updates the way its releases are shipped: it
 * asks GitHub for the latest release once a day, offers it when it is newer, downloads the
 * signed APK, and hands it to Android's installer. Declining is remembered per version, so
 * saying "later" means later, not "every time you open the app".
 */
class UpdateViewModel(private val context: Context) : ViewModel() {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("hoset.update", Context.MODE_PRIVATE)
    private val currentVersion: String = BuildConfig.VERSION_NAME

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** Whether the prompt should be on screen; a check the person asked for always shows. */
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    /** The release being offered or downloaded, kept across a failed attempt so a retry
     *  does not have to ask GitHub again. */
    private var pending: UpdateInfo? = null

    fun maybeCheckOnLaunch() {
        if (_state.value !is UpdateState.Idle) return
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        if (System.currentTimeMillis() - last < CHECK_INTERVAL_MS) return
        check(auto = true)
    }

    fun checkNow() = check(auto = false)

    private fun check(auto: Boolean) {
        val current = _state.value
        if (current is UpdateState.Checking || current is UpdateState.Downloading) return
        viewModelScope.launch {
            _state.value = UpdateState.Checking
            val info = runCatching { UpdateChecker.latest(currentVersion) }.getOrNull()
            prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

            when {
                info == null -> {
                    _state.value = if (auto) {
                        UpdateState.Idle
                    } else {
                        UpdateState.Failed("Güncelleme denetlenemedi.")
                    }
                    if (!auto) _visible.value = true
                }
                UpdateChecker.isNewer(info.version, currentVersion) &&
                    prefs.getString(KEY_SKIPPED, null) != info.version -> {
                    pending = info
                    _state.value = UpdateState.Available(info)
                    _visible.value = true
                }
                auto -> _state.value = UpdateState.Idle
                else -> {
                    _state.value = UpdateState.UpToDate(currentVersion)
                    _visible.value = true
                }
            }
        }
    }

    fun download() {
        val info = pending ?: (when (val current = _state.value) {
            is UpdateState.Available -> current.info
            is UpdateState.Downloading -> current.info
            is UpdateState.Ready -> current.info
            else -> null
        }) ?: return

        pending = info
        _visible.value = true
        _state.value = UpdateState.Downloading(info, 0f)

        viewModelScope.launch {
            val target = File(appContext.cacheDir, "updates/hoset-${info.version}.apk")
            runCatching {
                UpdateChecker.download(info, target, currentVersion) { progress ->
                    _state.value = UpdateState.Downloading(info, progress)
                }
            }.fold(
                onSuccess = { apk ->
                    val launched = runCatching { ApkInstaller(appContext).install(apk) }
                    _state.value = if (launched.isSuccess) {
                        UpdateState.Ready(info)
                    } else {
                        UpdateState.Failed("Güncelleme kurulamadı.")
                    }
                },
                onFailure = { error ->
                    target.delete()
                    _state.value = UpdateState.Failed(
                        if (error is UpdateException && error.message == "checksum") {
                            "İndirilen dosya doğrulanamadı."
                        } else {
                            "Güncelleme indirilemedi."
                        }
                    )
                },
            )
        }
    }

    /** The drawer row: show what is already known, or go and ask. */
    fun open() {
        if (_state.value is UpdateState.Idle) checkNow() else _visible.value = true
    }

    fun retry() {
        if (pending != null) download() else checkNow()
    }

    fun dismiss() {
        // Only a real offer is remembered: a failed check should be tried again next launch.
        val offered = _state.value as? UpdateState.Available
        _visible.value = false
        offered?.let { prefs.edit().putString(KEY_SKIPPED, it.info.version).apply() }
    }

    private companion object {
        const val KEY_LAST_CHECK = "last_check"
        const val KEY_SKIPPED = "skipped"
        const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000
    }
}
