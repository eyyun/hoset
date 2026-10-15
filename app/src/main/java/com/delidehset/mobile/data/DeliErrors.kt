package com.delidehset.mobile.data

import com.delidehset.mobile.ui.tr
import com.delidehset.mobile.ui.trf
import java.io.EOFException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.coroutines.delay

/**
 * Turns low level failures into something a person can act on.
 *
 * delidehset.com runs as an early beta behind Cloudflare, so timeouts and the odd 5xx
 * are normal: the messages say so instead of showing a stack trace.
 */
object DeliErrors {

    fun explain(t: Throwable): String = when (t) {
        is NotSignedInException ->
            tr("Bu bölüm için oturum açman gerekiyor. Profil sekmesinden giriş yapabilirsin.")

        is SocketTimeoutException ->
            tr("Delidehşet zamanında yanıt vermedi. Site beta olduğu için arada yavaşlıyor — tekrar dene.")

        is UnknownHostException ->
            tr("Siteye ulaşılamadı. İnternet bağlantını kontrol et.")

        is ConnectException, is NoRouteToHostException ->
            tr("Sunucuya bağlanılamadı. Bağlantın kısıtlı olabilir (VPN/proxy).")

        is SSLException ->
            tr("Güvenli bağlantı kurulamadı. Ağ bir sertifikaya müdahale ediyor olabilir.")

        is InterruptedIOException, is EOFException ->
            tr("Bağlantı yarıda kesildi. Tekrar dene.")

        is DeliException -> t.message ?: tr("Beklenmeyen bir hata oluştu.")

        else -> t.message?.takeIf { it.isNotBlank() } ?: tr("Beklenmeyen bir hata oluştu.")
    }

    // The wording is a template and the values are filled in afterwards, so the sentence
    // can be translated as a whole — an interpolated message has already been assembled by
    // the time anyone could translate it, and would stay Turkish in every language.
    fun explainHttp(code: Int, what: String = tr("İçerik")): String = when (code) {
        400 -> trf("{0} isteği sunucu tarafından reddedildi (400). Alanları kontrol et.", what)
        401 -> trf("{0} için oturum açman gerekiyor (401).", what)
        403 -> tr("Sunucu isteği reddetti (403). Büyük olasılıkla güvenlik doğrulaması eskidi — sayfayı yenileyip tekrar dene.")
        404 -> trf("{0} bulunamadı (404). Ya kaldırılmış ya da henüz açılmamış bir bölüm.", what)
        405 -> trf("{0} bu şekilde açılmıyor (405). Bu bölüm yalnızca form gönderimiyle çalışıyor.", what)
        408 -> tr("Sunucu isteği zaman aşımına uğrattı (408). Tekrar dene.")
        429 -> tr("Çok hızlı istek gönderdin (429). Birkaç saniye bekleyip tekrar dene.")
        in 500..599 -> trf("Delidehşet şu anda hata veriyor (HTTP {0}). Site beta olduğu için bu olabiliyor — birazdan tekrar dene.", code)
        else -> trf("{0} yüklenemedi (HTTP {1}).", what, code)
    }

    /** True for failures that are worth retrying automatically. */
    fun isTransient(t: Throwable): Boolean =
        t is SocketTimeoutException || t is ConnectException ||
            t is InterruptedIOException || t is EOFException

    /**
     * Retries [block] with exponential backoff. Used for reads, never for writes,
     * so a slow beta server cannot double-post anything.
     */
    suspend fun <T> withRetry(
        attempts: Int = 3,
        initialDelayMs: Long = 400,
        block: suspend () -> T,
    ): T {
        var last: Throwable? = null
        var delayMs = initialDelayMs
        repeat(attempts) { attempt ->
            try {
                return block()
            } catch (t: Throwable) {
                last = t
                if (!isTransient(t) || attempt == attempts - 1) throw t
                delay(delayMs)
                delayMs *= 2
            }
        }
        throw last ?: DeliException(tr("Beklenmeyen bir hata oluştu."))
    }
}
