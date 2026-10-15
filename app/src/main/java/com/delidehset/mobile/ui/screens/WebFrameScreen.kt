package com.delidehset.mobile.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
 * Hosts one of the site's own pages inside the app.
 *
 * Only the chat room needs this: delidehset.com renders chat as a sandboxed iframe
 * talking to its own service, so there is nothing to port natively. Everything else the
 * app shows is native. The session cookies are copied from the native client into the
 * WebView first, so the frame opens already signed in.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebFrameScreen(
    path: String,
    title: String,
    cookies: DeliCookieJar,
    onClose: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(DeliColors.Background)) {
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
                        setBackgroundColor(android.graphics.Color.parseColor("#0D1117"))
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        // Hand the native session over before the frame is requested.
                        DeliHttp.BASE_URL.toHttpUrlOrNull()?.let { url ->
                            val header = cookies.exportRawHeader(url)
                            if (header.isNotBlank()) {
                                header.split("; ").forEach { pair ->
                                    CookieManager.getInstance()
                                        .setCookie(DeliHttp.BASE_URL, "$pair; path=/")
                                }
                                CookieManager.getInstance().flush()
                            }
                        }

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
                                // Ensure frame blends into the native app's dark theme
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        document.body.style.backgroundColor = '#0D1117';
                                        var nav = document.querySelector('nav, .navbar, header');
                                        if (nav) nav.style.display = 'none';
                                    })();
                                    """.trimIndent(),
                                    null
                                )
                            }
                        }
                        loadUrl(DeliHttp.BASE_URL + path)
                        webView = this
                    }
                },
                update = { webView = it },
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            (webView?.parent as? android.view.ViewGroup)?.removeView(webView)
            webView?.destroy()
        }
    }
}
