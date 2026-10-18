package com.delidehset.mobile.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.delidehset.mobile.ui.theme.DeliColors
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Post bodies and comments arrive as rich HTML (paragraphs, quote blocks, spoilers,
 * embedded images). Instead of handing that to a WebView we translate the markup into
 * an [AnnotatedString], and collect images so they can be loaded with Coil.
 */
object DeliHtml {

    data class Rendered(
        val text: AnnotatedString,
        val images: List<String>,
        /** Clip URLs in the body; played in the app instead of dropped. */
        val videos: List<String> = emptyList(),
    )

    private const val MAX_IMAGES = 12

    fun render(html: String?): Rendered {
        if (html.isNullOrBlank()) return Rendered(AnnotatedString(""), emptyList())
        val body = Jsoup.parseBodyFragment(html).body()
        val images = mutableListOf<String>()
        val videos = mutableListOf<String>()
        body.select("video[src]").forEach { videos += absolutize(it.attr("src")) }
        body.select("video source[src], video[data-src]").forEach {
            val src = it.attr("src").ifBlank { it.attr("data-src") }
            if (src.isNotBlank()) videos += absolutize(src)
        }
        val builder = AnnotatedString.Builder()

        fun emit(node: Node, listDepth: Int) {
            when (node) {
                is TextNode -> builder.append(node.text().replace('\u00A0', ' '))
                is Element -> emitElement(node, images, builder, listDepth) { child -> emit(child, listDepth) }
                else -> {}
            }
        }

        body.childNodes().forEach { emit(it, 0) }

        return Rendered(
            text = trimTrailingBlankLines(buildAnnotatedString { append(builder.toAnnotatedString()) }),
            images = images.distinct().take(MAX_IMAGES),
            videos = videos.distinct(),
        )
    }

    private fun emitElement(
        el: Element,
        images: MutableList<String>,
        b: AnnotatedString.Builder,
        listDepth: Int,
        recurse: (Node) -> Unit,
    ) {
        val tag = el.tagName().lowercase()
        val style = el.attr("style").lowercase()
        val classes = el.className().lowercase()

        fun children() = el.childNodes().forEach { recurse(it) }

        when (tag) {
            "script", "style", "head", "template" -> Unit

            "br" -> b.append("\n")

            "p", "div", "section", "article" -> {
                if (b.length > 0 && !b.endsWithNewline()) b.append("\n\n")
                children()
                if (tag == "p") b.append("\n")
            }

            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                if (b.length > 0 && !b.endsWithNewline()) b.append("\n\n")
                b.withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DeliColors.OnBackground)) {
                    children()
                }
                b.append("\n")
            }

            "b", "strong" -> b.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { children() }
            "i", "em" -> b.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { children() }
            "u" -> b.withStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) { children() }
            "del", "s", "strike" ->
                b.withStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) { children() }

            "code", "pre" -> b.withStyle(
                SpanStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    background = DeliColors.SurfaceVariant,
                    color = DeliColors.OnBackground,
                )
            ) { children() }

            "blockquote" -> {
                b.append("\n")
                b.withStyle(SpanStyle(color = DeliColors.OnSurfaceVariant, fontStyle = FontStyle.Italic)) {
                    b.append("\u2503 ")
                    children()
                }
                b.append("\n")
            }

            "a" -> {
                val href = el.attr("href")
                if (href.startsWith("/") || href.startsWith("http")) {
                    val absolute = if (href.startsWith("/")) "https://delidehset.com$href" else href
                    b.withLink(LinkAnnotation.Url(absolute, androidx.compose.ui.text.TextLinkStyles(
                        style = SpanStyle(color = DeliColors.Blue, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                    ))) { children() }
                } else {
                    children()
                }
            }

            "img" -> {
                val src = el.attr("src")
                if (src.isNotBlank() && !src.contains("emoji")) images += absolutize(src)
            }

            "span" -> {
                val isSpoiler = classes.contains("spoiler") || style.contains("spoiler")
                val hasImage = classes.contains("img-embed") || classes.contains("rt-img")
                if (hasImage) {
                    el.select("img").forEach { img ->
                        val src = img.attr("src")
                        if (src.isNotBlank()) images += absolutize(src)
                    }
                } else if (isSpoiler) {
                    // Keep spoilers readable: dim them rather than hiding the text.
                    b.withStyle(SpanStyle(background = DeliColors.SurfaceVariant, color = DeliColors.OnSurfaceVariant)) {
                        children()
                    }
                } else {
                    children()
                }
            }

            "ul", "ol" -> {
                b.append("\n")
                el.select("> li").forEachIndexed { index, li ->
                    repeat(listDepth) { b.append("    ") }
                    b.append(if (tag == "ul") "\u2022 " else "${index + 1}. ")
                    li.childNodes().forEach { recurse(it) }
                    b.append("\n")
                }
            }

            "hr" -> b.append("\n\u2500\u2500\u2500\u2500\u2500\n")

            else -> children()
        }
    }

    private fun AnnotatedString.Builder.endsWithNewline(): Boolean =
        length > 0 && toAnnotatedString().text.lastOrNull() == '\n'

    private fun absolutize(src: String): String = when {
        src.startsWith("http") -> src
        src.startsWith("//") -> "https:$src"
        src.startsWith("/") -> "https://delidehset.com$src"
        else -> "https://delidehset.com/$src"
    }

    private fun trimTrailingBlankLines(s: AnnotatedString): AnnotatedString {
        val t = s.text.trimEnd()
        return if (t.length == s.text.length) s else AnnotatedString(t)
    }

    /** Plain one-line preview used on feed cards. */
    fun excerpt(html: String?): String = if (html.isNullOrBlank()) "" else Jsoup.parse(html).text()
}
