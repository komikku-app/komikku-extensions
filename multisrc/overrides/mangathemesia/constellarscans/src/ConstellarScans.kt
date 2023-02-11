package eu.kanade.tachiyomi.extension.en.constellarscans

import android.annotation.SuppressLint
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import eu.kanade.tachiyomi.lib.dataimage.DataImageInterceptor
import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.Request
import org.jsoup.nodes.Document
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.CountDownLatch

class ConstellarScans : MangaThemesia("Constellar Scans", "https://constellarscans.com", "en") {

    override val client = super.client.newBuilder()
        .addInterceptor(DataImageInterceptor())
        .rateLimit(1, 3)
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Referer", "$baseUrl/")
        .add("Accept-Language", "en-US,en;q=0.9")
        .add("DNT", "1")
        .add("User-Agent", mobileUserAgent)
        .add("Upgrade-Insecure-Requests", "1")

    override val seriesStatusSelector = ".status"

    private val mobileUserAgent by lazy {
        val req = GET(UA_DB_URL)
        val data = client.newCall(req).execute().body.use {
            json.parseToJsonElement(it.string()).jsonArray
        }.mapNotNull {
            it.jsonObject["user-agent"]?.jsonPrimitive?.content?.takeIf { ua ->
                ua.startsWith("Mozilla/5.0") &&
                    (
                        ua.contains("iPhone") &&
                            (ua.contains("FxiOS") || ua.contains("CriOS")) ||
                            ua.contains("Android") &&
                            (ua.contains("EdgA") || ua.contains("Chrome") || ua.contains("Firefox"))
                        )
            }
        }
        data.random()
    }

    override fun pageListRequest(chapter: SChapter): Request =
        super.pageListRequest(chapter).newBuilder()
            .header(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
            )
            .header("Sec-Fetch-Site", "same-origin")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-User", "?1")
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()

    internal class JsObject(val imageList: MutableList<String> = mutableListOf()) {
        @JavascriptInterface
        fun passSingleImage(url: String) {
            Log.d("constellarscans", "received image: $url")
            imageList.add(url)
        }
    }

    private fun randomString(length: Int = 10): String {
        val charPool = ('a'..'z') + ('A'..'Z')
        return List(length) { charPool.random() }.joinToString("")
    }

    private val funkyScript by lazy {
        client.newCall(GET(FUNKY_SCRIPT_URL)).execute().body.string()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun pageListParse(document: Document): List<Page> {
        val interfaceName = randomString()
        document.body().prepend("<script>${funkyScript.replace("\$interfaceName", interfaceName)}</script>")

        val handler = Handler(Looper.getMainLooper())
        val latch = CountDownLatch(1)
        val jsInterface = JsObject()
        var webView: WebView? = null
        handler.post {
            val webview = WebView(Injekt.get<Application>())
            webView = webview
            webview.settings.javaScriptEnabled = true
            webview.settings.domStorageEnabled = true
            webview.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            webview.settings.useWideViewPort = false
            webview.settings.loadWithOverviewMode = false
            webview.settings.userAgentString = mobileUserAgent
            webview.addJavascriptInterface(jsInterface, interfaceName)

            webview.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress == 100) {
                        latch.countDown()
                    }
                }

                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    if (consoleMessage == null) { return false }
                    val logContent = "wv: ${consoleMessage.message()} ${consoleMessage.sourceId()}, line ${consoleMessage.lineNumber()}"
                    when (consoleMessage.messageLevel()) {
                        ConsoleMessage.MessageLevel.DEBUG -> Log.d("constellarscans", logContent)
                        ConsoleMessage.MessageLevel.ERROR -> Log.e("constellarscans", logContent)
                        ConsoleMessage.MessageLevel.LOG -> Log.i("constellarscans", logContent)
                        ConsoleMessage.MessageLevel.TIP -> Log.i("constellarscans", logContent)
                        ConsoleMessage.MessageLevel.WARNING -> Log.w("constellarscans", logContent)
                        else -> Log.d("constellarscans", logContent)
                    }

                    return true
                }
            }
            Log.d("constellarscans", "starting webview shenanigans")
            webview.loadDataWithBaseURL(baseUrl, document.toString(), "text/html", "UTF-8", null)
        }

        latch.await()
        handler.post { webView?.destroy() }
        return jsInterface.imageList.mapIndexed { idx, it -> Page(idx, imageUrl = it) }
    }

    override fun imageRequest(page: Page): Request = super.imageRequest(page).newBuilder()
        .header("Accept", "image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
        .header("Sec-Fetch-Dest", "image")
        .header("Sec-Fetch-Mode", "no-cors")
        .header("Sec-Fetch-Site", "same-origin")
        .build()

    companion object {
        const val UA_DB_URL =
            "https://cdn.jsdelivr.net/gh/mimmi20/browscap-helper@30a83c095688f40b9eaca0165a479c661e5a7fbe/tests/0002999.json"
        val FUNKY_SCRIPT_URL = "https://cdn.jsdelivr.net/npm/@beerpsi/funky-script@latest/constellar.js"
    }
}
