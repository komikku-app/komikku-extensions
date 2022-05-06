package eu.kanade.tachiyomi.extension.ru.unicomics

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.util.concurrent.TimeUnit

class UniComics : ParsedHttpSource() {

    override val name = "UniComics"

    private val baseDefaultUrl = "https://unicomics.ru"
    override var baseUrl = baseDefaultUrl

    override val lang = "ru"

    override val supportsLatest = true

    override val client: OkHttpClient = network.client.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor(RateLimitInterceptor(3))
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36 Edg/100.0.1185.50")
        .add("Referer", baseDefaultUrl)

    override fun popularMangaRequest(page: Int): Request =
        GET("$baseDefaultUrl/comics/series/page/$page", headers)

    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseDefaultUrl/comics/online/page/$page", headers)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        GET("https://yandex.ru/search/site/?frame=1&lr=172&searchid=1959358&topdoc=xdm_e=$baseDefaultUrl&xdm_c=default5044&xdm_p=1&v=2.0&web=0&text=$query&p=$page", headers)

    override fun searchMangaSelector() =
        ".b-serp-item__content:has(.b-serp-url__item:contains(/comics/):not(:contains(/comics/events)):not(:contains(/comics/publishers)):not(:contains(/page/))):has(.b-serp-item__title-link:not(:contains(Комиксы читать онлайн бесплатно)))"

    override fun searchMangaNextPageSelector() = ".b-pager__next"
    override fun searchMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("a.b-serp-item__title-link").first().let {
                val originUrl = it.attr("href")
                val urlString =
                    "/characters$|/creators$".toRegex().replace(
                        "/page$".toRegex().replace(
                            "/[0-9]+/?$".toRegex().replace(
                                originUrl.substringAfter(PATH_URL).substringAfter(PATH_online).substringAfter(PATH_issue), ""
                            ),
                            ""
                        ),
                        ""
                    )
                val issueNumber = "-[0-9]+/?$".toRegex()
                setUrlWithoutDomain(
                    if (issueNumber.containsMatchIn(urlString) && (originUrl.contains(PATH_online) || originUrl.contains(PATH_issue)))
                        issueNumber.replace(urlString, "")
                    else urlString
                )

                title = it.text().substringBefore(" (").substringBefore(" №")
            }
        }
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        if (document.select(".CheckboxCaptcha").isNotEmpty() && baseUrl == baseDefaultUrl) {
            baseUrl = document.location()
            throw Exception("Пройдите капчу в WebView(слишком много запросов)")
        } else if (baseUrl != baseDefaultUrl) {
            baseUrl = baseDefaultUrl
        }

        var hasNextPage = false

        val mangas = document.select(searchMangaSelector()).map { element ->
            searchMangaFromElement(element)
        }
        val nextSearchPage = document.select(searchMangaNextPageSelector())
        if (nextSearchPage.isNotEmpty()) {
            hasNextPage = true
        }
        return MangasPage(mangas.distinctBy { it.url }, hasNextPage)
    }

    private fun searchMangaByIdRequest(id: String): Request {
        return GET("$baseDefaultUrl$PATH_URL$id", headers)
    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return if (query.startsWith(PREFIX_SLUG_SEARCH)) {
            val realQuery = query.removePrefix(PREFIX_SLUG_SEARCH)
            client.newCall(searchMangaByIdRequest(realQuery))
                .asObservableSuccess()
                .map { response ->
                    val details = mangaDetailsParse(response)
                    details.url = realQuery
                    MangasPage(listOf(details), false)
                }
        } else {
            client.newCall(searchMangaRequest(page, query, filters))
                .asObservableSuccess()
                .map { response ->
                    searchMangaParse(response)
                }
        }
    }

    override fun popularMangaSelector() = "div.list_comics"

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.thumbnail_url = element.select(".left_comics img").first().attr("src").replace(".jpg", "_big.jpg")
        element.select("a").first().let {
            manga.setUrlWithoutDomain(it.attr("href").substringAfter(PATH_URL))
        }
        manga.title = element.select(".list_title").first().text()
        return manga
    }
    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select(popularMangaSelector()).map { element ->
            popularMangaFromElement(element)
        }
        return MangasPage(mangas, true)
    }

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)
    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    override fun popularMangaNextPageSelector(): String? = null

    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    override fun mangaDetailsRequest(manga: SManga): Request {
        return GET(baseDefaultUrl + PATH_URL + manga.url, headers)
    }

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val infoElement = document.select(".block.left.common").first()
        title = infoElement.select("h1").first().text()
        thumbnail_url = infoElement.select("img").first().attr("src")
        description = infoElement.select("p").last()?.text()
        author = infoElement.select("tr:contains(Издательство)").text()
        genre = infoElement.select("tr:contains(Жанр) a").joinToString { it.text() }
    }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val document = client.newCall(GET(baseDefaultUrl + PATH_URL + manga.url, headers)).execute().asJsoup()
        val pages = mutableListOf(1)
        val dataStrArray = document.toString()
            .substringAfter("new Paginator(")
            .substringBefore(");</script>")
            .split(", ")
        if (dataStrArray[1].toInt() > 1) {
            pages += (2..dataStrArray[1].toInt()).toList()
        }
        return Observable.just(
            pages.flatMap { page ->
                chapterListParse(client.newCall(chapterPageListRequest(manga, page)).execute())
            }.reversed()
        )
    }

    private fun chapterPageListRequest(manga: SManga, page: Int): Request {
        return GET("$baseDefaultUrl$PATH_URL${manga.url}/page/$page", headers)
    }

    override fun chapterListSelector() = "div.right_comics"

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select(".button.online a").first()
        val chapter = SChapter.create()
        element.select(".list_title").first().text().let {
            if (it.contains(" №")) {
                chapter.name = it.substringAfterLast(" ")
                chapter.chapter_number = it.substringAfter(" №").toFloatOrNull() ?: -1f
            } else {
                chapter.name = "$it Сингл"
                chapter.chapter_number = 0f
            }
        }
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        return chapter
    }

    override fun pageListRequest(chapter: SChapter): Request {
        return GET(baseDefaultUrl + chapter.url, headers)
    }

    override fun pageListParse(document: Document): List<Page> {
        val dataStrArray = document.toString()
            .substringAfter("new Paginator(")
            .substringBefore(");</script>")
            .split(", ")
        return (1..dataStrArray[1].toInt()).mapIndexed { i, page ->
            Page(i, document.location() + "/$page")
        }
    }
    override fun imageUrlParse(document: Document): String {
        return document.select("#b_image").attr("src")
    }

    companion object {
        const val PREFIX_SLUG_SEARCH = "slug:"
        private const val PATH_URL = "/comics/series/"
        private const val PATH_online = "/comics/online/"
        private const val PATH_issue = "/comics/issue/"
    }
}
