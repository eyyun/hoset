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

    fun cached(name: String?): String? = name?.let { pictures[it.lowercase()] }

    /** The picture for [name], or null when the account has none (or it could not be had). */
    suspend fun remember(repository: DeliRepository, name: String): String? {
        val key = name.lowercase()
        pictures[key]?.let { return it }
        if (noPicture.contains(key)) return null
        val found = runCatching { repository.profilePicture(name) }.getOrNull()
        if (found != null) {
            pictures[key] = found
        } else {
            noPicture.add(key)
        }
        return found
    }
}
