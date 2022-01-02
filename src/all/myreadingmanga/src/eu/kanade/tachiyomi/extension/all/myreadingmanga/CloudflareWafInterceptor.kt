package eu.kanade.tachiyomi.extension.all.myreadingmanga

import android.annotation.SuppressLint
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// TODO: remove after new stable release
class CloudflareWafInterceptor(private val cookieDomain: String) : Interceptor {
    private val context = Injekt.get<Application>()
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val cookieManager by lazy { CookieManager.getInstance() }

    private val initWebView by lazy {
        WebSettings.getDefaultUserAgent(context)
    }

    @Synchronized
    override fun intercept(chain: Interceptor.Chain): Response {
        initWebView

        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code != 403 || response.header("Server") !in SERVER_CHECK) {
            return response
        }

        try {
            response.close()

            // Remove all cookies and retry the Cloudflare WAF in a webview.
            // This is the key.
            cookieManager.setCookie(request.url.toString(), "__cf_bm=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; domain=$cookieDomain")
            cookieManager.flush()
            resolveWithWebView(request)

            return chain.proceed(request)
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithWebView(request: Request) {
        val latch = CountDownLatch(1)

        var webView: WebView? = null

        val origRequestUrl = request.url.toString()
        val headers = request.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }.toMutableMap()
        headers.remove("cookie")

        handler.post {
            val webview = WebView(context)
            webView = webview
            with(webview.settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = false
                loadWithOverviewMode = false
                userAgentString = request.header("User-Agent")
            }

            webview.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    latch.countDown()
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    latch.countDown()
                }
            }

            webView?.loadUrl(origRequestUrl, headers)
        }

        latch.await(TIMEOUT_SEC, TimeUnit.SECONDS)

        handler.post {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    companion object {
        private val SERVER_CHECK = arrayOf("cloudflare-nginx", "cloudflare")
        const val TIMEOUT_SEC: Long = 10
    }
}
