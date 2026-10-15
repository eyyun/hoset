package com.delidehset.mobile

import android.content.Context
import coil.Coil
import com.delidehset.mobile.data.CookieStore
import com.delidehset.mobile.data.DeliCache
import com.delidehset.mobile.data.DeliHttp
import com.delidehset.mobile.data.DeliRepository
import com.delidehset.mobile.data.LangStore
import com.delidehset.mobile.data.SessionStore
import okhttp3.OkHttpClient

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val cookies = CookieStore(context)
    val http = DeliHttp(cookies)
    val session = SessionStore(context)

    /** The app's own language, which the site's setting never touched. */
    val lang = LangStore(context)

    /** Memory + disk cache so a slow page is only slow the first time. */
    val cache = DeliCache(context)

    val repository = DeliRepository(http, cookies, session, cache)

    /**
     * The session, for pictures.
     *
     * This site serves `/avatar/…` only to signed-in requests — without the cookies every
     * avatar came back as a redirect to `/login`, which is why the app showed initials
     * everywhere while the site showed faces. Redirects are followed here (the page client
     * inspects them itself, so it deliberately does not), and a disk cache keeps the
     * pictures between launches.
     */
    val imageClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookies)
        .followRedirects(true)
        .followSslRedirects(true)
        .cache(okhttp3.Cache(java.io.File(appContext.cacheDir, "images"), 20L * 1024 * 1024))
        .build()

    /**
     * Throws away the pictures Coil has already fetched.
     *
     * A new profile picture is served from the same URL as the old one, so without this the
     * one thing an upload changed looks like the one thing it did not.
     */
    fun forgetImages() {
        runCatching {
            Coil.imageLoader(appContext).memoryCache?.clear()
            Coil.imageLoader(appContext).diskCache?.clear()
            imageClient.cache?.evictAll()
        }
    }
}
