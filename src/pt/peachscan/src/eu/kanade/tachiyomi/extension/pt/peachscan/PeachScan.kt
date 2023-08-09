package eu.kanade.tachiyomi.extension.pt.peachscan

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class PeachScan : ParsedHttpSource() {

    override val name = "Peach Scan"

    override val baseUrl = "https://peachscan.com"

    override val lang = "pt-BR"

    override val supportsLatest = true

    // Migrated from Madara to a custom CMS.
    override val versionId = 2

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()

    private val json: Json by injectLazy()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request = GET(baseUrl, headers)

    override fun popularMangaSelector(): String = "section.populares a.populares__links"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("p.nome__obra")!!.text()
        thumbnail_url = element.selectFirst("img.populares__img")!!.absUrl("src")
        setUrlWithoutDomain(element.attr("href"))
    }

    override fun popularMangaNextPageSelector(): String? = null

    override fun latestUpdatesRequest(page: Int): Request = GET(baseUrl, headers)

    override fun latestUpdatesSelector() = "section.all__comics div.comic"

    override fun latestUpdatesFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("h2.titulo__comic")!!.text()
        thumbnail_url = element.selectFirst("img.comic__img")!!.absUrl("src")
        setUrlWithoutDomain(element.selectFirst("a.box-image")!!.attr("href"))
    }

    override fun latestUpdatesNextPageSelector(): String? = null

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/auto-complete".toHttpUrl().newBuilder()
            .addQueryParameter("term", query)
            .toString()

        return GET(url, headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val partialHtml = response.parseAs<List<AutoCompleteDto>>()
            .joinToString("") { it.html }

        val document = Jsoup.parseBodyFragment(partialHtml, baseUrl)
        val results = document.select(searchMangaSelector())
            .map(::searchMangaFromElement)

        return MangasPage(results, hasNextPage = false)
    }

    override fun searchMangaSelector() = "a.autocomplete-link"

    override fun searchMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("span.autocomplete-text")!!.text()
        thumbnail_url = element.selectFirst("img.autocomplete-img")!!.absUrl("src")
        setUrlWithoutDomain(element.attr("href"))
    }

    override fun searchMangaNextPageSelector(): String? = null

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val descriptionEl = document.selectFirst("section.desc__comics")!!

        title = descriptionEl.selectFirst("h1.desc__titulo__comic")!!.text()
        author = descriptionEl.selectFirst("div:contains(Autor) + span")!!.text()
        genre = descriptionEl.select("div:contains(Gênero) + span a")
            .joinToString { it.text() }
        status = descriptionEl.selectFirst("div:contains(Status) + span")!!.text().toStatus()
        description = document.selectFirst("p.sumario__sinopse__texto")!!.text()
        thumbnail_url = descriptionEl.selectFirst("img.sumario__img")!!.absUrl("src")
    }

    override fun chapterListSelector() = "ul.capitulos__lista a.link__capitulos"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.selectFirst("span.numero__capitulo")!!.text()
        scanlator = name
        date_upload = element.selectFirst("span.data__lançamento")!!.text().toDate()
        setUrlWithoutDomain(element.attr("href"))
    }

    override fun pageListRequest(chapter: SChapter): Request {
        val newHeaders = headersBuilder()
            .set("Referer", baseUrl + chapter.url.substringBeforeLast("/"))
            .build()

        return GET(baseUrl + chapter.url, newHeaders)
    }

    override fun pageListParse(document: Document): List<Page> {
        return document.select("div.capitulo img")
            .mapIndexed { i, element ->
                Page(i, document.location(), element.absUrl("src"))
            }
    }

    override fun imageUrlParse(document: Document) = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .set("Accept", ACCEPT_IMAGE)
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private fun String.toDate(): Long {
        return runCatching { DATE_FORMATTER.parse(trim())?.time }
            .getOrNull() ?: 0L
    }

    private fun String.toStatus() = when (this) {
        "Em Lançamento" -> SManga.ONGOING
        "Completo", "Concluído", "Finalizado" -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    private inline fun <reified T> Response.parseAs(): T = use {
        json.decodeFromString(it.body.string())
    }

    @Serializable
    private data class AutoCompleteDto(val html: String)

    companion object {
        private const val ACCEPT_IMAGE = "image/webp,image/apng,image/*,*/*;q=0.8"

        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("dd 'de' MMMMM 'de' yyyy 'às' HH:mm", Locale("pt", "BR"))
        }
    }
}
