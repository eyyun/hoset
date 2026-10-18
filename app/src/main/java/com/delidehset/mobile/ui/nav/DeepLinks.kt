package com.delidehset.mobile.ui.nav

import java.net.URI

/**
 * Turns a delidehset.com link into the app's own screen.
 *
 * The manifest claims the site's `http`/`https` links, so a tap in a browser, a chat or any
 * other app lands here instead of in a browser. What the link is *about* decides where it
 * goes: a post to the post screen, a profile to the profile, a community to its feed.
 * Everything the site has and the app has no native reader for is returned as a path by
 * [pathFor] and shown in the app's web frame, so a link is never a dead end.
 *
 * Parsed with [java.net.URI] rather than `android.net.Uri` on purpose: this is the part
 * worth pinning down with tests, and `Uri` is a stub in JVM unit tests. The two agree on
 * the components used here — scheme, host, path, query, fragment.
 */
object DeepLinks {

    /** The site the app is a client for; subdomains are treated as the same site. */
    const val HOST = "delidehset.com"

    /**
     * The native route a link names, or null when the app has no screen for it.
     *
     * Null is not a failure: the caller shows the page in the web frame instead.
     */
    fun routeFor(url: String): String? {
        val uri = siteUri(url) ?: return null
        val path = uri.path.orEmpty()
        return when {
            // `/post/<slug>`, and the comment the link may point at — the site writes it
            // as `#comment-<id>`, and some links carry it as `?comment=<id>` instead.
            path.startsWith("/post/") -> {
                val rest = path.removePrefix("/post/")
                val permalink = rest.substringBefore("/").trim()
                // `/post/<slug>/comment/<id>` is a comment's own page; it still opens the
                // post, scrolled to that comment.
                val fromPath = rest.substringAfter("/comment/", "").substringBefore("/").trim()
                permalink.takeIf { it.isNotBlank() }?.let {
                    Routes.post(it, commentId(uri) ?: fromPath.ifBlank { null })
                }
            }
            // Profiles appear as `/@name` in the site's own links and as `/u/name` in the
            // older ones. A trailing tab (`/@name/comments`) still names the profile.
            path.startsWith("/@") -> username(path.removePrefix("/@"))
            path.startsWith("/u/") -> username(path.removePrefix("/u/"))
            // A community's page is its feed; the app draws that itself.
            path.startsWith("/h/") -> path.removePrefix("/h/").substringBefore("/")
                .trim()
                .takeIf { it.isNotBlank() }
                ?.let(Routes::feed)
            else -> null
        }
    }

    /**
     * The site path a link names, for the pages the app has no native reader for.
     *
     * Keeps the query and fragment: a link to a search, a settings tab or a page number is
     * only useful with them.
     */
    fun pathFor(url: String): String? {
        val uri = siteUri(url) ?: return null
        val path = uri.rawPath?.takeIf { it.isNotBlank() } ?: "/"
        val query = uri.rawQuery?.takeIf { it.isNotBlank() }?.let { "?$it" }.orEmpty()
        val fragment = uri.rawFragment?.takeIf { it.isNotBlank() }?.let { "#$it" }.orEmpty()
        return path + query + fragment
    }

    /** The URI, if it belongs to the site and is one of the two schemes links use. */
    private fun siteUri(url: String): URI? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.lowercase() ?: return null
        if (host != HOST && !host.endsWith(".$HOST")) return null
        return uri
    }

    private fun username(rest: String): String? =
        rest.substringBefore("/").trim().takeIf { it.isNotBlank() }?.let(Routes::user)

    private fun commentId(uri: URI): String? {
        uri.fragment
            ?.takeIf { it.startsWith("comment-") }
            ?.removePrefix("comment-")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        // `?comment=<id>`; read by hand because URI gives the query as one undecoded string
        // and this app never needs the rest of it.
        return uri.rawQuery
            ?.split("&")
            ?.firstOrNull { it.startsWith("comment=") }
            ?.removePrefix("comment=")
            ?.takeIf { it.isNotBlank() }
    }
}
