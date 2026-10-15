package com.delidehset.mobile.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------ */
/*  /api/feed/posts — the site's own JSON feed                          */
/* ------------------------------------------------------------------ */

@Serializable
data class FeedResponse(
    val sort: String? = null,
    val hole: String? = null,
    @SerialName("next_cursor") val nextCursor: String? = null,
    val posts: List<FeedPost> = emptyList(),
)

@Serializable
data class FeedPost(
    val id: String = "",
    val permalink: String = "",
    @SerialName("title_html") val titleHtml: String = "",
    val flair: String? = null,
    @SerialName("created_utc") val createdUtc: Long = 0L,
    val ago: String? = null,
    val score: Int = 0,
    @SerialName("viewer_vote") val viewerVote: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    val nsfw: Boolean = false,
    @SerialName("has_thumbnail") val hasThumbnail: Boolean = false,
    @SerialName("thumb_url") val thumbUrl: String? = null,
    val images: List<String> = emptyList(),
    @SerialName("media_href") val mediaHref: String? = null,
    @SerialName("external_media") val externalMedia: Boolean = false,
    @SerialName("excerpt_desktop") val excerptDesktop: String? = null,
    @SerialName("excerpt_mobile") val excerptMobile: String? = null,
    val hole: String? = null,
    @SerialName("hole_label") val holeLabel: String? = null,
    @SerialName("hole_logo") val holeLogo: String? = null,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("author_tripcode") val authorTripcode: String? = null,
    @SerialName("author_href") val authorHref: String? = null,
    @SerialName("author_avatar") val authorAvatar: String? = null,
)

/* ------------------------------------------------------------------ */
/*  Parsed page models                                                 */
/* ------------------------------------------------------------------ */

/** Which list a feed belongs to. Mirrors the buttons on the website. */
enum class FeedSort(val key: String, val label: String) {
    HOT("hot", "Hot"),
    NEW("new", "New"),
    TOP("top", "Top"),
    CONTROVERSIAL("controversial", "Controversial");

    companion object {
        fun fromKey(key: String?): FeedSort = entries.firstOrNull { it.key == key } ?: HOT
    }
}

data class PostDetail(
    val id: String,
    val permalink: String,
    val title: String,
    val bodyHtml: String,
    val hole: String?,
    val holeHref: String?,
    val authorName: String?,
    val authorHref: String?,
    val authorAvatar: String?,
    val ago: String?,
    val editedAgo: String?,
    val commentCount: Int,
    val viewerVote: Int,
    /**
     * The website only renders the score when you are signed in; for guests the vote
     * row is replaced by a sign-up link. So this stays null when the page does not
     * carry it, and the UI shows a dash instead of inventing a number.
     */
    val score: Int?,
    val nsfw: Boolean,
    val flair: String?,
    val isDeleted: Boolean,
)

data class CommentNode(
    val id: String,
    val parentId: String?,
    val depth: Int,
    val authorName: String?,
    val authorHref: String?,
    val authorAvatar: String?,
    val bodyHtml: String,
    val score: Int,
    val viewerVote: Int,
    val ago: String?,
    val isDeleted: Boolean,
    val children: List<CommentNode> = emptyList(),
)

data class Hole(
    val name: String,
    val href: String,
    val postCount: Int,
    val createdOn: String?,
    val stealth: String?,
    val publicUse: String?,
    val description: String? = null,
)

data class SiteUser(
    val id: String,
    val name: String,
    val href: String,
    val joined: String?,
    val posts: String?,
    val comments: String?,
)

data class LeaderRow(
    val rank: Int,
    val name: String,
    val href: String,
    val score: String?,
    val extra: String? = null,
)

data class BadgeRow(
    val name: String,
    val href: String?,
    val description: String?,
    val price: String?,
)

data class SimpleRow(
    val cells: List<String>,
    val href: String? = null,
)

data class KeyValue(val key: String, val value: String)

data class StatsPage(
    val sections: List<Pair<String, List<KeyValue>>>,
)

data class UserProfile(
    val id: String,
    val name: String,
    val href: String,
    val bio: String?,
    val avatarUrl: String?,
    val joinedAgo: String?,
    val stats: List<KeyValue>,
    val isFollowing: Boolean,
    val postDays: List<DailyCount> = emptyList(),
)

data class DailyCount(val label: String, val count: Int)

/** Result of a login attempt. */
sealed interface LoginResult {
    data object Success : LoginResult
    data class Failure(val message: String) : LoginResult
}

/** The signed-in viewer, as far as we can tell from the chrome the site renders. */
data class Viewer(
    val isSignedIn: Boolean,
    val username: String? = null,
    val href: String? = null,
    val avatarUrl: String? = null,
    val canVote: Boolean = false,
    /** Mirrors `<body data-admin>`; gates the admin-only destinations. */
    val isAdmin: Boolean = false,
)

/* ------------------------------------------------------------------ */
/*  Account settings                                                   */
/* ------------------------------------------------------------------ */

/**
 * Current values of the `/settings` (Personal) page.
 *
 * The names and defaults mirror the real markup: the language and theme `<select>`s
 * carry no `name` attribute (the site's own JavaScript reads them by id), so they are
 * parsed by id rather than through the generic form reader.
 */
data class PersonalSettings(
    val bio: String = "",
    val pronouns: String = "",
    val language: String = "en",
    val languageOptions: List<Pair<String, String>> = emptyList(),
    val theme: String = "midnight",
    val themeOptions: List<Pair<String, String>> = emptyList(),
    val themeColor: String = "",
)

/**
 * Current values of the `/settings/security` page.
 *
 * The page always ships the (hidden) 2FA setup + verify markup, so it does not reveal
 * whether 2FA is already on — only that the feature exists for this account. The app
 * therefore offers 2FA as "not handled here" instead of guessing a state.
 */
data class SecuritySettings(
    val email: String = "",
    val twoFactorAvailable: Boolean = false,
)

/** Both settings tabs in one snapshot, so the screen needs a single round trip. */
data class AccountSettings(
    val personal: PersonalSettings = PersonalSettings(),
    val security: SecuritySettings = SecuritySettings(),
)
