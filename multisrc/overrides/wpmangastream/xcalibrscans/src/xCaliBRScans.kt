package eu.kanade.tachiyomi.extension.en.xcalibrscans

import eu.kanade.tachiyomi.extension.en.xcalibrscans.interceptor.MirrorImageInterceptor
import eu.kanade.tachiyomi.extension.en.xcalibrscans.interceptor.SplittedImageInterceptor
import eu.kanade.tachiyomi.extension.en.xcalibrscans.interceptor.prepareMirrorImageForInterceptor
import eu.kanade.tachiyomi.extension.en.xcalibrscans.interceptor.prepareSplittedImageForInterceptor
import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.TimeUnit

class xCaliBRScans : WPMangaStream("xCaliBR Scans", "https://xcalibrscans.com", "en") {
    private val json: Json by injectLazy()

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor(RateLimitInterceptor(2))
        .addNetworkInterceptor(SplittedImageInterceptor())
        .addNetworkInterceptor(MirrorImageInterceptor())
        .build()

    override val hasProjectPage = true

    override val pageSelector = "div#readerarea > p, div#readerarea > div"

    override fun pageListParse(document: Document): List<Page> {
        val htmlPages = mutableListOf<Page>()

        document.select(pageSelector)
            .filterNot {
                it.select("img").all { imgEl ->
                    imgEl.attr("abs:src").isNullOrEmpty()
                }
            }
            .map { el ->
                if (el.tagName() == "div") {
                    when {
                        el.hasClass("kage") -> {
                            el.select("img").map { imgEl ->
                                val index = htmlPages.size
                                val imageUrl =
                                    imgEl.attr("abs:src").prepareMirrorImageForInterceptor()
                                htmlPages.add(Page(index, "", imageUrl))
                            }
                        }
                        el.hasClass("row") -> {
                            val index = htmlPages.size
                            val imageUrls = el.select("img").map { imgEl ->
                                imgEl.attr("abs:src")
                            }.prepareSplittedImageForInterceptor()
                            htmlPages.add(Page(index, "", imageUrls))
                        }
                        else -> {
                            val index = htmlPages.size
                            Page(index, "", el.select("img").attr("abs:src"))
                        }
                    }
                } else {
                    val index = htmlPages.size
                    Page(index, "", el.select("img").attr("abs:src"))
                }
            }

        val docString = document.toString()
        val imageListRegex = Regex("\\\"images.*?:.*?(\\[.*?\\])")
        val imageListJson = imageListRegex.find(docString)!!.destructured.toList()[0]

        val imageList = json.parseToJsonElement(imageListJson).jsonArray
        val baseResolver = baseUrl.toHttpUrl()

        val scriptPages = imageList.mapIndexed { i, jsonEl ->
            val imageUrl = jsonEl.jsonPrimitive.content
            Page(i, "", baseResolver.resolve(imageUrl).toString())
        }

        if (htmlPages.size < scriptPages.size) {
            htmlPages += scriptPages
        }

        countViews(document)

        return htmlPages.distinctBy { it.imageUrl }
    }
}
