package eu.kanade.tachiyomi.extension.en.xcalibrscans

import eu.kanade.tachiyomi.extension.en.xcalibrscans.interceptor.MirrorImageInterceptor
import eu.kanade.tachiyomi.extension.en.xcalibrscans.interceptor.SplittedImageInterceptor
import eu.kanade.tachiyomi.extension.en.xcalibrscans.interceptor.prepareMirrorImageForInterceptor
import eu.kanade.tachiyomi.extension.en.xcalibrscans.interceptor.prepareSplittedImageForInterceptor
import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import java.lang.IllegalArgumentException
import java.util.concurrent.TimeUnit

class xCaliBRScans : MangaThemesia("xCaliBR Scans", "https://xcalibrscans.com", "en") {

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .rateLimit(2)
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

        countViews(document)

        // Some sites also loads pages via javascript
        if (htmlPages.isNotEmpty()) { return htmlPages }

        val docString = document.toString()
        val imageListJson = JSON_IMAGE_LIST_REGEX.find(docString)?.destructured?.toList()?.get(0).orEmpty()
        val imageList = try {
            json.parseToJsonElement(imageListJson).jsonArray
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
        val scriptPages = imageList.mapIndexed { i, jsonEl ->
            Page(i, "", jsonEl.jsonPrimitive.content)
        }

        return scriptPages
    }
}
