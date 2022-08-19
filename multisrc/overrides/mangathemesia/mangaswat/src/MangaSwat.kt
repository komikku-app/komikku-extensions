package eu.kanade.tachiyomi.extension.ar.mangaswat

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Page
import okhttp3.Headers
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class MangaSwat : MangaThemesia(
    "MangaSwat",
    "https://swatmanga.me",
    "ar",
    dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
) {

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add(
            "Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
        )
        .add("Accept-language", "en-US,en;q=0.9")
        .add("Referer", baseUrl)

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .set("Referer", baseUrl)
            .build()
        return GET(page.imageUrl!!, newHeaders)
    }

    override val seriesArtistSelector = "span:contains(الناشر) i"
    override val seriesAuthorSelector = "span:contains(المؤلف) i"
    override val seriesGenreSelector = "span:contains(التصنيف) a, .mgen a"
    override val seriesTypeSelector = "span:contains(النوع) a"
    override val seriesStatusSelector = "span:contains(الحالة)"

    override val pageSelector = "div#readerarea img"

    override fun pageListParse(document: Document): List<Page> {
        var page: List<Page>? = null
        val scriptContent = document.selectFirst("script:containsData(ts_reader)").data()
        val removeHead = scriptContent.replace("ts_reader.run(", "").replace(");", "")
        val jsonObject = JSONObject(removeHead)
        val sourcesArray = jsonObject.getJSONArray("sources")
        val imagesArray = sourcesArray.getJSONObject(0).getJSONArray("images")
        page = List(imagesArray.length()) { i ->
            Page(i, "", imagesArray[i].toString())
        }

        return page
    }
}
