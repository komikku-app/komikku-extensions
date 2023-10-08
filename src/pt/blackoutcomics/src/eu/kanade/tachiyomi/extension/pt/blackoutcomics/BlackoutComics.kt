package eu.kanade.tachiyomi.extension.pt.blackoutcomics

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class BlackoutComics : ParsedHttpSource() {

    override val name = "Blackout Comics"

    override val baseUrl = "https://blackoutcomics.com"

    override val lang = "pt-BR"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimit(1, 3, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Accept", ACCEPT)
        .add("Accept-Language", ACCEPT_LANGUAGE)
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request = GET(baseUrl, headers)

    override fun popularMangaSelector(): String = "h3:contains(Mais Lidos) ~ div.anime-box a"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("div.anime-blog p")!!.text()
        thumbnail_url = element.selectFirst("img")!!.absUrl("src")
        url = element.attr("href")
    }

    override fun popularMangaNextPageSelector(): String? = null

    override fun latestUpdatesRequest(page: Int) = popularMangaRequest(page)

    override fun latestUpdatesSelector() = "div:contains(Atualizados Recentemente) + div.row div.anime-blog"

    override fun latestUpdatesFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("a p")!!.text()
        thumbnail_url = element.selectFirst("img")!!.absUrl("src")
        url = element.selectFirst("a")!!.attr("href")
    }

    override fun latestUpdatesNextPageSelector(): String? = null

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/comics".toHttpUrl().newBuilder()
            .addQueryParameter("search", query)
            .build()

        return GET(url, headers)
    }

    override fun searchMangaSelector() = "section.anime div.anime-box"

    override fun searchMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("p")!!.text()
        thumbnail_url = element.selectFirst("img")!!.absUrl("src")
        url = element.selectFirst("[onclick]")!!.attr("onclick")
            .substringAfter("'")
            .substringBeforeLast("'")
    }

    override fun searchMangaNextPageSelector(): String? = null

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val infoElement = document.selectFirst("section.video")!!

        title = infoElement.selectFirst("h2")!!.text()
        author = infoElement.selectFirst("div.trailer-content p:contains(Autor:) b")!!.text()
        artist = infoElement.selectFirst("div.trailer-content p:contains(Artista:) b")!!.text()
        genre = infoElement.selectFirst("div.trailer-content p:contains(Genêros:)")!!.ownText()
        status = infoElement.selectFirst("div.trailer-content p:contains(Status:) b")!!.text().toStatus()
        description = infoElement.selectFirst("h3:contains(Descrição) + p")!!.text()
        thumbnail_url = infoElement.selectFirst("img")!!.absUrl("src")
    }

    override fun chapterListSelector() = "section.relese h5:not(:has(img.vip))"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.selectFirst("a")!!.ownText()
        date_upload = element.select("span:last-of-type").text().toDate()
        url = element.selectFirst("a")!!.attr("href")
    }

    override fun pageListRequest(chapter: SChapter): Request {
        val newHeaders = headersBuilder()
            .set("Referer", baseUrl + chapter.url.substringBeforeLast("/ler"))
            .add("Sec-Fetch-Dest", "document")
            .add("Sec-Fetch-Mode", "navigate")
            .add("Sec-Fetch-Site", "same-origin")
            .add("Sec-Fetch-User", "?1")
            .build()

        return GET(baseUrl + chapter.url, newHeaders)
    }

    override fun pageListParse(document: Document): List<Page> {
        return document.select("div.chapter-image canvas")
            .mapIndexed { index, element ->
                Page(index, document.location(), element.absUrl("data-src"))
            }
    }

    override fun imageUrlParse(document: Document) = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .set("Referer", page.url)
            .set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private fun String.toDate(): Long {
        return runCatching { DATE_FORMATTER.parse(trim())?.time }
            .getOrNull() ?: 0L
    }

    private fun String.toStatus() = when (this) {
        "Em Lançamento" -> SManga.ONGOING
        "Completo" -> SManga.COMPLETED
        "Cancelado" -> SManga.CANCELLED
        "Em Espera" -> SManga.ON_HIATUS
        else -> SManga.UNKNOWN
    }

    companion object {
        private const val ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
        private const val ACCEPT_LANGUAGE = "pt-BR,pt;q=0.8,en-US;q=0.5,en;q=0.3"

        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        }
    }
}
