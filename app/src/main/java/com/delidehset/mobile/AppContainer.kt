package com.delidehset.mobile

import android.content.Context
import coil.Coil
import com.delidehset.mobile.data.ChatSession
import com.delidehset.mobile.data.CookieStore
import com.delidehset.mobile.data.DeliCache
import com.delidehset.mobile.data.DeliHttp
import com.delidehset.mobile.data.DeliRepository
import com.delidehset.mobile.data.LangStore
import com.delidehset.mobile.data.SessionStore
import com.delidehset.mobile.data.ThemeStore
import okhttp3.OkHttpClient

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val cookies = CookieStore(context)
    val http = DeliHttp(cookies)
    val session = SessionStore(context)

    /** The app's own language, which the site's setting never touched. */
    val lang = LangStore(context)

    /** The app's own look, likewise kept apart from the site's theme setting. */
    val theme = ThemeStore(context)

    /** Memory + disk cache so a slow page is only slow the first time. */
    val cache = DeliCache(context)

    val repository = DeliRepository(http, cookies, session, cache)

    /**
     * The chat room's transport, owned by the app rather than by the chat screen.
     *
     * It has to outlive the screen: rebuilding the room's WebView, its token and its socket
     * every time somebody glanced at their profile is what made Sohbet open empty and slow.
     * Closing it is the job of [MainActivity.onStop] and of signing out.
     */
    val chat = ChatSession(appContext, http, cookies)

    /**
     * The session, for pictures.
     *
     * This site serves an avatar only to signed-in requests — without the cookies they came
     * back as a redirect to `/login`, which is why the app showed initials everywhere while
     * the site showed faces. Redirects are followed here (the page client inspects them
     * itself, so it deliberately does not).
     *
     * The caching is Coil's, not this client's: an HTTP cache stores whole *responses* by
     * URL, so a single signed-out fetch of an avatar was replayed as a login page for that
     * URL long after the session worked, and the app kept drawing initials with no way
     * back. Coil's own disk cache only keeps pictures it managed to decode.
     */
    val imageClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookies)
        .followRedirects(true)
        .followSslRedirects(true)
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
