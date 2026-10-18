package com.delidehset.mobile.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Names to faces.
 *
 * The site's comment payload and its user tables name an account but carry no picture, so
 * every one of those rows fell back to a bare initial even on accounts that have a photo.
 * `/api/profile/{name}` is what the site's own hovercards call for the same job, and its
 * answer does not change often, so it is remembered for the life of the process: a thread
 * of twenty comments by six people costs six requests, and scrolling back costs none.
 *
 * A `null` is remembered too — an account without a picture should not be asked about
 * again on every scroll — but it is kept in a separate set, so a failure of the network is
 * not mistaken for "this person has no picture" for the rest of the session.
 */
object AvatarStore {
    private val pictures = ConcurrentHashMap<String, String>()
    private val noPicture = ConcurrentHashMap.newKeySet<String>()

    fun cached(name: String?): String? =
        name?.removePrefix("@")?.trim()?.lowercase()?.let { pictures[it] }

    fun put(name: String?, url: String?) {
        val key = name?.removePrefix("@")?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return
        val clean = url?.trim()?.takeIf { it.isNotEmpty() && !it.contains("default-profile-pic") } ?: return
        pictures[key] = clean
        noPicture.remove(key)
    }

    /** The picture for [name], or null when the account has none (or it could not be had). */
    suspend fun remember(repository: DeliRepository, name: String): String? {
        val key = name.removePrefix("@").trim().lowercase()
        if (key.isEmpty()) return null
        pictures[key]?.let { return it }
        if (noPicture.contains(key)) return null
        val found = runCatching { repository.profilePicture(key) }.getOrNull()
        if (found != null) {
            pictures[key] = found
        } else {
            noPicture.add(key)
        }
        return found
    }
}

/**
 * Community names to their uploaded logos and display titles (e.g. `/h/yavsarlar` ->
 * `/uploads/b8c0d88f30605cfa8776193d7df00aec.webp`).
 *
 * The main home feed (`/api/feed/posts`) returns the generic `/assets/images/hole.webp`
 * placeholder even for communities that have uploaded their own icon, while `/holes`
 * lists only the slug. Remembering each community's sidebar icon and display name once
 * per session lets feed cards, the community list, and community headers all show the
 * real community photo and title.
 */
object HoleStore {
    private val logos = ConcurrentHashMap<String, String>()
    private val labels = ConcurrentHashMap<String, String>()
    private val noLogo = ConcurrentHashMap.newKeySet<String>()

    private fun keyOf(hole: String?): String? =
        hole?.removePrefix("/h/")?.removePrefix("h/")?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

    fun isGenericLogo(url: String?): Boolean {
        if (url.isNullOrBlank()) return true
        val lower = url.trim().lowercase()
        return lower.endsWith("/hole.webp") ||
            lower.contains("images/logo") ||
            lower.contains("delidehset2.webp")
    }

    fun cachedLogo(hole: String?): String? = keyOf(hole)?.let { logos[it] }

    fun cachedLabel(hole: String?): String? = keyOf(hole)?.let { labels[it] }

    fun put(hole: String?, logo: String? = null, label: String? = null) {
        val key = keyOf(hole) ?: return
        val cleanLogo = logo?.trim()?.takeUnless { isGenericLogo(it) }
        if (cleanLogo != null) {
            logos[key] = cleanLogo
            noLogo.remove(key)
        }
        val cleanLabel = label?.trim()?.takeIf {
            it.isNotEmpty() && !it.equals(key, ignoreCase = true) && !it.equals("/h/$key", ignoreCase = true)
        }
        if (cleanLabel != null) {
            labels[key] = cleanLabel
        }
    }

    suspend fun remember(repository: DeliRepository, hole: String): String? {
        val key = keyOf(hole) ?: return null
        logos[key]?.let { return it }
        if (noLogo.contains(key)) return null
        val state = runCatching { repository.holeState(key) }.getOrNull() ?: return null
        put(key, logo = state.logo, label = state.displayName)
        val found = logos[key]
        if (found == null) {
            noLogo.add(key)
        }
        return found
    }
}

/**
 * In-memory cache of visited profiles, their posts, and their comments so that opening
 * your own profile or any previously visited profile renders on the first frame without
 * waiting on a network round-trip.
 */
object ProfileStore {
    private val profiles = ConcurrentHashMap<String, UserProfile>()
    private val posts = ConcurrentHashMap<String, List<FeedPost>>()
    private val comments = ConcurrentHashMap<String, List<UserComment>>()

    private fun keyOf(name: String?): String? =
        name?.removePrefix("@")?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

    fun cachedProfile(name: String?): UserProfile? = keyOf(name)?.let { profiles[it] }

    fun putProfile(name: String?, profile: UserProfile) {
        val key = keyOf(name) ?: keyOf(profile.name) ?: return
        profiles[key] = profile
    }

    fun cachedPosts(name: String?): List<FeedPost>? = keyOf(name)?.let { posts[it] }

    fun putPosts(name: String?, list: List<FeedPost>) {
        val key = keyOf(name) ?: return
        posts[key] = list
    }

    fun cachedComments(name: String?): List<UserComment>? = keyOf(name)?.let { comments[it] }

    fun putComments(name: String?, list: List<UserComment>) {
        val key = keyOf(name) ?: return
        comments[key] = list
    }

    fun clear() {
        profiles.clear()
        posts.clear()
        comments.clear()
    }
}

