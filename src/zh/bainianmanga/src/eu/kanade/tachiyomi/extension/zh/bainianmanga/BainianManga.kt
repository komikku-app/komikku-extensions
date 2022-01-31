package eu.kanade.tachiyomi.extension.zh.bainianmanga

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy

class BainianManga : ParsedHttpSource() {

    override val name = "百年漫画"
    override val baseUrl = "https://www.bnman.net"
    override val lang = "zh"
    override val supportsLatest = true

    override val client: OkHttpClient
        get() = network.client.newBuilder()
            .addNetworkInterceptor(rewriteOctetStream)
            .build()

    // Based on Pufei ext
    private val rewriteOctetStream: Interceptor = Interceptor { chain ->
        val originalResponse: Response = chain.proceed(chain.request())
        if (originalResponse.headers("Content-Type").contains("application/octet-stream") && originalResponse.request.url.toString().contains(".jpg")) {
            val orgBody = originalResponse.body!!.bytes()
            val newBody = orgBody.toResponseBody("image/jpeg".toMediaTypeOrNull())
            originalResponse.newBuilder()
                .body(newBody)
                .build()
        } else originalResponse
    }

    private val json: Json by injectLazy()

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/page/hot/$page.html", headers)
    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/page/new/$page.html", headers)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/search.html?".toHttpUrl().newBuilder()
            .addQueryParameter("keyword", query)
            .addQueryParameter("page", page.toString())
        return GET(url.toString(), headers)
    }

    override fun mangaDetailsRequest(manga: SManga) = GET(baseUrl + manga.url, headers)
    override fun chapterListRequest(manga: SManga) = mangaDetailsRequest(manga)
    override fun pageListRequest(chapter: SChapter) = GET(baseUrl + chapter.url, headers)

    override fun popularMangaSelector() = "ul#list_img > li"
    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun searchMangaSelector() = popularMangaSelector()
    override fun chapterListSelector() = "ul.jslist01 > li"

    override fun searchMangaNextPageSelector() = ".pagination > li:last-child > a"
    override fun popularMangaNextPageSelector() = searchMangaNextPageSelector()
    override fun latestUpdatesNextPageSelector() = searchMangaNextPageSelector()

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", baseUrl)

    override fun popularMangaFromElement(element: Element) = mangaFromElement(element)
    override fun latestUpdatesFromElement(element: Element) = mangaFromElement(element)
    override fun searchMangaFromElement(element: Element) = mangaFromElement(element)
    private fun mangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.select("p").first().text()
        }
        manga.thumbnail_url = element.select("img").attr("src")
        return manga
    }

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select("a")

        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text()
        return chapter
    }

    override fun mangaDetailsParse(document: Document): SManga {
        val infoElement = document.select(".info")

        val manga = SManga.create()
        manga.description = document.select(".mt10").first().text()
        manga.author = infoElement.select("ul > li > span:contains(漫画作者) + p").first().text()
        return manga
    }

    override fun pageListParse(response: Response): List<Page> {
        return json.decodeFromString<List<String>>(
            response.body!!.string()
                .substringAfter("var z_img='")
                .substringBefore("';")
        ).mapIndexed { i, imageUrl ->
            Page(i, "", imageUrl)
        }
    }

    override fun pageListParse(document: Document): List<Page> =
        throw UnsupportedOperationException("Not used.")

    override fun imageUrlParse(document: Document) = ""

    override fun getFilterList() = FilterList()
}
