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

        // The sidebar's account block links to the profile (`/@name`) and holds the picture
        // and the name; the older markup reached the same block through the settings link.
        val ownLink = sidebar.selectFirst("a[href^=/@]:has(img[src])")

        val avatar = ownLink?.selectFirst("img[src]")?.attr("src")?.takeIf { it.isNotBlank() }
            ?: profileLink?.selectFirst("img[src]")?.attr("src")
            ?: sidebar.selectFirst("img[src*=/avatar/]")?.attr("src")
            ?: sidebar.selectFirst("img[src*=/pp/]")?.attr("src")

        val name = ownLink?.selectFirst("strong")?.text()?.trim()?.takeIf { it.isNotBlank() }
            ?: profileLink?.selectFirst("strong")?.text()?.trim()?.takeIf { it.isNotBlank() }
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

    fun parsePost(html: String, permalink: String): PostDetail? =
        parsePost(document(html), permalink, html)

    fun parsePost(doc: Document, permalink: String, rawHtml: String = ""): PostDetail? {
        val root = doc.selectFirst("#post-root") ?: return null
        // NB: #post-root itself matches [id^=post-], so the wrapper is matched by
        // its UUID-shaped id instead of by prefix alone.
        val container = root.selectFirst("div[id^=post-]:not(#post-root)")
            ?: doc.selectFirst("div[id^=post-]:not(#post-root)")
            ?: doc.selectFirst("#post-root")
        val id = container?.id()?.removePrefix("post-")?.takeIf { UUID_RE.matches(it) }
            ?: UUID_RE.find(rawHtml.ifEmpty { doc.outerHtml() })?.value
            ?: return null

        val title = doc.selectFirst("#post-title")?.text()?.trim().orEmpty()
        val body = doc.selectFirst("#post-body")?.html().orEmpty()

        val meta = doc.selectFirst(".post-meta")
        val authorLink = meta?.selectFirst("a.user-name") ?: meta?.selectFirst("a[href^=/@]")
        val holeLink = meta?.select("a[href^=/h/]")?.firstOrNull()
        val timeTexts = meta?.select("span.text-muted")?.map { it.text() } ?: emptyList()
        val ago = timeTexts.firstOrNull { it.contains("ago") || HAS_DIGIT_RE.matches(it) }
        val edited = timeTexts.firstOrNull { it.contains("edited", ignoreCase = true) }

        val commentCount = doc.selectFirst(".post-actions")?.text()
            ?.let { FIRST_INT_RE.find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: 0

        val viewerVote = root.attr("data-page-post-vote").toIntOrNull() ?: 0

        // Rendered only for signed-in viewers; null means "not shown on this page".
        val score = doc.selectFirst("[data-page-post-score]")?.attr("data-page-post-score")?.toIntOrNull()
            ?: doc.selectFirst("#post-score")?.text()?.trim()?.toIntOrNull()
            ?: doc.selectFirst(".post-actions .score")?.text()?.trim()?.toIntOrNull()

        val deleted = body.contains("deleted", ignoreCase = true) && body.length < 200
        val authorName = authorLink?.text()?.trim()?.removePrefix("@")
        val authorAvatar = doc.selectFirst(".actual-post img[src*=/avatar/], .actual-post img[src*=/uploads/], .actual-post img[src*=/pp/]")?.attr("src")
        AvatarStore.put(authorName, authorAvatar)

        return PostDetail(
            id = id,
            permalink = permalink,
            title = title.ifBlank { "(başlıksız)" },
            bodyHtml = body,
            hole = holeLink?.text()?.removePrefix("/h/")?.trim(),
            holeHref = holeLink?.attr("href"),
            authorName = authorName,
            authorHref = authorLink?.attr("href"),
            authorAvatar = authorAvatar,
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
    fun parseComments(html: String): List<CommentNode> = parseComments(document(html))

    fun parseComments(doc: Document): List<CommentNode> {
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
            authorColor = o.str("flair_text_color", "flairTextColor", "text_color", "author_color")
                ?: author?.str("color", "text_color"),
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
                authorAvatar = el.selectFirst("img[src*=/avatar/], img[src*=/uploads/], img[src*=/pp/]")?.attr("src"),
                bodyHtml = body,
                score = el.selectFirst(".comment-score, .score")?.text()?.trim()?.toIntOrNull() ?: 0,
                viewerVote = 0,
                ago = el.selectFirst(".text-muted")?.text(),
                isDeleted = body.isBlank(),
            )
        }
        return nodes.filter { it.parentId == null || nodes.none { p -> p.id == it.parentId } }
    }

    fun commentVotes(html: String): Map<String, Int> = commentVotes(document(html))

    fun commentVotes(doc: Document): Map<String, Int> {
        val raw = doc.selectFirst("#post-root")?.attr("data-page-comment-votes")
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

    /**
     * The community table, read by its column headings.
     *
     * It used to be read by position with a guess for the counting column, and the guess
     * landed on the row's own number: the app showed one post, then two, then three, for
     * communities the site says have none — the first cell is an index and the
     * first cell that parses as a number is that index. The headings say which column is
     * which, and the site renders a different table for admins (with an Admin column at
     * the end), so names are the only stable thing here.
     */
    fun parseHoles(html: String): List<Hole> {
        val doc = document(html)
        val headers = doc.select("table thead th").map { it.text().trim().lowercase() }
        fun columnOf(vararg keys: String): Int =
            headers.indexOfFirst { header -> keys.any { header.contains(it) } }

        val nameColumn = columnOf("hole name", "topluluk", "hole", "isim")
        val postsColumn = columnOf("posts", "gönderi", "postlar")
        val dateColumn = columnOf("created", "tarih")
        val stealthColumn = columnOf("stealth", "gizli")
        val publicColumn = columnOf("public", "herkese", "halka")

        return doc.select("table tbody tr").mapNotNull { row ->
            val cells = row.select("td")
            if (cells.size < 3) return@mapNotNull null
            val nameCell = cells.getOrNull(nameColumn)
                ?: cells.firstOrNull { it.selectFirst("a[href^=/h/]") != null }
            val link = nameCell?.selectFirst("a[href^=/h/]")
                ?: cells.firstNotNullOfOrNull { it.selectFirst("a[href^=/h/]") }
                ?: return@mapNotNull null
            val texts = cells.map { it.text().trim() }
            val dateCell = texts.getOrNull(dateColumn)?.takeIf { DATE.containsMatchIn(it) }
                ?: texts.firstOrNull { DATE.containsMatchIn(it) }

            // The counting column by heading; without a table head, any cell that says
            // "post"/"gönderi", and otherwise the first *linked* number after the index.
            val postsCell = texts.getOrNull(postsColumn)
                ?: texts.firstOrNull { it.contains("gönderi", true) || it.contains("post", true) }
                ?: cells.drop(1).firstOrNull { cell ->
                    cell.selectFirst("a[href^=/h/]") != null && countOf(cell.text()) != null
                }?.text()

            val name = link.text().removePrefix("/h/").trim()
            val rowLogo = row.selectFirst("img[src]")?.attr("src")?.trim()?.takeIf { it.isNotBlank() }
            val inviteOnly = nameCell?.select(".badge")?.any {
                it.text().contains("invite", true) || it.text().contains("davet", true)
            } == true
            if (rowLogo != null) HoleStore.put(name, logo = rowLogo)

            Hole(
                name = name,
                href = link.attr("href"),
                postCount = countOf(postsCell)?.toIntOrNull() ?: 0,
                createdOn = dateCell,
                stealth = texts.getOrNull(stealthColumn),
                publicUse = texts.getOrNull(publicColumn),
                // A community picture, when the table carries one or HoleStore remembered it.
                logo = rowLogo ?: HoleStore.cachedLogo(name),
                displayName = HoleStore.cachedLabel(name),
                inviteOnly = inviteOnly,
            )
        }
    }

    /**
     * How many accounts a followers/following page holds.
     *
     * The profile header's counters are not refreshed when a follow happens — the site's
     * own page keeps saying "follows 0 users" while the list behind the link is one page
     * long — so the heading of the list, `cronus Follows (1)`, is the honest number.
     * Returns null when the page is not one of those two.
     */
    fun parseRelationTotal(html: String): Int? {
        val doc = document(html)
        val heading = doc.selectFirst("#main-content-col h3, #main-content-col h2")?.text()?.trim()
            ?: return null
        if (!heading.contains("Follow", true) && !heading.contains("takip", true)) return null
        val counted = RELATION_COUNT_RE.find(heading) ?: return null
        return countOf(counted.groupValues[1] + counted.groupValues[2])?.toIntOrNull()
    }

    /** A community's settings page: the three values the app can edit, and the permission. */
    fun parseHoleSettings(html: String, hole: String): HoleSettings? {
        val doc = document(html)
        val main = doc.selectFirst("#main-content-col") ?: return null
        val hasSettingsMarker = doc.title().contains("Settings", true) ||
            doc.title().contains("ayar", true) ||
            main.selectFirst("input[name=display_name], textarea[name=blocked_words], form[action*=marsey_image], .settings-nav, h4") != null
        if (!hasSettingsMarker) return null

        val display = main.selectFirst("input[name=display_name]")?.attr("value")?.trim().orEmpty()
        // Textareas carry the saved value as their text, not as an attribute.
        val ips = main.selectFirst("textarea[name=blocked_ip_ranges]")?.text().orEmpty()
        val words = main.selectFirst("textarea[name=blocked_words]")?.text().orEmpty()
        val logo = main.selectFirst("img[alt=Hole logo]")?.attr("src")?.trim()?.takeIf { it.isNotBlank() }
        HoleStore.put(hole, logo = logo, label = display)
        val canEdit = main.selectFirst("form[onsubmit*=saveHoleDisplayName]") != null ||
            main.selectFirst("form[onsubmit*=saveHoleContentFilters]") != null ||
            main.selectFirst("form[action*=marsey_image]") != null
        return HoleSettings(
            hole = hole,
            displayName = display,
            blockedIps = ips.trim(),
            blockedWords = words.trim(),
            logoUrl = logo,
            canEdit = canEdit,
        )
    }

    /**
     * The lock screen of an invite-only community.
     *
     * Returns null for any other page, so the caller can tell "this community is closed"
     * from "this page failed". The note under the heading is written by the community's
     * own moderators, which is why it is shown as it is rather than replaced with an
     * app-level sentence.
     */
    fun parseHoleLock(html: String): HoleAccess? {
        val doc = document(html)
        val main = doc.selectFirst("#main-content-col") ?: return null
        val heading = main.selectFirst("h1")?.text()?.trim() ?: return null
        val locked = heading.contains("is private", true) || heading.contains("gizli", true) ||
            doc.selectFirst("[onclick*=openJoinModal]") != null
        if (!locked) return null
        val reason = main.selectFirst("p.text-muted")?.text()?.trim()?.takeIf { it.isNotBlank() }
        val canRequest = doc.selectFirst("[onclick*=openJoinModal]") != null
        return HoleAccess(locked = true, reason = reason, canRequest = canRequest)
    }

    /**
     * The site's rules page, as the lines it is written in.
     *
     * `#rules` is the block the site's own sidebar renders: a heading, the rules as list
     * items, then the "Bilgi" section. Each line is kept whole so the app can show them as
     * paragraphs without guessing which part is a heading.
     */
    fun parseRules(html: String): SiteRules {
        val doc = document(html)
        val root = doc.selectFirst("#rules") ?: return SiteRules()
        val blocks = mutableListOf<String>()
        for (element in root.select("h1, h2, h3, h4, li, p")) {
            val text = element.text().trim()
            if (text.isBlank()) continue
            if (text.none { it.isLetter() }) continue
            if (blocks.lastOrNull() == text) continue
            blocks += text
        }
        return SiteRules(blocks = blocks)
    }

    fun parseHoleHeader(html: String): Hole? {
        val doc = document(html)
        val link = doc.selectFirst("#main-content-col h1 a[href^=/h/]")
            ?: doc.selectFirst(".feed-head-name[href^=/h/]")
            ?: return null
        val name = link.text().removePrefix("/h/").trim().ifBlank { return null }
        // The header of a community page counts its posts too; it used to be recorded as a
        // flat zero, which is why opening /h/general showed an empty community.
        val counted = doc.selectFirst("#hole--posts, #hole-posts, .hole-post-count")
            ?.text()
            ?: doc.select("#main-content-col .text-muted").firstOrNull { it.text().contains("gönderi", true) }?.text()
        val state = parseHoleState(doc)
        return Hole(
            name = name,
            href = link.attr("href"),
            postCount = countOf(counted)?.toIntOrNull() ?: 0,
            createdOn = null,
            stealth = null,
            publicUse = null,
            description = doc.selectFirst(".hole-description, #main-content-col .text-muted")?.text()?.trim(),
            logo = state.logo ?: HoleStore.cachedLogo(name),
            displayName = state.displayName ?: HoleStore.cachedLabel(name),
        )
    }

    /** `17 Eki 2026` / `29 September 2026`: the row's date, so it is not read as a count. */
    private val DATE = Regex("\\d{1,2}\\s+\\p{L}+\\s+\\d{4}")

    /**
     * A count as the site prints it: `1`, `1,234`, `12.3k`, `1.2K`, `1.4m`.
     *
     * Reading only the leading digits — which is what the profile used to do — turned 12.3k
     * followers into 12, and a thousand into 1.
     */
    internal fun countOf(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val match = COUNT_OF_RE.find(text) ?: return null
        val raw = match.groupValues[1].trimEnd('.', ',')
        val suffix = match.groupValues[2].lowercase()
        val value = if (suffix.isEmpty()) {
            raw.filter { it.isDigit() }.toLongOrNull()
        } else {
            raw.replace(",", ".").toDoubleOrNull()?.times(
                when (suffix) {
                    "k" -> 1_000.0
                    "m" -> 1_000_000.0
                    else -> 1_000_000_000.0
                }
            )?.toLong()
        } ?: return null
        return value.toString()
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
    /**
     * A community page's own two switches: joined, and blocked.
     *
     * The page renders both states as a pair of buttons where exactly one carries
     * `d-none` — `#follow-hole`/`#unfollow-hole` for joining, `#block-hole`/
     * `#unblock-hole` for blocking. A guest page has neither pair, so `null` is "the page
     * did not say", not "no".
     */
    fun parseHoleState(html: String): HoleState = parseHoleState(document(html))

    fun parseHoleState(doc: Document): HoleState {
        val joined = when {
            doc.selectFirst("#unfollow-hole") != null ->
                doc.selectFirst("#unfollow-hole")?.hasClass("d-none") == false
            doc.selectFirst("#follow-hole") != null ->
                doc.selectFirst("#follow-hole")?.hasClass("d-none") == true
            else -> null
        }
        val blocked = when {
            doc.selectFirst("#unblock-hole") != null ->
                doc.selectFirst("#unblock-hole")?.hasClass("d-none") == false
            doc.selectFirst("#block-hole") != null ->
                doc.selectFirst("#block-hole")?.hasClass("d-none") == true
            else -> null
        }
        // Only the people who run the community are shown this link; a member — or
        // anybody else — must not be offered settings the site would refuse.
        // `*=`, not `$=`: the site's own link to that page can carry a query string
        // (`/h/x/settings?tab=general`), and requiring it to *end* with `/settings` meant the
        // gear never appeared for the people the community actually lets in there.
        val canManage = doc.selectFirst("a[href^=/h/][href*=/settings]") != null ||
            doc.selectFirst("form[action*=/settings], form[action*=/marsey_image], form[onsubmit*=saveHole]") != null
        // The community's own picture: the sidebar paints it as `<img alt="/h/x icon">` on
        // every one of that community's pages. The feed's own `hole_logo` is the site's
        // generic file, so this is the only place the real picture can be read from.
        val logoImg = doc.selectFirst("img[alt^=/h/][alt$=icon]")
        val logo = logoImg?.attr("src")?.trim()?.takeIf { it.isNotBlank() && !HoleStore.isGenericLogo(it) }
        val holeSlug = logoImg?.attr("alt")
            ?.removePrefix("/h/")
            ?.removeSuffix("icon")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val displayName = logoImg?.parent()?.selectFirst(".font-weight-bold")?.text()?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: doc.title().let { title ->
                if (title.contains("(/h/")) title.substringBefore("(/h/").trim().takeIf { it.isNotBlank() }
                else null
            }
        if (holeSlug != null) {
            HoleStore.put(holeSlug, logo = logo, label = displayName)
        }
        return HoleState(
            joined = joined,
            blocked = blocked,
            canManage = canManage,
            logo = logo,
            displayName = displayName,
        )
    }

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

        // Holes / Communities tab: list-group of community cards
        val holes = pane.select("a.list-group-item[href^=/h/]")
        if (holes.isNotEmpty()) {
            return holes.mapNotNull { link ->
                val name = link.selectFirst("b")?.text()?.trim() ?: link.text().trim()
                val desc = link.selectFirst(".text-muted, .text-small")?.text()?.trim().orEmpty()
                if (name.isBlank()) null
                else SimpleRow(
                    cells = if (desc.isNotBlank()) listOf(name, desc) else listOf(name),
                    href = link.attr("href"),
                )
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
                price = BADGE_PRICE_RE.find(el.text())?.value,
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
        val followers = countOf(main.selectFirst("#profile--followers")?.text())
        val following = countOf(main.selectFirst("#profile--following")?.text())
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
            ?.replace(JOINED_PREFIX_RE, "")
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
        // The account's own pronouns, which the site prints under the name. The app could
        // already *set* them in the account settings and never showed them here.
        val pronouns = main.selectFirst("#profile--pronouns")?.text()?.trim()?.let { text ->
            PRONOUNS_PREFIX_RE.replaceFirst(text, "").trim().takeIf { it.isNotBlank() }
        }

        val moderation = main.selectFirst("a[href*=target_id=]")?.attr("href")
            ?.takeIf { it.isNotBlank() }

        // The profile's own tabs, with the counts the site prints beside them.
        val tabs = main.select(".settings-nav a.nav-link, #content a.nav-link").mapNotNull { a ->
            val href = a.attr("href").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val count = a.selectFirst(".count")?.text()?.let { FIRST_INT_RE.find(it)?.value?.toIntOrNull() }
            ProfileTab(
                label = a.text().replace(PAREN_INT_RE, "").trim(),
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

        // The wall itself: the same cards the feed draws, so the first tab is the
        // person's posts rather than an empty page with two counters on it.
        val wall = feedCardsFrom(doc)
        val avatarUrl = main.selectFirst("#profile--pfp img[src]")?.attr("src")
            ?: main.selectFirst("img[src*=/avatar/]")?.attr("src")
            ?: main.selectFirst("img[src*=/pp/]")?.attr("src")
        AvatarStore.put(name, avatarUrl)

        return UserProfile(
            id = main.selectFirst("#profile--info--id")?.text()?.substringAfter("ID:")?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: UUID_RE.find(html)?.value.orEmpty(),
            name = name,
            href = "/@$name",
            bio = bio,
            avatarUrl = avatarUrl,
            joinedAgo = joined,
            stats = stats,
            isFollowing = when {
                unsubButton != null -> !unsubButton.hasClass("d-none")
                subButton != null -> subButton.hasClass("d-none")
                else -> main.selectFirst("a[href*=unfollow]") != null
            },
            postDays = parsePostDays(main),
            bannerUrl = banner,
            pronouns = pronouns,
            operator = operator,
            followers = followers?.toIntOrNull(),
            following = following?.toIntOrNull(),
            moderationHref = moderation,
            tabs = tabs,
            muted = muted,
            canModerate = canModerate,
            wallPosts = wall,
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
    fun parseForm(html: String, cssSelector: String): DiscoveredForm? =
        parseForm(document(html), cssSelector)

    fun parseForm(doc: Document, cssSelector: String): DiscoveredForm? {
        val form = doc.selectFirst(cssSelector) ?: return null
        return buildForm(form)
    }

    /* ---------------------------------------------------------------- */
    /*  Account settings (Personal + Security)                          */
    /* ---------------------------------------------------------------- */

    /* ---------------------------------------------------------------- */
    /*  A profile's own lists: its posts, then its comments            */
    /* ---------------------------------------------------------------- */

    /**
     * `/@name/posts` — the same `.actual-post` cards the feed draws, read into [FeedPost]
     * so the profile can use the app's own post card instead of a browser.
     *
     * The site prints no JSON for this list (the feed API ignores an author filter), so the
     * page is the only source. Everything it does print — title, hole, score, comment
     * count, age — is taken from the card, and a card without a title link is skipped
     * rather than shown as a blank row.
     */
    fun parseUserPosts(html: String): List<FeedPost> = feedCardsFrom(document(html))

    /** The `.actual-post` cards inside a page, shared by the wall and the posts tab. */
    private fun feedCardsFrom(doc: org.jsoup.nodes.Document): List<FeedPost> {
        val cards = doc.select(".posts .actual-post, .posts article, .posts .card")
        return cards.mapNotNull { card ->
            val titleLink = card.selectFirst("h5.card-title a")
                ?: card.selectFirst("a.stretched-link")
                ?: card.selectFirst("a[href*=/post/]")
            val href = titleLink?.attr("href").orEmpty()
            val title = titleLink?.text()?.trim().orEmpty()
            if (title.isBlank() || href.isBlank()) return@mapNotNull null
            val meta = card.selectFirst(".post-meta")?.text().orEmpty()
            FeedPost(
                id = card.id().removePrefix("post-"),
                permalink = href,
                titleHtml = title,
                ago = meta.substringAfter("·", "").trim().substringBefore(" in ").ifBlank { null },
                score = card.selectFirst(".score")?.text()?.trim()?.toIntOrNull() ?: 0,
                commentCount = card.selectFirst(".post-actions a")?.text()
                    ?.filter { it.isDigit() }?.toIntOrNull() ?: 0,
                hole = card.selectFirst("a.hole-link")?.text()?.trim()
                    ?.removePrefix("/h/")?.takeIf { it.isNotBlank() },
                authorName = card.selectFirst("a.user-name")?.text()?.trim()
                    ?.removePrefix("@")?.takeIf { it.isNotBlank() },
                authorHref = card.selectFirst("a.user-name")?.attr("href")?.takeIf { it.isNotBlank() },
                // These cards are the profile's own post list, and they carry the author's
                // picture right beside the name. Leaving it out was why the same post had
                // a face in the feed and a bare initial under a profile.
                authorAvatar = card.selectFirst(
                    "a.user-name img, img[src*=/uploads/], img[src*=/avatar/], img[src*=/pp/]"
                )?.attr("src")?.takeIf { it.isNotBlank() },
                thumbUrl = card.selectFirst("img.post-img")?.attr("src")
                    ?.takeIf { it.isNotBlank() && !it.contains("default_") },
                hasThumbnail = card.selectFirst("img.post-img") != null,
            )
        }
    }

    /**
     * `/@name/comments` — the cards under `#comment-<uuid>`.
     *
     * Each one names the post it belongs to as a `/post/<uuid>` link, which is a page the
     * site answers directly, so a tap can open it in the app.
     */
    fun parseUserComments(html: String): List<UserComment> {
        val doc = document(html)
        return doc.select("div[id^=comment-]").mapNotNull { card ->
            val id = card.id().removePrefix("comment-")
            val body = card.selectFirst(".comment-body")?.html().orEmpty()
            if (id.isBlank() && body.isBlank()) return@mapNotNull null
            val meta = card.selectFirst(".comment-meta")
            val postLink = meta?.selectFirst("a[href^=/post/]")
            // The site labels these "post <uuid>"; the uuid is not a title, so the row
            // shows the link only when the label is worth reading.
            val label = postLink?.text()?.trim()
                ?.takeIf { it.isNotBlank() && !it.startsWith("post ", ignoreCase = true) }
            // The card's own score sits beside the vote arrows. The site has moved that
            // markup around before, so several shapes are accepted and a row without any of
            // them simply shows no score instead of a wrong zero.
            val score = countOf(
                card.selectFirst(".comment-score, .vote-score, .score, [data-score]")?.text()
            )?.toIntOrNull()
            UserComment(
                id = id,
                ago = meta?.selectFirst("span.text-muted")?.text()?.trim().orEmpty(),
                postHref = postLink?.attr("href")?.takeIf { it.isNotBlank() },
                postLabel = label,
                bodyHtml = body,
                score = score ?: 0,
            )
        }
    }

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
    private val HAS_DIGIT_RE = Regex(".*\\d.*")
    private val FIRST_INT_RE = Regex("(\\d+)")
    private val RELATION_COUNT_RE = Regex("\\((\\d[\\d.,]*)\\s*([kKmM])?\\)")
    private val COUNT_OF_RE = Regex("(\\d[\\d.,]*)\\s*([kKmMbB])?")
    private val BADGE_PRICE_RE = Regex("(\\d+)\\s*(mern|marseybux|coin|token)", RegexOption.IGNORE_CASE)
    private val JOINED_PREFIX_RE = Regex("(?i)^joined\\s*")
    private val PRONOUNS_PREFIX_RE = Regex("(pronouns?|zamirler?)\\s*:?\\s*", RegexOption.IGNORE_CASE)
    private val PAREN_INT_RE = Regex("\\(\\d+\\)")
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

internal fun JsonPrimitive.contentOrNullSafe(): String? =
    runCatching { if (this is JsonPrimitive && this.isString) content else content }.getOrNull()
