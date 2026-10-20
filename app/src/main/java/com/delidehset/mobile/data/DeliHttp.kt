package com.delidehset.mobile.data

import com.delidehset.mobile.ui.tr
import com.delidehset.mobile.ui.trf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** A response plus its decoded body. */
data class DeliResponse(
    val code: Int,
    val location: String?,
    val contentType: String?,
    val body: String,
) {
    val isSuccess: Boolean get() = code in 200..299
    val isRedirect: Boolean get() = code in 300..399
    val isJson: Boolean get() = contentType?.contains("json", ignoreCase = true) == true
}

class DeliHttp(private val cookies: DeliCookieJar) {

    companion object {
        const val BASE_URL = "https://delidehset.com"

        /**
         * The site is served through Cloudflare, so the browser-shaped prefix stays; the
         * trailing `Hoset/<version>` token is what identifies this client to the site.
         */
        val USER_AGENT: String =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Mobile Safari/537.36 Hoset/${com.delidehset.mobile.BuildConfig.VERSION_NAME}"

        /**
         * ASP.NET Core antiforgery: the page ships a hidden `__RequestVerificationToken`
         * and the site's JS echoes it back in a `RequestVerificationToken` header.
         * We keep the newest token we have seen and send it with every mutation.
         */
        private const val HEADER_TOKEN = "RequestVerificationToken"
        private const val FORM_TOKEN = "__RequestVerificationToken"
    }

    private val antiforgery = AtomicReference<String?>(null)

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookies)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false) // we inspect redirects ourselves to detect "not signed in"
        .retryOnConnectionFailure(true)
        .build()

    fun updateAntiforgery(token: String?) {
        if (!token.isNullOrBlank()) antiforgery.set(token)
    }

    fun currentAntiforgery(): String? = antiforgery.get()

    fun url(path: String): String = if (path.startsWith("http")) path else BASE_URL + path

    fun absolute(path: String?): String? = when {
        path.isNullOrBlank() -> null
        path.startsWith("http") -> path
        else -> BASE_URL + path
    }

    suspend fun get(
        path: String,
        accept: String = "text/html,application/xhtml+xml,*/*",
        referer: String? = null,
    ): DeliResponse = withContext(Dispatchers.IO) { execute(build("GET", path, accept, referer).build()) }

    suspend fun postForm(
        path: String,
        form: List<Pair<String, String>>,
        referer: String? = null,
        accept: String = "text/html,application/json,*/*",
    ): DeliResponse = withContext(Dispatchers.IO) {
        val builder = FormBody.Builder()
        for ((k, v) in form) builder.add(k, v)
        antiforgery.get()?.let { builder.add(FORM_TOKEN, it) }
        execute(build("POST", path, accept, referer).post(builder.build()).build())
    }

    /** Used by the vote endpoints, which take no body. */
    suspend fun postEmpty(
        path: String,
        referer: String? = null,
        accept: String = "application/json,text/html,*/*",
    ): DeliResponse = withContext(Dispatchers.IO) {
        execute(build("POST", path, accept, referer).post(ByteArray(0).toRequestBody(null)).build())
    }

    suspend fun postJson(
        path: String,
        json: String,
        referer: String? = null,
    ): DeliResponse = withContext(Dispatchers.IO) {
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        execute(build("POST", path, "application/json,*/*", referer).post(body).build())
    }

    /**
     * The site's submit form is posted as multipart/form-data
     * (`submitPostMultipart`), because it can carry image uploads.
     */
    suspend fun postMultipart(
        path: String,
        fields: List<Pair<String, String>>,
        referer: String? = null,
    ): DeliResponse = withContext(Dispatchers.IO) {
        val builder = okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
        for ((k, v) in fields) builder.addFormDataPart(k, v)
        antiforgery.get()?.let { builder.addFormDataPart(FORM_TOKEN, it) }
        execute(build("POST", path, "text/html,application/json,*/*", referer).post(builder.build()).build())
    }

    /**
     * Uploads one file the way the site's profile/banner forms do: the picture is sent
     * as a multipart part under its own field name, alongside the antiforgery token.
     */
    suspend fun postFile(
        path: String,
        fieldName: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray,
        referer: String? = null,
    ): DeliResponse = withContext(Dispatchers.IO) {
        val builder = okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
        val part = bytes.toRequestBody(contentType.toMediaType(), 0, bytes.size)
        builder.addFormDataPart(fieldName, fileName, part)
        builder.addFormDataPart("__RequestVerificationToken", antiforgery.get().orEmpty())
        execute(build("POST", path, "text/html,application/json,*/*", referer).post(builder.build()).build())
    }

    private fun build(method: String, path: String, accept: String, referer: String? = null): Request.Builder {
        val full = url(path)
        val b = Request.Builder()
            .url(full)
            .header("User-Agent", USER_AGENT)
            .header("Accept", accept)
            .header("Accept-Language", "tr-TR,tr;q=0.9,en;q=0.8")
            .header("Origin", BASE_URL)
            .header("Referer", referer?.let { url(it) } ?: full)
        antiforgery.get()?.let { b.header(HEADER_TOKEN, it) }
        return b
    }

    private fun execute(request: Request): DeliResponse {
        val response: Response = try {
            client.newCall(request).execute()
        } catch (e: java.io.IOException) {
            throw DeliException(trf("Bağlantı kurulamadı: {0}", e.message ?: tr("ağ hatası")), e)
        }
        response.use { r ->
            val body = runCatching { r.body?.string().orEmpty() }.getOrDefault("")
            return DeliResponse(
                code = r.code,
                location = r.header("Location"),
                contentType = r.header("Content-Type"),
                body = body,
            )
        }
    }
}

class DeliException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Thrown when the server bounced us to /login because the session is gone. */
class NotSignedInException : Exception("Bu işlem için oturum açmalısın.")
