package com.delidehset.mobile.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delidehset.mobile.BuildConfig
import com.delidehset.mobile.data.ApkInstaller
import com.delidehset.mobile.data.UpdateChecker
import com.delidehset.mobile.data.UpdateDecision
import com.delidehset.mobile.data.UpdateException
import com.delidehset.mobile.data.UpdateInfo
import com.delidehset.mobile.data.decideUpdate
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        if (!com.delidehset.mobile.data.HosetPrefs.autoUpdate(appContext)) return
        // A failed check leaves the state on Failed; that must not stop the next launch from
        // trying again, or one bad minute of network hides every release after it.
        when (_state.value) {
            UpdateState.Checking, is UpdateState.Available, is UpdateState.Downloading, is UpdateState.Ready -> return
            else -> Unit
        }
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
            val decision = decideUpdate(
                latestVersion = info?.version,
                currentVersion = currentVersion,
                auto = auto,
                skippedVersion = prefs.getString(KEY_SKIPPED, null),
            )

            // Only a check that actually reached the server counts as a check: writing the
            // timestamp on a failure is what kept the app from looking again for a day.
            if (info != null) prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

            when (decision) {
                UpdateDecision.AVAILABLE -> if (info != null) {
                    pending = info
                    _state.value = UpdateState.Available(info)
                    _visible.value = true
                }
                UpdateDecision.UP_TO_DATE -> {
                    _state.value = UpdateState.UpToDate(currentVersion)
                    _visible.value = true
                }
                UpdateDecision.FAILED -> {
                    _state.value = UpdateState.Failed("Güncelleme denetlenemedi.")
                    _visible.value = true
                }
                UpdateDecision.NONE -> _state.value = UpdateState.Idle
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
        // Installing it is the answer to "later"; remembering the skip past that would
        // silence the same version if the install were cancelled.
        prefs.edit().remove(KEY_SKIPPED).apply()
        _state.value = UpdateState.Downloading(info, 0f)

        viewModelScope.launch {
            val target = File(appContext.cacheDir, "updates/hoset-${info.version}.apk")
            val downloaded = runCatching {
                UpdateChecker.download(info, target, currentVersion) { progress ->
                    _state.value = UpdateState.Downloading(info, progress)
                }
            }

            val apk = downloaded.getOrNull()
            if (apk == null) {
                target.delete()
                val error = downloaded.exceptionOrNull()
                _state.value = UpdateState.Failed(
                    if (error is UpdateException && error.message == "checksum") {
                        "İndirilen dosya doğrulanamadı."
                    } else {
                        "Güncelleme indirilemedi."
                    }
                )
                return@launch
            }

            // Reading the APK's signature and copying it into an installer session are both
            // slow (Android parses the whole package twice) — and this coroutine runs on the
            // main thread, so doing them here froze the screen for the length of the
            // install, with no spinner to say anything was happening. The work moved to IO;
            // only the state, which Compose reads, is written back here.
            val launched = withContext(Dispatchers.IO) {
                runCatching {
                    val installer = ApkInstaller(appContext)
                    // Asked before the installer is handed anything: a different signing
                    // key is the one refusal Android describes as a package conflict,
                    // and it is worth saying so in words instead of showing that error.
                    if (installer.signingKeyDiffers(apk)) throw UpdateException("signature")
                    installer.install(apk)
                }
            }
            _state.value = when {
                launched.isSuccess -> UpdateState.Ready(info)
                (launched.exceptionOrNull() as? UpdateException)?.message == "signature" ->
                    UpdateState.Failed(
                        "Kurulu uygulama başka bir anahtarla imzalanmış; Android bu güncellemeyi üstüne yazamaz. Bir kez kaldırıp yeni sürümü kurunca sonrası kendiliğinden gelir."
                    )
                else -> UpdateState.Failed("Güncelleme kurulamadı.")
            }
        }
    }

    /** The drawer row: show what is already known, or go and ask. */
    fun open() {
        when (_state.value) {
            is UpdateState.Available, is UpdateState.Downloading, is UpdateState.Ready -> _visible.value = true
            // A failure is a question worth re-asking, not a dialog worth re-reading.
            else -> checkNow()
        }
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
