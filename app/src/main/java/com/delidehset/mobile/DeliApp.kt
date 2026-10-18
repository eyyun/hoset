package com.delidehset.mobile

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.delidehset.mobile.notify.HosetNotifications
import com.delidehset.mobile.notify.NotificationPoller
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import okhttp3.OkHttpClient

class DeliApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        com.delidehset.mobile.data.HosetPrefs.init(this)
        container = AppContainer(this)

        // The site offers no push, so notifications are polled — the channel and the
        // repeating alarm are set up here, on the way up, and the poll itself does nothing
        // at all while there is no session to poll with.
        HosetNotifications.channel(this)
        NotificationPoller.schedule(this)
    }

    /**
     * Every picture in the app goes through the same session as the API calls.
     *
     * Passed as a *call factory* rather than through the `okHttpClient` extension so this
     * does not depend on which OkHttp integration is on the classpath.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .callFactory(if (::container.isInitialized) container.imageClient else OkHttpClient())
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .build()
}

/** Lets every screen build its ViewModel straight from the container. */
val CreationExtras.container: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as DeliApp).container

@Suppress("UNCHECKED_CAST")
fun <VM : ViewModel> containerFactory(build: (AppContainer) -> VM) =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            build(extras.container) as T
    }
