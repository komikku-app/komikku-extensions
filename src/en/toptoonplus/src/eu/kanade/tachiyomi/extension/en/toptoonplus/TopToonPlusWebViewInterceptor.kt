package eu.kanade.tachiyomi.extension.en.toptoonplus

import android.annotation.SuppressLint
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import eu.kanade.tachiyomi.network.GET
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

abstract class TopToonPlusWebViewInterceptor : Interceptor {

    protected abstract val baseUrl: String

    protected abstract val headers: Headers

    protected open val executeJavascript: Boolean = true

    protected val windowKey: String by lazy {
        UUID.randomUUID().toString().replace("-", "")
    }

    protected val handler by lazy { Handler(Looper.getMainLooper()) }

    protected class JsInterface(private val latch: CountDownLatch, var payload: String = "") {
        @JavascriptInterface
        fun passPayload(passedPayload: String) {
            payload = passedPayload
            latch.countDown()
        }
    }

    abstract override fun intercept(chain: Interceptor.Chain): Response

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    protected fun proceedWithWebView(websiteRequest: Request): String? {
        val latch = CountDownLatch(1)
        var webView: WebView? = null

        val requestUrl = websiteRequest.url.toString()
        val headers = websiteRequest.headers.toMultimap()
            .mapValues { it.value.getOrNull(0) ?: "" }
            .toMutableMap()
        val userAgent = headers["User-Agent"]
        val jsInterface = JsInterface(latch)

        handler.post {
            val webview = WebView(Injekt.get<Application>())
            webView = webview

            with(webview.settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = false
                loadWithOverviewMode = false
                userAgentString = userAgent.orEmpty().ifEmpty { userAgentString }
            }

            if (executeJavascript) {
                webview.addJavascriptInterface(jsInterface, windowKey)
            }

            webview.webViewClient = createWebViewClient(jsInterface)

            webview.loadUrl(requestUrl, headers)
        }

        latch.await(TIMEOUT_SEC, TimeUnit.SECONDS)

        handler.postDelayed({ webView?.destroy() }, DELAY_MILLIS)

        if (jsInterface.payload.isBlank()) {
            return null
        }

        return jsInterface.payload
    }

    protected abstract fun createWebViewClient(jsInterface: JsInterface): WebViewClient

    companion object {
        private const val TIMEOUT_SEC: Long = 20
        private const val DELAY_MILLIS: Long = 10 * 1000
    }
}

/**
 * WebView interceptor to get the access token from the user.
 * It was created because the website started to use reCAPTCHA.
 */
class TopToonPlusTokenInterceptor(
    override val baseUrl: String,
    override val headers: Headers
) : TopToonPlusWebViewInterceptor() {

    private var token: String? = null

    @Synchronized
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (!request.url.toString().startsWith(TopToonPlus.API_URL)) {
            return chain.proceed(request)
        }

        if (token != null) {
            request = request.newBuilder()
                .header("Token", token!!)
                .build()

            val response = chain.proceed(request)

            // The API throws 463 if the token is invalid.
            if (response.code != 463) {
                return response
            }

            token = null
            request = request.newBuilder()
                .removeHeader("Token")
                .build()

            response.close()
        }

        try {
            val websiteRequest = GET(baseUrl, headers)
            token = proceedWithWebView(websiteRequest)
        } catch (e: Exception) {
            throw IOException(e.message)
        }

        if (token != null) {
            request = request.newBuilder()
                .header("Token", token!!)
                .build()
        }

        return chain.proceed(request)
    }

    override fun createWebViewClient(jsInterface: JsInterface): WebViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            view.evaluateJavascript(createScript()) {}
        }
    }

    private fun createScript(): String = """
        (function () {
            var database = JSON.parse(localStorage.getItem("persist:topco"));

            if (!database) {
                window["$windowKey"].passPayload("");
                return;
            }

            var userDatabase = JSON.parse(database.user);

            if (!userDatabase) {
                window["$windowKey"].passPayload("");
                return;
            }

            var accessToken = userDatabase.accessToken;
            window["$windowKey"].passPayload(accessToken || "");
        })();
    """.trimIndent()
}

/**
 * WebView interceptor to get the viewer token for the chapter.
 * It was created because the website started to use reCAPTCHA.
 */
class TopToonPlusViewerInterceptor(
    override val baseUrl: String,
    override val headers: Headers
) : TopToonPlusWebViewInterceptor() {

    override val executeJavascript: Boolean = false

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (!request.url.toString().startsWith(TopToonPlus.API_URL)) {
            return chain.proceed(request)
        }

        if (request.url.pathSegments.joinToString("/") != VIEWER_ENDPOINT) {
            return chain.proceed(request)
        }

        val comicId = request.url.queryParameter("comicId")!!
        val episodeId = request.url.queryParameter("episodeId")!!
        val chapterRequest = GET("$baseUrl/comic/$comicId/$episodeId", headers)
        val urlWithToken: String

        try {
            urlWithToken = proceedWithWebView(chapterRequest).orEmpty()
                .ifEmpty { request.url.toString() }
        } catch (e: Exception) {
            throw IOException(e.message)
        }

        request = request.newBuilder()
            .url(urlWithToken)
            .build()

        return chain.proceed(request)
    }

    override fun createWebViewClient(jsInterface: JsInterface): WebViewClient = object : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest
        ): WebResourceResponse? {
            if (!request.url.toString().contains(VIEWER_ENDPOINT)) {
                return null
            }

            val badResponse = buildJsonObject {
                put("action", "unusable comic")
                put("message", "not allowed")
                put("uuid", UUID.randomUUID().toString())
            }

            jsInterface.passPayload(request.url.toString())

            return WebResourceResponse(
                "application/json",
                "utf-8",
                badResponse.toString().byteInputStream()
            )
        }
    }

    companion object {
        private const val VIEWER_ENDPOINT = "api/v1/page/viewer"
    }
}
