package eu.kanade.tachiyomi.extension.en.realmscans

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import java.lang.IllegalArgumentException
import java.util.concurrent.TimeUnit

class RealmScans : MangaThemesia(
    "Realm Scans",
    "https://realmscans.com",
    "en",
    "/series"
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 1, TimeUnit.SECONDS)
        .build()

    override fun pageListParse(document: Document): List<Page> {
        val htmlPages = document.select(pageSelector)
            .mapIndexed { i, img ->
                val url = img.attr("data-wpfc-original-src")
                    .ifEmpty { img.attr("abs:src") }

                Page(i, "", url)
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
