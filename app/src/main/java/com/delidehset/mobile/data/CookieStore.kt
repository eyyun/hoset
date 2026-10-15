package com.delidehset.mobile.data

import android.content.Context
import okhttp3.Cookie
import okhttp3.HttpUrl

/**
 * Cookie jar backed by SharedPreferences.
 *
 * delidehset.com is an ASP.NET Core app: the session lives in cookies and every
 * mutating request is guarded by an antiforgery cookie/token pair. Keeping the jar
 * on disk means the user stays signed in between app launches.
 */
class CookieStore(context: Context) : DeliCookieJar {

    private companion object {
        const val KEY = "cookies"
    }

    private val prefs = context.getSharedPreferences("deli_cookies", Context.MODE_PRIVATE)
    private val lock = Any()

    private fun key(c: Cookie): String =
        listOf(c.name, c.domain, c.path).joinToString("\u0001")

    private fun encode(c: Cookie): String = listOf(
        c.name, c.value, c.domain, c.path,
        c.expiresAt.toString(), c.secure.toString(), c.httpOnly.toString(), c.hostOnly.toString()
    ).joinToString("\u0002")

    private fun decode(raw: String): Cookie? = runCatching {
        val p = raw.split("\u0002")
        if (p.size < 8) return null
        Cookie.Builder()
            .name(p[0])
            .value(p[1])
            .apply { if (p[7] == "true") hostOnlyDomain(p[2]) else domain(p[2]) }
            .path(p[3])
            .expiresAt(p[4].toLong())
            .apply {
                if (p[5] == "true") secure()
                if (p[6] == "true") httpOnly()
            }
            .build()
    }.getOrNull()

    private fun loadAll(): List<Cookie> = synchronized(lock) {
        prefs.getStringSet(KEY, emptySet())
            .orEmpty()
            .mapNotNull { decode(it) }
            .filter { it.expiresAt > System.currentTimeMillis() }
    }

    private fun saveAll(cookies: List<Cookie>) = synchronized(lock) {
        prefs.edit().putStringSet(KEY, cookies.map { encode(it) }.toSet()).apply()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        synchronized(lock) {
            val byName = loadAll().associateBy { key(it) }.toMutableMap()
            for (c in cookies) {
                if (c.expiresAt < System.currentTimeMillis()) byName.remove(key(c)) else byName[key(c)] = c
            }
            saveAll(byName.values.toList())
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        loadAll().filter { cookie ->
            val domainOk = url.host.endsWith(cookie.domain.removePrefix("."))
            val pathOk = url.encodedPath.startsWith(cookie.path)
            domainOk && pathOk && (cookie.secure implies url.isHttps)
        }

    private infix fun Boolean.implies(other: Boolean) = !this || other

    override fun clear() = synchronized(lock) { prefs.edit().clear().apply() }

    override fun importRawHeader(url: HttpUrl, header: String?) {
        if (header.isNullOrBlank()) return
        val imported = header.split(';').mapNotNull { part ->
            val kv = part.trim().split('=', limit = 2)
            if (kv.size != 2 || kv[0].isBlank()) return@mapNotNull null
            runCatching {
                Cookie.Builder()
                    .name(kv[0].trim())
                    .value(kv[1].trim())
                    .domain(url.host)
                    .path("/")
                    .build()
            }.getOrNull()
        }
        if (imported.isNotEmpty()) saveFromResponse(url, imported)
    }

    override fun exportRawHeader(url: HttpUrl): String =
        loadForRequest(url).joinToString("; ") { "${it.name}=${it.value}" }

    /**
     * True only for a real auth cookie. `.AspNetCore.Antiforgery.*` is handed out to
     * anonymous visitors too, so it must not be mistaken for a signed-in session.
     */
    override fun hasSessionCookie(): Boolean = loadAll().any { c ->
        val n = c.name
        n.contains("AspNetCore.Cookies", ignoreCase = true) ||
            n.contains("Identity.Application", ignoreCase = true) ||
            n.contains("AspNetCore.Session", ignoreCase = true)
    }
}
