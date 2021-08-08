package eu.kanade.tachiyomi.extension.fr.kangaryu

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

class Kangaryu : ParsedHttpSource() {

    override val name = "Kangaryu"

    override val baseUrl = "https://kangaryu-team.fr"

    override val lang = "fr"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    // used to be in FoolSlide
    override val versionId = 2

    // Popular

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/manga-list", headers)
    }

    override fun popularMangaSelector() = "div.l-card"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("div.card-image").let {
                setUrlWithoutDomain(it.select("a").attr("href"))
                thumbnail_url = it.select("img").attr("abs:src")
            }

            title = element.select("a.chart-title").text()
        }
    }

    override fun popularMangaNextPageSelector(): String? = null

    // Latest

    override fun latestUpdatesRequest(page: Int): Request {
        return GET(baseUrl, headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        return MangasPage(super.latestUpdatesParse(response).mangas.distinctBy { it.url }, false)
    }

    override fun latestUpdatesSelector() = "div.events"

    override fun latestUpdatesFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("div.manga-chap a").let {
                setUrlWithoutDomain(it.attr("href"))
                title = it.text()
            }
            thumbnail_url = element.select("img").attr("abs:src")
        }
    }

    override fun latestUpdatesNextPageSelector(): String? = null

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/search?query=$query", headers)
    }

    private val json: Json by injectLazy()

    override fun searchMangaParse(response: Response): MangasPage {
        val mangas = json.parseToJsonElement(response.body!!.string()).jsonObject["suggestions"]!!.jsonArray.map {
            val data = it.jsonObject["data"]!!.jsonPrimitive.content
            SManga.create().apply {
                url = "/manga/$data"
                title = it.jsonObject["value"]!!.jsonPrimitive.content
                thumbnail_url = "https://kangaryu-team.fr/uploads/manga/$data/cover/cover_250x350.jpg"
            }
        }
        return MangasPage(mangas, false)
    }

    override fun searchMangaSelector() = throw UnsupportedOperationException("Not used")
    override fun searchMangaFromElement(element: Element): SManga = throw UnsupportedOperationException("Not used")
    override fun searchMangaNextPageSelector() = throw UnsupportedOperationException("Not used")

    // Details

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            thumbnail_url = document.select("div.boxed img").attr("abs:src")
            with(document.select("div.col-sm-12 div.col-sm-8")) {
                status = select("span.label").text().toStatus()
                author = select("dd a[href*=author]").text()
                artist = select("dd a[href*=artist]").text()
                genre = select("dd a[href*=category]").joinToString { it.text() }
            }
            description = document.select("div.col-lg-12 p").text()
        }
    }

    private fun String.toStatus() = when {
        this.contains("En cours", ignoreCase = true) -> SManga.ONGOING
        this.contains("Terminé", ignoreCase = true) -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    // Chapters

    override fun chapterListSelector() = "ul.chapters li:not([data-volume])"

    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            name = element.select("h5.chapter-title-rtl").text()
            setUrlWithoutDomain(element.select("h5 a").attr("href"))
            date_upload = element.select("div.date-chapter-title-rtl").text().toDate()
        }
    }

    private val dateFormat by lazy { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }

    private fun String.toDate(): Long {
        return dateFormat.parse(this)?.time ?: 0
    }

    // Pages

    override fun pageListParse(document: Document): List<Page> {
        return document.select("div#all img").mapIndexed { i, img ->
            Page(i, "", img.attr("abs:data-src"))
        }
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")
}
