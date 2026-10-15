package com.delidehset.mobile.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import com.delidehset.mobile.ui.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.delidehset.mobile.data.DeliCookieJar
import com.delidehset.mobile.data.DeliHttp
import com.delidehset.mobile.ui.components.MutedText
import com.delidehset.mobile.ui.theme.DeliColors
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Sign-in and sign-up sheet.
 *
 * Everything else in the app is native, but registration cannot be: delidehset.com
 * guards `/signup` with Cloudflare Turnstile, which is a browser challenge. Instead of
 * throwing the user out to an external browser, this screen hosts the real page inside
 * the app and — the moment a session cookie shows up — hands it to the native HTTP
 * client so every other screen keeps working natively.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AuthWebScreen(
    mode: String, // "login" | "signup"
    cookies: DeliCookieJar,
    contentPadding: PaddingValues,
    onAuthenticated: () -> Unit,
    onCancel: () -> Unit,
) {
    val startUrl = if (mode == "signup") "/signup" else "/login?redirect=%2F"
    var loading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    fun handOff(view: WebView?) {
        val header = CookieManager.getInstance().getCookie(DeliHttp.BASE_URL)
        DeliHttp.BASE_URL.toHttpUrlOrNull()?.let { cookies.importRawHeader(it, header) }
        view?.stopLoading()
        onAuthenticated()
    }

    Column(modifier = Modifier.fillMaxSize().background(DeliColors.Background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeliColors.Surface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (mode == "signup") "Kayıt ol" else "Oturum aç",
                    color = DeliColors.OnBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                MutedText(
                    if (mode == "signup") {
                        "Kayıt, Cloudflare Turnstile istediği için site sayfası burada açılıyor."
                    } else {
                        "Giriş yaptığın an oturum uygulamaya aktarılır, gerisi native devam eder."
                    }
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onCancel) {
                Text("Kapat", color = DeliColors.Blue, fontSize = 14.sp)
            }
        }

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = DeliColors.Primary,
                trackColor = DeliColors.Border,
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.mediaPlaybackRequiresUserGesture = false

                        // Turnstile runs on challenges.cloudflare.com, so third-party
                        // cookies must be allowed for the challenge to complete.
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loading = true
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean = false

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                if (url == null) return
                                if (looksSignedIn(url)) handOff(view)
                            }
                        }
                        loadUrl(DeliHttp.BASE_URL + startUrl)
                        webView = this
                    }
                },
                update = { webView = it },
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Only the view is torn down; the WebView's cookies are intentionally kept.
            webView?.stopLoading()
            (webView?.parent as? android.view.ViewGroup)?.removeView(webView)
            webView?.destroy()
        }
    }
}

/** The site lands a signed-in visitor on `/` rather than back on the auth form. */
private fun looksSignedIn(url: String): Boolean {
    val path = runCatching { Uri.parse(url).path.orEmpty() }.getOrDefault("")
    if (path.contains("signup") || path.contains("login")) return false
    val cookie = CookieManager.getInstance().getCookie(DeliHttp.BASE_URL).orEmpty()
    return cookie.contains("AspNetCore.Cookies", true) ||
        cookie.contains("Identity.Application", true)
}
