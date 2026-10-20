package com.delidehset.mobile.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import com.delidehset.mobile.ui.tr

/**
 * The site now wraps its JSON answers as `{"meta":{status,messages,details},"data":…}`.
 *
 * Older builds (and a few endpoints) still answer with the bare payload, so every reader
 * goes through here: an enveloped body gives back `data`, anything else comes back as is.
 */
object ApiEnvelope {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(body: String): JsonElement? = runCatching { json.parseToJsonElement(body) }.getOrNull()

    fun isEnveloped(root: JsonElement?): Boolean =
        root is JsonObject && root["meta"] is JsonObject && root.containsKey("data")

    /** The payload of [root]: its `data` when enveloped, otherwise [root] itself. */
    fun payload(root: JsonElement?): JsonElement? =
        if (isEnveloped(root)) (root as JsonObject)["data"] else root

    fun payload(body: String): JsonElement? = payload(parse(body))

    /** The payload of [body] when it is an object, else null. */
    fun payloadObject(body: String): JsonObject? = payload(body) as? JsonObject

    /** `meta.status`, or null when the body carries no envelope. */
    fun status(root: JsonElement?): Boolean? =
        ((root as? JsonObject)?.get("meta") as? JsonObject)
            ?.get("status")?.let { (it as? JsonPrimitive)?.booleanOrNull }

    /** The first non-blank `meta.messages` entry (a code such as `I_OK` on success). */
    fun message(root: JsonElement?): String? =
        (((root as? JsonObject)?.get("meta") as? JsonObject)?.get("messages") as? JsonArray)
            ?.firstNotNullOfOrNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf { s -> s.isNotBlank() } }

    /**
     * `meta.messages` carries codes (`E_FORBIDDEN`, `W_ALREADY_FOLLOWING`…), not text.
     * Known ones become a sentence; an unknown code gives null so the caller falls back
     * to the HTTP explanation instead of showing the raw code.
     */
    fun humanize(code: String?): String? {
        val c = code?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (!Regex("^[EWI]_[A-Z0-9_]+$").matches(c)) return c // already a sentence
        return when (c) {
            "E_UNAUTHORIZED", "E_NOT_LOGGED_IN" -> tr("Önce oturum açmalısın.")
            "E_FORBIDDEN" -> tr("Bunu yapmaya iznin yok.")
            "E_USER_NOT_FOUND" -> tr("Kullanıcı bulunamadı.")
            "E_POST_NOT_FOUND" -> tr("Gönderi bulunamadı.")
            "E_COMMENT_NOT_FOUND" -> tr("Yorum bulunamadı.")
            "E_DELETE_FAILED" -> tr("Silinemedi, tekrar dene.")
            "E_SELF_ACTION_NOT_ALLOWED" -> tr("Bunu kendine yapamazsın.")
            "E_CANNOT_SUBSCRIBE_OWN_POST" -> tr("Kendi gönderine abone olamazsın.")
            "E_NO_IMAGE", "E_IMAGE_TYPE" -> tr("Bir görsel yüklemelisin (png/jpg/gif/webp).")
            "E_IMAGE_TOO_LARGE" -> tr("Dosya çok büyük (en fazla 8 MB).")
            "E_UPLOADS_DISABLED" -> tr("Görsel yükleme kapalı.")
            "E_MUTED" -> tr("Susturulmuşken yükleme yapamazsın.")
            "W_ALREADY_FOLLOWING" -> tr("Zaten takip ediyorsun.")
            "W_NOT_FOLLOWING" -> tr("Zaten takip etmiyorsun.")
            "W_ALREADY_MUTED" -> tr("Bildirimleri zaten susturdun.")
            "W_NOT_MUTED" -> tr("Bu kullanıcının bildirimlerini susturmamışsın.")
            else -> null
        }
    }

    /** Unwraps [body] to its payload text so serializers written for the bare shape still work. */
    fun unwrapText(body: String): String {
        val root = parse(body)
        return if (isEnveloped(root)) (payload(root)?.toString() ?: body) else body
    }
}
