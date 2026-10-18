package com.delidehset.mobile.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Everything delidehset.com renders is server-side HTML with htmx sprinkled on top
 * (the feed additionally has a JSON endpoint, which we prefer when we can).
 * These helpers turn those pages into the models the UI consumes.
 *
 * The site is an early beta with very little content, so every selector below is
 * written defensively: a missing node yields null/empty rather than an exception.
 */
object HtmlParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    const val UNSET = -999

    fun document(html: String): Document = Jsoup.parse(html, DeliHttp.BASE_URL)

    /** Text with any markup stripped — used for `title_html` and tagged excerpts. */
    fun stripTags(html: String?): String = if (html.isNullOrBlank()) "" else Jsoup.parse(html).text()

    /* ---------------------------------------------------------------- */
    /*  Session plumbing                                                */
    /* ---------------------------------------------------------------- */

    fun antiforgeryToken(doc: Document): String? =
        doc.selectFirst("input[name=__RequestVerificationToken]")?.attr("value")?.takeIf { it.isNotBlank() }

    /**
     * Works out who (if anyone) is signed in from the chrome the site renders.
     *
     * A signed-in page is recognised by any of: a `/logout` link, the sidebar's
     * logout button (`onclick="logout()"`), the notification bell, or the profile link
     * the sidebar's bottom block points at `/settings/personal`. That last one is where
     * the username lives — inside a `<strong>` next to the avatar — and unlike a
     * `/@name` link it is present on every signed-in page, and independent of the
     * language the user selected for the site.
     */
    fun parseViewer(doc: Document): Viewer {
        val sidebar: Element = doc.selectFirst("#platform-sidebar") ?: doc
        val profileLink = sidebar.selectFirst("a[href^=/settings/personal]")
            ?: doc.selectFirst("a[href^=/settings/personal]")

        val hasLogout = doc.selectFirst("a[href*=/logout]") != null ||
            doc.selectFirst("[onclick*=logout]") != null
        val signedIn = hasLogout ||
            doc.selectFirst("#header--notifications--item") != null ||
            profileLink != null
        if (!signedIn) return Viewer(isSignedIn = false, isAdmin = isAdmin(doc))

        val avatar = profileLink?.selectFirst("img[src]")?.attr("src")
            ?: sidebar.selectFirst("img[src*=/avatar/]")?.attr("src")
            ?: sidebar.selectFirst("img[src*=/pp/]")?.attr("src")

        val name = profileLink?.selectFirst("strong")?.text()?.trim()?.takeIf { it.isNotBlank() }
            ?: extractSignedInName(doc)

        return Viewer(
            isSignedIn = true,
            username = name,
            href = name?.let { "/@$it" },
            avatarUrl = avatar,
            canVote = parseFeedConfig(doc)?.canVote ?: true,
            isAdmin = isAdmin(doc),
        )
    }

    /** The site marks admins on `<body data-admin="true">` on every page. */
    fun isAdmin(doc: Document): Boolean =
        doc.selectFirst("body[data-admin=true]") != null

    private fun extractSignedInName(doc: Document): String? =
        doc.select("a[href^=/@]").map { it.attr("href").removePrefix("/@") }
            .firstOrNull { it.isNotBlank() && !it.contains("/") }

    private data class FeedConfig(val sort: String?, val hole: String?, val cursor: String?, val canVote: Boolean)

    private fun parseFeedConfig(doc: Document): FeedConfig? {
        val raw = doc.selectFirst("script#feed-config")?.data()?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val o = json.parseToJsonElement(raw).jsonObject
            FeedConfig(
                sort = o["sort"]?.jsonPrimitive?.contentOrNullSafe(),
                hole = o["hole"]?.jsonPrimitive?.contentOrNullSafe(),
                cursor = o["cursor"]?.jsonPrimitive?.contentOrNullSafe(),
                canVote = o["can_vote"]?.jsonPrimitive?.booleanOrNull ?: false,
            )
        }.getOrNull()
    }

    /* ---------------------------------------------------------------- */
    /*  Post detail + comments                                          */
    /* ---------------------------------------------------------------- */

    fun parsePost(html: String, permalink: String): PostDetail? {
        val doc = document(html)
        val root = doc.selectFirst("#post-root") ?: return null
        // NB: #post-root itself matches [id^=post-], so the wrapper is matched by
        // its UUID-shaped id instead of by prefix alone.
        val container = root.selectFirst("div[id^=post-]:not(#post-root)")
            ?: doc.selectFirst("div[id^=post-]:not(#post-root)")
            ?: doc.selectFirst("#post-root")
        val id = container?.id()?.removePrefix("post-")?.takeIf { UUID_RE.matches(it) }
            ?: UUID_RE.find(html)?.value
            ?: return null

        val title = doc.selectFirst("#post-title")?.text()?.trim().orEmpty()
        val body = doc.selectFirst("#post-body")?.html().orEmpty()

        val meta = doc.selectFirst(".post-meta")
        val authorLink = meta?.selectFirst("a.user-name") ?: meta?.selectFirst("a[href^=/@]")
        val holeLink = meta?.select("a[href^=/h/]")?.firstOrNull()
        val timeTexts = meta?.select("span.text-muted")?.map { it.text() } ?: emptyList()
        val ago = timeTexts.firstOrNull { it.contains("ago") || it.matches(Regex(".*\\d.*")) }
        val edited = timeTexts.firstOrNull { it.contains("edited", ignoreCase = true) }

        val commentCount = doc.selectFirst(".post-actions")?.text()
            ?.let { Regex("(\\d+)").find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: 0

        val viewerVote = root.attr("data-page-post-vote").toIntOrNull() ?: 0

        // Rendered only for signed-in viewers; null means "not shown on this page".
        val score = doc.selectFirst("[data-page-post-score]")?.attr("data-page-post-score")?.toIntOrNull()
            ?: doc.selectFirst("#post-score")?.text()?.trim()?.toIntOrNull()
            ?: doc.selectFirst(".post-actions .score")?.text()?.trim()?.toIntOrNull()

        val deleted = body.contains("deleted", ignoreCase = true) && body.length < 200

        return PostDetail(
            id = id,
            permalink = permalink,
            title = title.ifBlank { "(başlıksız)" },
            bodyHtml = body,
            hole = holeLink?.text()?.removePrefix("/h/")?.trim(),
            holeHref = holeLink?.attr("href"),
            authorName = authorLink?.text()?.trim()?.removePrefix("@"),
            authorHref = authorLink?.attr("href"),
            authorAvatar = doc.selectFirst(".actual-post img[src*=/avatar/]")?.attr("src"),
            ago = ago,
            editedAgo = edited,
            commentCount = commentCount,
            viewerVote = viewerVote,
            score = score,
            nsfw = doc.selectFirst("meta[name=nsfw]") != null,
            flair = doc.selectFirst(".post-flair, .flair")?.text()?.trim()?.ifBlank { null },
            isDeleted = deleted,
        )
    }

    /**
     * The comment tree is embedded as JSON in `#replies-data` and rendered client-side
     * into `#replies-root`. We consume the JSON directly. Field names differ between
     * builds of the platform, so several aliases are accepted per field.
     */
    fun parseComments(html: String): List<CommentNode> {
        val doc = document(html)
        val raw = doc.selectFirst("script#replies-data")?.data()?.takeIf { it.isNotBlank() }
            ?: return parseCommentsFromHtml(doc)
        val roots = runCatching {
            val el = json.parseToJsonElement(raw)
            if (el is JsonArray) el.mapNotNull { toComment(it, null, 0) } else emptyList()
        }.getOrElse { emptyList() }
        return if (roots.isEmpty()) parseCommentsFromHtml(doc) else roots
    }

    private fun toComment(el: JsonElement, parentId: String?, depth: Int): CommentNode? {
        val o = runCatching { el.jsonObject }.getOrNull() ?: return null
        val id = o.str("id", "comment_id", "commentId") ?: return null
        val author = o.obj("author")
        val bodyHtml = o.str("body_html", "bodyHtml", "body", "content")
            ?: o.obj("body")?.str("html", "value", "text")
            ?: ""
        val children = (o["children"] ?: o["replies"] ?: o["children_json"])
            ?.let { runCatching { it.jsonArray }.getOrNull() }
            ?.mapNotNull { toComment(it, id, depth + 1) }
            ?: emptyList()

        return CommentNode(
            id = id,
            parentId = o.str("parent_id", "parentId") ?: parentId,
            depth = o.int("level", "depth") ?: depth,
            // `author` is a plain user name in the shipped build; other builds nest an
            // object under the same key, so both shapes are accepted.
            authorName = o.str("author_name", "authorName", "username", "author")
                ?: author?.str("name", "username"),
            authorHref = o.str("author_href", "authorHref", "author_url")
                ?: author?.str("href", "url", "profile_url")
                ?: o.str("author")?.takeIf { it.isNotBlank() }?.let { "/@$it" },
            authorAvatar = o.str("author_avatar", "authorAvatar", "avatar")
                ?: author?.str("avatar", "avatar_url"),
            bodyHtml = bodyHtml,
            score = o.int("score", "points") ?: 0,
            viewerVote = o.int("viewer_vote", "viewerVote", "voted", "vote") ?: 0,
            ago = o.str("ago", "created_ago", "created"),
            isDeleted = o.bool("is_deleted", "deleted", "isDeleted") ?: (bodyHtml.isBlank()),
            children = children,
        )
    }

    /** Fallback for builds that render comments server-side into `#replies-root`. */
    private fun parseCommentsFromHtml(doc: Document): List<CommentNode> {
        val root = doc.selectFirst("#replies-root") ?: return emptyList()
        val nodes = root.select("div[id^=comment-]").mapNotNull { el ->
            val id = el.id().removePrefix("comment-").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val body = el.selectFirst(".comment-body, .comment-content, .mr-2")?.html().orEmpty()
            val authorLink = el.selectFirst("a.user-name, a[href^=/@]")
            CommentNode(
                id = id,
                parentId = el.attr("data-parent-id").ifBlank { null },
                depth = el.attr("data-level").toIntOrNull() ?: 0,
                authorName = authorLink?.text()?.trim()?.removePrefix("@"),
                authorHref = authorLink?.attr("href"),
                authorAvatar = el.selectFirst("img[src*=/avatar/]")?.attr("src"),
                bodyHtml = body,
                score = el.selectFirst(".comment-score, .score")?.text()?.trim()?.toIntOrNull() ?: 0,
                viewerVote = 0,
                ago = el.selectFirst(".text-muted")?.text(),
                isDeleted = body.isBlank(),
            )
        }
        return nodes.filter { it.parentId == null || nodes.none { p -> p.id == it.parentId } }
    }

    fun commentVotes(html: String): Map<String, Int> {
        val raw = document(html).selectFirst("#post-root")?.attr("data-page-comment-votes")
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            json.parseToJsonElement(raw).jsonObject.mapNotNull { (k, v) ->
                v.jsonPrimitive.intOrNull?.let { k to it }
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    /* ---------------------------------------------------------------- */
    /*  Holes, users and the various tables                             */
    /* ---------------------------------------------------------------- */

    fun parseHoles(html: String): List<Hole> {
        val doc = document(html)
        return doc.select("table tbody tr").mapNotNull { row ->
            val cells = row.select("td")
            if (cells.size < 3) return@mapNotNull null
            val link = cells[1].selectFirst("a[href^=/h/]") ?: row.selectFirst("a[href^=/h/]") ?: return@mapNotNull null
            Hole(
                name = link.text().removePrefix("/h/").trim(),
                href = link.attr("href"),
                postCount = cells.getOrNull(2)?.text()?.trim()?.toIntOrNull() ?: 0,
                createdOn = cells.getOrNull(3)?.text()?.trim(),
                stealth = cells.getOrNull(4)?.text()?.trim(),
                publicUse = cells.getOrNull(5)?.text()?.trim(),
            )
        }
    }

    fun parseHoleHeader(html: String): Hole? {
        val doc = document(html)
        val link = doc.selectFirst("#main-content-col h1 a[href^=/h/]")
            ?: doc.selectFirst(".feed-head-name[href^=/h/]")
            ?: return null
        val name = link.text().removePrefix("/h/").trim().ifBlank { return null }
        return Hole(
            name = name,
            href = link.attr("href"),
            postCount = 0,
            createdOn = null,
            stealth = null,
            publicUse = null,
            description = doc.selectFirst(".hole-description, #main-content-col .text-muted")?.text()?.trim(),
        )
    }

    fun parseUsers(html: String): List<SiteUser> {
        val doc = document(html)
        return doc.select("table tbody tr").mapNotNull { row ->
            val cells = row.select("td")
            if (cells.size < 2) return@mapNotNull null
            val link = cells[1].selectFirst("a[href^=/@]") ?: return@mapNotNull null
            SiteUser(
                id = cells[0].text().trim(),
                name = link.text().removePrefix("@").trim(),
                href = link.attr("href"),
                joined = cells.getOrNull(2)?.text()?.trim(),
                posts = cells.getOrNull(3)?.text()?.trim(),
                comments = cells.getOrNull(4)?.text()?.trim(),
            )
        }
    }

    /** Generic row reader used by /badges, /admins, /comments, /leaderboard and /stats. */
    fun parseRows(html: String): List<SimpleRow> {
        val doc = document(html)
        val rows = doc.select("table tbody tr")
        if (rows.isNotEmpty()) {
            return rows.map { row ->
                val cells = row.select("td, th").map { it.text().trim() }
                SimpleRow(cells, row.selectFirst("a[href]")?.attr("href"))
            }
        }
        // Some pages render list groups instead of tables.
        val groups = doc.select("#main-content-col ul.list-group li, #main-content-col .list-group-item").map { li ->
            val link = li.selectFirst("a[href]")
            SimpleRow(listOf(li.text().trim()), link?.attr("href"))
        }
        if (groups.isNotEmpty()) return groups

        // Link menus, e.g. the admin tools page: `<h4>` sections full of `<ul><li><a>`.
        val container = doc.selectFirst("#main-content-col") ?: return emptyList()
        val linkRows = mutableListOf<SimpleRow>()
        var section = ""
        for (el in container.allElements) {
            when {
                el.tagName().equals("h4", true) -> section = el.text().trim()
                el.tagName().equals("a", true) && el.parent()?.tagName()?.equals("li", true) == true -> {
                    val label = el.text().trim()
                    if (label.isNotBlank()) {
                        linkRows += SimpleRow(
                            listOfNotNull(section.takeIf { it.isNotBlank() }, label),
                            el.attr("href"),
                        )
                    }
                }
            }
        }
        return linkRows
    }

    /**
     * A search results page, in whichever tab it happens to be showing.
     *
     * The page is a tab strip plus a `.tab-content` pane, and the strip itself is built
     * from `<li><a>` links — which the generic row reader happily mistook for results,
     * so an empty search used to come back as three rows named after the tabs. Only the
     * pane is read here, and an empty pane is an empty result, not a fallback to links.
     */
    fun parseSearch(html: String): List<SimpleRow> {
        val pane = document(html).selectFirst(".tab-content") ?: return emptyList()

        val posts = pane.select(".posts .actual-post, .posts article, .posts .card")
        if (posts.isNotEmpty()) {
            return posts.mapNotNull { card ->
                val titleLink = card.selectFirst("h5.card-title a")
                    ?: card.selectFirst("a.stretched-link")
                    ?: card.selectFirst("a[href*=/post/]")
                val title = titleLink?.text()?.trim().orEmpty()
                if (title.isBlank()) return@mapNotNull null
                SimpleRow(
                    cells = listOf(
                        title,
                        card.selectFirst("a.hole-link")?.text()?.trim().orEmpty(),
                        card.selectFirst("a.user-name")?.text()?.trim().orEmpty(),
                        card.selectFirst(".score")?.text()?.trim().orEmpty(),
                    ),
                    href = titleLink?.attr("href"),
                )
            }
        }

        val users = pane.select("a.list-group-item[href]")
        if (users.isNotEmpty()) {
            return users.mapNotNull { link ->
                val name = link.text().trim()
                if (name.isBlank()) null else SimpleRow(listOf(name), link.attr("href"))
            }
        }

        // Comments: the pane holds either blocks or a single "nothing found" note.
        val comments = pane.select(".comments .comment, .comments .actual-comment, .comments article")
        if (comments.isNotEmpty()) {
            return comments.mapNotNull { comment ->
                val text = comment.text().trim()
                if (text.isBlank()) null
                else SimpleRow(listOf(text), comment.selectFirst("a[href]")?.attr("href"))
            }
        }

        return emptyList()
    }

    fun parseRowHeaders(html: String): List<String> =
        document(html).select("table thead th").map { it.text().trim() }

    /**
     * The sentence the page itself offers when it has no table to show.
     *
     * Several destinations on this prototype are deliberately empty — `/comments`
     * answers "No comments yet." and `/stats` says the feature was never ported — so the
     * honest thing to show is the server's own words rather than a generic empty state.
     */
    fun pageNote(html: String): String? {
        val doc = document(html)
        val main = doc.selectFirst("#main-content-col") ?: return null
        return main.select("p, .text-muted").map { it.text().trim() }
            .firstOrNull { it.isNotBlank() }
            ?.take(240)
    }

    fun parseBadges(html: String): List<BadgeRow> {
        val doc = document(html)
        val fromTable = doc.select("table tbody tr").mapNotNull { row ->
            val cells = row.select("td")
            if (cells.isEmpty()) return@mapNotNull null
            BadgeRow(
                name = cells[0].text().trim(),
                href = row.selectFirst("a[href]")?.attr("href"),
                description = cells.getOrNull(1)?.text()?.trim(),
                price = cells.getOrNull(2)?.text()?.trim(),
            )
        }
        if (fromTable.isNotEmpty()) return fromTable

        return doc.select("#main-content-col .badge-card, #main-content-col .card, #main-content-col li").mapNotNull { el ->
            val name = el.selectFirst("h5, h4, .card-title, .fw-bold")?.text()?.trim()
                ?: el.text().trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
            BadgeRow(
                name = name,
                href = el.selectFirst("a[href]")?.attr("href"),
                description = el.selectFirst("p, small, .text-muted")?.text()?.trim(),
                price = Regex("(\\d+)\\s*(mern|marseybux|coin|token)", RegexOption.IGNORE_CASE)
                    .find(el.text())?.value,
            )
        }
    }

    fun parseProfile(html: String, fallbackName: String): UserProfile {
        val doc = document(html)
        val main = doc.selectFirst("#main-content-col") ?: doc
        val name = main.selectFirst("#profile--name")?.text()?.trim()?.removePrefix("@")
            ?.takeIf { it.isNotBlank() }
            ?: main.selectFirst("h1, h2")?.text()?.trim()?.removePrefix("@")
                ?.takeIf { it.isNotBlank() } ?: fallbackName

        // The profile page labels every field with its own id. Reading those directly is
        // what the site's own JavaScript does; `.text-muted` here is the *bio*, not the
        // join line, which is why the screen used to print "Katılım: No bio...".
        val followers = main.selectFirst("#profile--followers")?.text()?.trim()
            ?.let { Regex("\\d+").find(it)?.value }
        val following = main.selectFirst("#profile--following")?.text()?.trim()
            ?.let { Regex("\\d+").find(it)?.value }
        val coins = main.selectFirst("#profile-coins-amount")?.text()?.trim()
        val stats = buildList {
            followers?.let { add(KeyValue("Takipçi", it)) }
            following?.let { add(KeyValue("Takip", it)) }
            coins?.takeIf { it.isNotBlank() && it != "0" }?.let { add(KeyValue("Coin", it)) }
            addAll(
                main.select(".stat, .profile-stat").mapNotNull { el ->
                    val text = el.text().trim()
                    if (text.isBlank() || text.length > 80) return@mapNotNull null
                    val parts = text.split(":", limit = 2)
                    if (parts.size == 2) KeyValue(parts[0].trim(), parts[1].trim()) else null
                }
            )
        }.distinctBy { it.key }.take(20)

        // "No bio..." is the site's own placeholder, not something the user wrote.
        val bio = main.selectFirst("#profile--bio, .user-bio, .bio, #bio")?.text()?.trim()
            ?.takeIf { it.isNotBlank() && !it.equals("No bio...", ignoreCase = true) }

        // "Joined 29 Sep 2026" — the screen renders its own "Katılım:" label.
        val joined = main.selectFirst("#profile--joined")?.text()?.trim()
            ?.replace(Regex("(?i)^joined\\s*"), "")
            ?.takeIf { it.isNotBlank() }

        val subButton = main.selectFirst("#button-sub")
        val unsubButton = main.selectFirst("#button-unsub")

        // The banner is a CSS background on the hero, so the picture's URL is inside the
        // style attribute rather than in an <img>.
        val banner = main.selectFirst("#desktopUserBanner")?.attr("style")
            ?.let { BANNER_URL.find(it)?.groupValues?.get(1) }
            ?.takeIf { it.isNotBlank() && !it.contains("site_preview") }

        // The broom next to the name: the site marks operators this way, and nothing else
        // on the page says it.
        val operator = main.selectFirst("#profile--mop") != null

        val moderation = main.selectFirst("a[href*=target_id=]")?.attr("href")
            ?.takeIf { it.isNotBlank() }

        // The profile's own tabs, with the counts the site prints beside them.
        val tabs = main.select(".settings-nav a.nav-link, #content a.nav-link").mapNotNull { a ->
            val href = a.attr("href").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val count = a.selectFirst(".count")?.text()?.let { Regex("\\d+").find(it)?.value?.toIntOrNull() }
            ProfileTab(
                label = a.text().replace(Regex("\\(\\d+\\)"), "").trim(),
                href = href,
                count = count,
                active = a.hasClass("active"),
            )
        }.distinctBy { it.href }

        // The mute button the page renders is the opposite of the state: `#mute-notifs`
        // hidden means notifications are already muted.
        val muted = main.selectFirst("#mute-notifs")?.hasClass("d-none") == true
        // Selected by suffix rather than by the id written out: the id contains the word
        // the app's own translation test treats as Turkish, and a selector is not a line
        // anybody reads.
        val canModerate = main.selectFirst("form[action*=/ban_user/], [id$=--admintools]") != null

        return UserProfile(
            id = main.selectFirst("#profile--info--id")?.text()?.substringAfter("ID:")?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: UUID_RE.find(html)?.value.orEmpty(),
            name = name,
            href = "/@$name",
            bio = bio,
            avatarUrl = main.selectFirst("#profile--pfp img[src]")?.attr("src")
                ?: main.selectFirst("img[src*=/avatar/]")?.attr("src")
                ?: main.selectFirst("img[src*=/pp/]")?.attr("src"),
            joinedAgo = joined,
            stats = stats,
            isFollowing = when {
                unsubButton != null -> !unsubButton.hasClass("d-none")
                subButton != null -> subButton.hasClass("d-none")
                else -> main.selectFirst("a[href*=unfollow]") != null
            },
            postDays = parsePostDays(main),
            bannerUrl = banner,
            operator = operator,
            followers = followers?.toIntOrNull(),
            following = following?.toIntOrNull(),
            moderationHref = moderation,
            tabs = tabs,
            muted = muted,
            canModerate = canModerate,
        )
    }

    /** The picture a hero's inline `background-image: url(...)` points at. */
    private val BANNER_URL = Regex("url\\(['\"]?([^'\")]+)")

    private fun parsePostDays(scope: Element): List<DailyCount> =
        scope.select("td[data-count], .post-day").mapNotNull { el ->
            val count = el.attr("data-count").toIntOrNull()
                ?: el.text().trim().toIntOrNull() ?: return@mapNotNull null
            val label = el.attr("data-day").ifBlank { el.attr("title") }.ifBlank { "?" }
            DailyCount(label, count)
        }.take(60)

    /* ---------------------------------------------------------------- */
    /*  Live form discovery                                             */
    /* ---------------------------------------------------------------- */

    /**
     * Reads a real <form> out of a page the server rendered for the signed-in user.
     *
     * This matters because the site's comment and submit forms are only rendered once
     * you are authenticated, so their exact field names cannot be read from the public
     * markup or from the JavaScript bundle. Instead of hard-coding names we may get
     * wrong, the app mirrors whatever the server currently expects.
     */
    fun parseForm(html: String, cssSelector: String): DiscoveredForm? {
        val doc = document(html)
        val form = doc.selectFirst(cssSelector) ?: return null
        return buildForm(form)
    }

    /* ---------------------------------------------------------------- */
    /*  Account settings (Personal + Security)                          */
    /* ---------------------------------------------------------------- */

    /* ---------------------------------------------------------------- */
    /*  Notifications                                                   */
    /* ---------------------------------------------------------------- */

    /**
     * `/notifications`, read from the markup the site really sends.
     *
     * Each row is a `li.list-group-item` holding: a `new` badge while it is unread, the
     * age as `span.text-muted.small`, a `context` link straight to the comment, the post
     * it happened on as a link beginning `/post/`, and the quoted body in `.comment-body`.
     * The header bell's own number (`.notif-count`, which the site writes as `9+` when it
     * gets big) is read from the same response, so one request answers both questions.
     */
    fun parseNotifications(html: String): NotificationPage {
        val doc = document(html)
        val scope = doc.selectFirst("#main-content-col") ?: doc
        val items = mutableListOf<DeliNotification>()
        for (row in scope.select("li.list-group-item")) {
            val context = row.selectFirst("a[href*=/comment/]")?.attr("href")?.takeIf { it.isNotBlank() }
            // The `context` link also starts with `/post/`; the post itself is the one
            // without `/comment/` in it.
            val postLink = row.selectFirst("a[href^=/post/]:not([href*=/comment/])")
            val body = row.selectFirst(".comment-body")?.html().orEmpty()
            val ago = row.selectFirst("span.text-muted.small")?.text().orEmpty()
            val unread = row.selectFirst(".badge.bg-danger") != null
            val id = context
                ?: (postLink?.attr("href").orEmpty() + "#" + ago + "#" + body.hashCode())
            items += DeliNotification(
                id = id,
                unread = unread,
                ago = ago,
                href = context,
                postTitle = postLink?.text()?.takeIf { it.isNotBlank() },
                postHref = postLink?.attr("href")?.takeIf { it.isNotBlank() },
                bodyHtml = body,
            )
        }
        val bell = doc.selectFirst(".notif-count")?.text()?.trim().orEmpty()
        val unreadCount = bell.filter { it.isDigit() }.toIntOrNull() ?: items.count { it.unread }
        return NotificationPage(items = items, unreadCount = unreadCount)
    }

    /**
     * The picture forms of the settings page: kind (profile/banner) to the form's action
     * and the name of its file field.
     *
     * Read from the page rather than hard-coded, because a renamed field is exactly the
     * failure that looks like success: the upload is accepted as a request, the picture
     * never changes, and nothing says why. The forms are nested inside the outer settings
     * form on the live page, so the file input is what this looks for, not the form.
     */
    fun parseImageUploadForms(html: String): Map<String, Pair<String, String>> {
        val doc = document(html)
        val out = mutableMapOf<String, Pair<String, String>>()
        for (input in doc.select("input[type=file]")) {
            val field = input.attr("name").takeIf { it.isNotBlank() } ?: continue
            val form = input.parents().firstOrNull { it.tagName() == "form" && it.hasAttr("action") }
            val action = form?.attr("action").orEmpty()
            val banner = action.contains("banner", true) || field.contains("banner", true)
            val kind = if (banner) "banner" else "profile"
            out[kind] = action.ifBlank {
                if (banner) "/settings/images/banner" else "/settings/images/profile"
            } to field
        }
        return out
    }

    /**
     * The signed-in settings pages are submitted with JavaScript, so their `<form>`s
     * have no `action` and the language/theme `<select>`s have no `name` at all — the
     * site reads those two by id. These readers take the current values straight out of
     * the rendered page, so the app shows exactly what the server has.
     */
    fun parsePersonalSettings(html: String): PersonalSettings {
        val doc = document(html)
        // Read by name from the page, not from `form#profile-settings`: the site nests
        // the profile-picture and banner forms *inside* that form, and HTML parsing closes
        // the outer form at the first inner `</form>`, which pushes the Bio textarea out
        // of the form element entirely. Scoping to the form silently produced an empty bio
        // — and saving that back would have wiped whatever the account had.
        val scope = doc.selectFirst("#main-content-col") ?: doc
        return PersonalSettings(
            bio = fieldValue(scope, "bio"),
            pronouns = fieldValue(scope, "pronouns"),
            language = selectedValue(doc, "language-select") ?: "en",
            languageOptions = selectOptions(doc, "language-select"),
            theme = selectedValue(doc, "theme-select") ?: "midnight",
            themeOptions = selectOptions(doc, "theme-select"),
            themeColor = doc.selectFirst("#themecolor-pickr")?.attr("data-initial").orEmpty(),
        )
    }

    /**
     * The security tab. Only the e-mail field carries state: the 2FA QR/verify block is
     * part of the template and simply hidden with `d-none` until setup runs, so it says
     * nothing about whether 2FA is already enabled.
     */
    fun parseSecuritySettings(html: String): SecuritySettings {
        val doc = document(html)
        val scope = doc.selectFirst("#main-content-col") ?: doc
        return SecuritySettings(
            email = scope.selectFirst("input[name=email]")?.attr("value").orEmpty(),
            twoFactorAvailable = doc.selectFirst("form#twofa-setup") != null,
        )
    }

    /** Options of a `<select>` addressed by id, as (value, label) pairs. */
    fun selectOptions(doc: Document, id: String): List<Pair<String, String>> =
        doc.selectFirst("select#$id")?.select("option")?.mapNotNull { o ->
            val v = o.attr("value")
            if (v.isBlank()) null else v to o.text().trim()
        }.orEmpty()

    private fun selectedValue(doc: Document, id: String): String? =
        doc.selectFirst("select#$id option[selected]")?.attr("value")
            ?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("select#$id option")?.attr("value")

    private fun fieldValue(scope: Element?, name: String): String {
        val el = scope?.selectFirst("[name=$name]") ?: return ""
        return when {
            el.tagName().equals("textarea", true) -> el.text()
            el.tagName().equals("select", true) ->
                el.select("option[selected]").attr("value")
                    .ifBlank { el.selectFirst("option")?.attr("value").orEmpty() }
            else -> el.attr("value")
        }
    }

    private fun buildForm(form: Element): DiscoveredForm {
        val fields = mutableListOf<FormField>()

        fun labelFor(el: Element): String? {
            val id = el.id()
            if (id.isNotBlank()) {
                form.selectFirst("label[for=$id]")?.text()?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
            }
            // Fall back to the closest preceding label inside the same block.
            var node = el.previousElementSibling()
            var hops = 0
            while (node != null && hops < 3) {
                if (node.tagName().equals("label", true)) {
                    return node.text().trim().takeIf { it.isNotBlank() }
                }
                node = node.previousElementSibling()
                hops++
            }
            return form.selectFirst("label[for=${el.attr("name")}]")?.text()?.trim()?.takeIf { it.isNotBlank() }
        }

        for (input in form.select("input[name]")) {
            val type = input.attr("type").lowercase()
            if (type == "submit" || type == "button" || type == "file") continue
            val name = input.attr("name")
            if (name == "__RequestVerificationToken") continue

            // NB: jsoup's two-argument attr() is a setter, so read the value and
            // supply the checkbox default ourselves.
            val checked = input.hasAttr("checked")
            if ((type == "checkbox" || type == "radio") && !checked) continue
            val value: String = when (type) {
                "checkbox", "radio" -> input.attr("value").ifBlank { "on" }
                else -> input.attr("value")
            }

            val kind = when (type) {
                "password" -> FieldKind.PASSWORD
                "email" -> FieldKind.EMAIL
                "checkbox", "radio" -> FieldKind.CHECKBOX
                "hidden" -> FieldKind.HIDDEN
                "number", "range" -> FieldKind.NUMBER
                "tel" -> FieldKind.TEL
                "color" -> FieldKind.COLOR
                else -> FieldKind.TEXT
            }
            fields.add(
                FormField(
                    name = name,
                    kind = kind,
                    value = value,
                    label = labelFor(input),
                    placeholder = input.attr("placeholder").takeIf { it.isNotBlank() },
                    required = input.hasAttr("required"),
                    maxLength = input.attr("maxlength").toIntOrNull(),
                )
            )
        }

        for (selectEl in form.select("select[name]")) {
            val options = selectEl.select("option").mapNotNull { o ->
                val v = o.attr("value").ifBlank { o.text().trim() }
                if (v.isBlank() && o.text().isBlank()) null else v to o.text().trim()
            }
            val chosen = selectEl.selectFirst("option[selected]") ?: selectEl.selectFirst("option")
            fields.add(
                FormField(
                    name = selectEl.attr("name"),
                    kind = FieldKind.SELECT,
                    value = chosen?.attr("value") ?: "",
                    label = labelFor(selectEl),
                    required = selectEl.hasAttr("required"),
                    options = options,
                )
            )
        }

        for (area in form.select("textarea[name]")) {
            fields.add(
                FormField(
                    name = area.attr("name"),
                    kind = FieldKind.TEXTAREA,
                    value = area.text(),
                    label = labelFor(area),
                    placeholder = area.attr("placeholder").takeIf { it.isNotBlank() },
                    required = area.hasAttr("required"),
                    maxLength = area.attr("maxlength").toIntOrNull(),
                )
            )
        }

        val textareas = form.select("textarea[name]")
        val bodyField = textareas.firstOrNull { it.attr("name") != "title" }?.attr("name")
            ?: textareas.firstOrNull()?.attr("name")
        val titleField = form.select("textarea[name=title], input[name=title]").firstOrNull()?.attr("name")

        val enctype = form.attr("enctype").lowercase()
        val multipart = enctype.contains("multipart") || form.id() == "submitform"

        return DiscoveredForm(
            action = form.attr("action").ifBlank { "" },
            fields = fields,
            bodyField = bodyField,
            titleField = titleField,
            isMultipart = multipart,
            holeOptions = form.select("select[name=hole] option").mapNotNull { o ->
                val v = o.attr("value")
                if (v.isBlank()) null else v to o.text().trim()
            },
        )
    }

    /** All holes offered in the submit form's <select>. */
    fun holeOptions(html: String): List<Pair<String, String>> =
        document(html).select("select[name=hole] option").mapNotNull { o ->
            val v = o.attr("value")
            if (v.isBlank()) null else v to o.text().trim()
        }

    /** Reads the JSON feed embedded in the HTML page (used as a fallback to the API). */
    fun parseEmbeddedFeedCursor(html: String): String? {
        val doc = document(html)
        val raw = doc.selectFirst("script#feed-config")?.data() ?: return null
        return runCatching { json.parseToJsonElement(raw).jsonObject["cursor"] }
            .getOrNull()?.jsonPrimitive?.contentOrNullSafe()
    }

    private val UUID_RE =
        Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
}

/** A form the server rendered, remade as data so the app can post it back. */
/** How a server-rendered field should be presented in the native UI. */
enum class FieldKind { TEXT, PASSWORD, EMAIL, NUMBER, TEL, TEXTAREA, CHECKBOX, SELECT, HIDDEN, COLOR }

/** One input of a server-rendered form, with everything the UI needs to draw it. */
data class FormField(
    val name: String,
    val kind: FieldKind,
    val value: String,
    val label: String? = null,
    val placeholder: String? = null,
    val required: Boolean = false,
    val maxLength: Int? = null,
    val options: List<Pair<String, String>> = emptyList(),
) {
    /** Hidden inputs are posted back untouched and never drawn. */
    val isVisible: Boolean get() = kind != FieldKind.HIDDEN

    fun displayLabel(): String = label?.takeIf { it.isNotBlank() } ?: name
}

/**
 * A form lifted out of a page the server rendered, so the app can post it back with
 * exactly the field names the current build expects.
 */
data class DiscoveredForm(
    val action: String,
    val fields: List<FormField>,
    val bodyField: String?,
    val titleField: String?,
    val isMultipart: Boolean,
    val holeOptions: List<Pair<String, String>> = emptyList(),
) {
    fun asPairs(): List<Pair<String, String>> = fields
        .filter { it.kind != FieldKind.CHECKBOX || it.value.isNotEmpty() }
        .map { it.name to it.value }

    fun visibleFields(): List<FormField> = fields.filter { it.isVisible }

    fun withValue(name: String, value: String): DiscoveredForm =
        copy(fields = fields.map { if (it.name == name) it.copy(value = value) else it })

    fun withBody(text: String): List<Pair<String, String>> {
        val target = bodyField
        val pairs = fields
            .filter { it.kind != FieldKind.CHECKBOX || it.value.isNotEmpty() }
            .map { f -> if (f.name == target) f.name to text else f.name to f.value }
            .toMutableList()
        if (target == null) pairs.add("body" to text)
        return pairs
    }

    fun withTitle(text: String): List<Pair<String, String>> {
        val target = titleField
        val pairs = fields
            .filter { it.kind != FieldKind.CHECKBOX || it.value.isNotEmpty() }
            .map { f -> if (f.name == target) f.name to text else f.name to f.value }
            .toMutableList()
        if (target == null) pairs.add("title" to text)
        return pairs
    }
}

/* --------------------------- small JSON helpers --------------------------- */

private fun JsonObject.str(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.contentOrNullSafe()?.takeIf { it.isNotBlank() } }

private fun JsonObject.int(vararg keys: String): Int? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.intOrNull }

private fun JsonObject.bool(vararg keys: String): Boolean? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.booleanOrNull }

private fun JsonObject.obj(key: String): JsonObject? =
    runCatching { this[key]?.jsonObject }.getOrNull()

private fun JsonPrimitive.contentOrNullSafe(): String? =
    runCatching { if (this is JsonPrimitive && this.isString) content else content }.getOrNull()
