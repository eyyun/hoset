package com.delidehset.mobile.data

import okhttp3.CookieJar

/**
 * The cookie jar the HTTP layer needs. Implemented on Android by [CookieStore]
 * (SharedPreferences) and in tests by an in-memory jar, which lets the whole
 * request/parse pipeline be exercised on the JVM against the real site.
 */
interface DeliCookieJar : CookieJar {
    fun clear()
    fun hasSessionCookie(): Boolean

    /**
     * Imports a raw `name=value; name2=value2` header — used to hand the session over
     * from the in-app sign-in/sign-up web sheet to the native HTTP client.
     */
    fun importRawHeader(url: okhttp3.HttpUrl, header: String?)

    /**
     * The reverse of [importRawHeader]: the cookies for [url] as one header value, so a
     * WebView (the chat frame, the sign-up challenge) starts out already signed in.
     */
    fun exportRawHeader(url: okhttp3.HttpUrl): String
}

/** A cached response body together with the moment it was stored. */
data class CachedBody(val body: String, val storedAtMs: Long) {
    fun ageMs(now: Long = System.currentTimeMillis()): Long = now - storedAtMs
    fun isFresh(ttlMs: Long, now: Long = System.currentTimeMillis()): Boolean = ageMs(now) < ttlMs
}

/**
 * Two-tier cache for page bodies.
 *
 * The site is slow enough that even opening a plain menu shows a spinner, so screens
 * read from here first and refresh in the background.
 */
interface BodyCache {
    fun read(key: String): CachedBody?
    fun write(key: String, body: String)
    fun clear()

    /** Used by tests, which do not want caching to leak between cases. */
    object None : BodyCache {
        override fun read(key: String): CachedBody? = null
        override fun write(key: String, body: String) = Unit
        override fun clear() = Unit
    }
}

/** Persists the last known viewer so the UI can render before the network answers. */
interface ViewerCache {
    suspend fun save(viewer: Viewer)
    suspend fun clear()
}
