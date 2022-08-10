package eu.kanade.tachiyomi.extension.en.koushoku

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.Jsoup
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException
import java.util.concurrent.CountDownLatch

class KoushokuWebViewInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.headers("Content-Type").any { it.contains("text/html") }) {
            val responseBody = response.peekBody(1 * 1024 * 1024).string()
            if (response.code == 403) {
                val document = Jsoup.parse(responseBody)
                if (document.selectFirst("h1")?.text()?.contains(Regex("banned$")) == true) {
                    throw IOException("You have been banned. Check WebView for details.")
                }
            }

            if (response.networkResponse != null) {
                try {
                    proceedWithWebView(response, responseBody)
                } catch (e: Exception) {
                    throw IOException(e)
                }
            }
        }

        return response
    }

    private fun proceedWithWebView(response: Response, responseBody: String) {
        val latch = CountDownLatch(1)
        val handler = Handler(Looper.getMainLooper())

        handler.post {
            val webView = WebView(Injekt.get<Application>())
            with(webView.settings) {
                loadsImagesAutomatically = false
                userAgentString = response.request.header("User-Agent")
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    webView.stopLoading()
                    webView.destroy()
                    latch.countDown()
                }
            }

            webView.loadDataWithBaseURL(
                response.request.url.toString(),
                responseBody,
                "text/html",
                "utf-8",
                null
            )
        }

        latch.await()
    }
}
