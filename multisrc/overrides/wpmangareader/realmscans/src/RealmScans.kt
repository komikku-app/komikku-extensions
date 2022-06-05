package eu.kanade.tachiyomi.extension.en.realmscans

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

class RealmScans : WPMangaReader(
    "Realm Scans",
    "https://realmscans.com",
    "en",
    "/series"
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 1, TimeUnit.SECONDS)
        .build()

    override fun pageListParse(document: Document): List<Page> {
        val pages = document.select(pageSelector)
            .mapIndexed { i, img ->
                val url = img.attr("data-wpfc-original-src")
                    .ifEmpty { img.attr("abs:src") }

                Page(i, "", url)
            }
            .toMutableList()

        countViews(document)

        if (pages.isNotEmpty()) {
            return pages
        }

        val docString = document.toString()
        val imageListRegex = "\\\"images.*?:.*?(\\[.*?\\])".toRegex()
        val imageListJson = imageListRegex.find(docString)!!.destructured.toList()[0]

        val imageList = json.parseToJsonElement(imageListJson).jsonArray

        pages += imageList.mapIndexed { i, jsonEl ->
            Page(i, "", jsonEl.jsonPrimitive.content)
        }

        return pages
    }
}
