package eu.kanade.tachiyomi.extension.id.komikcast

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

class KomikCast : MangaThemesia(
    "Komik Cast",
    baseUrl = "https://komikcast.me",
    "id",
    mangaUrlDirectory = "/daftar-komik"
) {
    // Formerly "Komik Cast (WP Manga Stream)"
    override val id = 972717448578983812

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .rateLimit(3)
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
        .add("Accept-language", "en-US,en;q=0.9,id;q=0.8")
        .add("Referer", baseUrl)
        .add("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0")

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .set("Referer", baseUrl)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    override fun popularMangaRequest(page: Int) = customPageRequest(page, "orderby", "popular")
    override fun latestUpdatesRequest(page: Int) = customPageRequest(page, "sortby", "update")

    private fun customPageRequest(page: Int, filterKey: String, filterValue: String): Request {
        val pagePath = if (page > 1) "page/$page/" else ""

        return GET("$baseUrl$mangaUrlDirectory/$pagePath?$filterKey=$filterValue", headers)
    }

    override fun searchMangaSelector() = "div.list-update_item"

    override fun searchMangaFromElement(element: Element) = super.searchMangaFromElement(element).apply {
        title = element.selectFirst("h3.title").ownText()
    }

    override val seriesDetailsSelector = "div.komik_info:has(.komik_info-content)"
    override val seriesTitleSelector = "h1.komik_info-content-body-title"
    override val seriesDescriptionSelector = ".komik_info-description-sinopsis"
    override val seriesAltNameSelector = ".komik_info-content-native"
    override val seriesGenreSelector = ".komik_info-content-genre a"
    override val seriesThumbnailSelector = ".komik_info-content-thumbnail img"

    override fun chapterListSelector() = "div.komik_info-chapters li"

    override fun chapterFromElement(element: Element) = super.chapterFromElement(element).apply {
        date_upload = element.selectFirst(".chapter-link-time")?.text().parseChapterDate()
    }

    override fun pageListParse(document: Document): List<Page> {
        var doc = document
        var cssQuery = "div#chapter_body .main-reading-area img.size-full"
        val imageListRegex = Regex("chapterImages = (.*) \\|\\|")
        val imageListMatchResult = imageListRegex.find(document.toString())

        if (imageListMatchResult != null) {
            val imageListJson = imageListMatchResult.destructured.toList()[0]
            val imageList = json.parseToJsonElement(imageListJson).jsonObject

            var imageServer = "cdn"
            if (!imageList.containsKey(imageServer)) imageServer = imageList.keys.first()
            val imageElement = imageList[imageServer]!!.jsonArray.joinToString("")
            doc = Jsoup.parse(json.decodeFromString(imageElement))
            cssQuery = "img.size-full"
        }

        return doc.select(cssQuery)
            .mapIndexed { i, img -> Page(i, "", img.imgAttr()) }
    }

    override val hasProjectPage: Boolean = true
}
