package eu.kanade.tachiyomi.extension.pt.meusmangas

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.mangasar.MangaSar
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

class MeusMangas : MangaSar(
    "Meus Mangás",
    "https://meusmangas.net",
    "pt-BR"
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(::searchIntercept)
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override fun popularMangaSelector() = "ul.sidebar-popular li.popular-treending"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("h4.title").text()
        thumbnail_url = element.selectFirst("div.tumbl img").attr("src")
        setUrlWithoutDomain(element.selectFirst("a").attr("abs:href"))
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val newHeaders = headersBuilder()
            .add("X-Requested-With", "XMLHttpRequest")
            .build()

        val pagePath = if (page > 1) "page/$page" else ""

        return GET("$baseUrl/$pagePath", newHeaders)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangaList = document.select("li.item_news-manga")
            .map(::latestMangaFromElement)

        val hasNextPage = document.select("div.loadmore.morepage").firstOrNull() != null

        return MangasPage(mangaList, hasNextPage)
    }

    private fun latestMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("h3.entry-title a").text()
        thumbnail_url = element.select("img.manga").attr("src")
        setUrlWithoutDomain(element.select("a").first().attr("abs:href"))
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        val infoElement = document.selectFirst("div.box-single:has(div.mangapage)")

        return SManga.create().apply {
            title = infoElement.selectFirst("h1.kw-title").text()
            author = infoElement.selectFirst("div.mdq.author").text().trim()
            description = infoElement.selectFirst("div.sinopse-page").text()
            genre = infoElement.select("div.generos a.widget-btn").joinToString { it.text() }
            status = infoElement.selectFirst("span.mdq").text().toStatus()
            thumbnail_url = infoElement.selectFirst("div.thumb img").attr("abs:src")
        }
    }

    override fun chapterListPaginatedRequest(mangaUrl: String, page: Int): Request {
        val newHeaders = headersBuilder()
            .add("X-Requested-With", "XMLHttpRequest")
            .build()

        return GET("$baseUrl$mangaUrl/page/$page", newHeaders)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        var document = response.asJsoup()

        val chapterList = document.select(chapterListSelector())
            .map(::chapterFromElement)
            .toMutableList()

        val mangaUrl = response.request.url.toString()
            .substringAfter(baseUrl)
            .substringBefore("/page")
        var hasNextPage = document.select(chapterListNextPageSelector())
            .firstOrNull()

        while (hasNextPage != null) {
            val page = hasNextPage.attr("href")
                .substringAfter("page/")
                .toInt()

            val nextRequest = chapterListPaginatedRequest(mangaUrl, page)
            val nextResponse = client.newCall(nextRequest).execute()
            document = nextResponse.asJsoup()

            chapterList += document.select(chapterListSelector())
                .map(::chapterFromElement)

            hasNextPage = document.select(chapterListNextPageSelector())
                .firstOrNull()
        }

        return chapterList
    }

    private fun chapterListSelector() = "ul.list-of-chapters li > a"

    private fun chapterListNextPageSelector() = "ul.content-pagination li.active + li:not(.next) a"

    private fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.select("span.cap-text").text()
        date_upload = element.select("span.chapter-date").text().toDate()
        setUrlWithoutDomain(element.attr("abs:href"))
    }

    private fun String.toStatus(): Int = when (this) {
        "Em andamento" -> SManga.ONGOING
        "Completo" -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }
}
