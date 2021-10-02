package eu.kanade.tachiyomi.extension.pt.mangayabu

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class MangaYabu : ParsedHttpSource() {

    // Hardcode the id because the language wasn't specific.
    override val id: Long = 7152688036023311164

    override val name = "MangaYabu!"

    override val baseUrl = "https://mangayabu.top"

    override val lang = "pt-BR"

    override val supportsLatest = true

    override val client: OkHttpClient = network.client.newBuilder()
        .connectTimeout(2, TimeUnit.MINUTES)
        .readTimeout(2, TimeUnit.MINUTES)
        .writeTimeout(2, TimeUnit.MINUTES)
        .addInterceptor(RateLimitInterceptor(1, 3, TimeUnit.SECONDS))
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Origin", baseUrl)
        .add("Referer", baseUrl)

    override fun popularMangaRequest(page: Int): Request = GET(baseUrl, headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val result = super.popularMangaParse(response)

        if (result.mangas.isEmpty()) {
            throw Exception(BLOCKING_MESSAGE)
        }

        return result
    }

    override fun popularMangaSelector(): String = "#main div.row:contains(Populares) div.carousel div.card > a"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        val tooltip = element.select("div.card-image.mango-hover").first()!!

        title = Jsoup.parse(tooltip.attr("data-tooltip")).select("span b").first()!!.text()
        thumbnail_url = element.selectFirst("img")!!.imgAttr()
        setUrlWithoutDomain(element.attr("href"))
    }

    override fun popularMangaNextPageSelector(): String? = null

    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> {
        return super.fetchLatestUpdates(page)
            .map { MangasPage(it.mangas.distinctBy { m -> m.url }, it.hasNextPage) }
    }

    override fun latestUpdatesRequest(page: Int): Request = GET(baseUrl, headers)

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = super.latestUpdatesParse(response)

        if (result.mangas.isEmpty()) {
            throw Exception(BLOCKING_MESSAGE)
        }

        return result
    }

    override fun latestUpdatesSelector() = "#main div.row:contains(Lançamentos) div.card"

    override fun latestUpdatesFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("div.card-content h4").first()!!.text().withoutFlags()
        thumbnail_url = element.selectFirst("div.card-image img")!!.imgAttr()
        url = mapChapterToMangaUrl(element.select("div.card-image > a").first()!!.attr("href"))
    }

    override fun latestUpdatesNextPageSelector(): String? = null

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val searchUrl = baseUrl.toHttpUrl().newBuilder()
            .addQueryParameter("s", query)
            .toString()

        return POST(searchUrl, headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val result = super.searchMangaParse(response)

        if (result.mangas.isEmpty()) {
            throw Exception(BLOCKING_MESSAGE)
        }

        return result
    }

    override fun searchMangaSelector() = "#main div.row:contains(Resultados) div.card"

    override fun searchMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("div.card-content h4")!!.text()
        thumbnail_url = element.selectFirst("div.card-image img")!!.imgAttr()
        setUrlWithoutDomain(element.selectFirst("a")!!.attr("abs:href"))
    }

    override fun searchMangaNextPageSelector(): String? = null

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val infoElement = document.select("div.manga-column")

        title = document.select("div.manga-info > h1").first()!!.text()
        status = infoElement.select("div.manga-column:contains(Status:)").first()!!
            .textWithoutLabel()
            .toStatus()
        genre = infoElement.select("div.manga-column:contains(Gêneros:)").first()!!
            .textWithoutLabel()
        description = document.select("div.manga-info").first()!!.text()
            .substringAfter(title)
            .trim()
        thumbnail_url = document.selectFirst("div.manga-index div.mango-hover img")!!.imgAttr()
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapters = super.chapterListParse(response)

        if (chapters.isEmpty()) {
            throw Exception(BLOCKING_MESSAGE)
        }

        return chapters
    }

    override fun chapterListSelector() = "div.manga-info:contains(Capítulos) div.manga-chapters div.single-chapter"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.select("a").first()!!.text().substringAfter("–").trim()
        date_upload = element.select("small")!!.text().toDate()
        setUrlWithoutDomain(element.select("a").first()!!.attr("href"))
    }

    override fun pageListParse(document: Document): List<Page> {
        val pages = document.select("#main img[loading], #main img[ezimgfmt]")
            .map { it.imgAttr() }
            .distinct()
            .drop(1)
            .mapIndexed { i, imgUrl ->
                Page(i, document.location(), imgUrl)
            }

        if (pages.isEmpty()) {
            throw Exception(BLOCKING_MESSAGE)
        }

        return pages
    }

    override fun imageUrlParse(document: Document) = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_IMAGE)
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    /**
     * Some mangas doesn't use the same slug from the chapter url, and
     * since the site doesn't have a proper popular list yet, we have
     * to deal with some exceptions and map them to the correct
     * slug manually.
     *
     * It's a bad solution, but it's a working one for now.
     */
    private fun mapChapterToMangaUrl(chapterUrl: String): String {
        val chapterSlug = chapterUrl
            .substringBefore("-capitulo")
            .substringAfter("ler/")

        return "/manga/" + (SLUG_EXCEPTIONS[chapterSlug] ?: chapterSlug)
    }

    private fun Element.imgAttr(): String {
        var imageSrc = attr(if (hasAttr("data-ezsrc")) "abs:data-ezsrc" else "abs:src")
            .substringBeforeLast("?")

        if (imageSrc.contains("ezoimgfmt")) {
            imageSrc = "https://" + imageSrc.substringAfter("ezoimgfmt/")
        }

        return imageSrc
    }

    private fun String.toDate(): Long {
        return runCatching { DATE_FORMATTER.parse(this)?.time }
            .getOrNull() ?: 0L
    }

    private fun String.toStatus() = when (this) {
        "Em lançamento" -> SManga.ONGOING
        "Completo" -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    private fun String.withoutFlags(): String = replace(FLAG_REGEX, "").trim()

    private fun Element.textWithoutLabel(): String = text()!!.substringAfter(":").trim()

    companion object {
        private const val ACCEPT_IMAGE = "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36"

        private val FLAG_REGEX = "\\((Pt[-/]br|Scan)\\)".toRegex(RegexOption.IGNORE_CASE)

        private val DATE_FORMATTER by lazy { SimpleDateFormat("dd/MM/yy", Locale.ENGLISH) }

        private val SLUG_EXCEPTIONS = mapOf(
            "the-promised-neverland-yakusoku-no-neverland" to "yakusoku-no-neverland-the-promised-neverland"
        )

        private const val BLOCKING_MESSAGE = "O site está bloqueando o Tachiyomi. " +
            "Migre para outras fontes caso o problema persistir."
    }
}
