package eu.kanade.tachiyomi.extension.ru.unicomics

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
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable

class UniComics : ParsedHttpSource() {

    override val name = "UniComics"

    override val baseUrl = "https://unicomics.ru"

    override val lang = "ru"

    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36 Edg/100.0.1185.50")
        .add("Referer", baseUrl)

    override fun popularMangaRequest(page: Int): Request =
        GET("$baseUrl/comics/series/page/$page", headers)

    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/comics/online/page/$page", headers)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw UnsupportedOperationException("Поиск только через браузер. Открывается в приложении через «СЕРИИ»(не отдельная глава)")
    override fun searchMangaFromElement(element: Element): SManga = throw UnsupportedOperationException("Not used")
    override fun searchMangaSelector() = throw UnsupportedOperationException("Not used")
    private fun searchMangaByIdRequest(id: String): Request {
        return GET("$baseUrl$PATH_URL$id", headers)
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
        manga.title = element.select(".list_title_en").first().text()
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

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    override fun mangaDetailsRequest(manga: SManga): Request {
        return GET(baseUrl + PATH_URL + manga.url, headers)
    }

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val infoElement = document.select(".block.left.common").first()
        url = document.location().substringAfter(PATH_URL)
        title = infoElement.select("h2").first().text()
        thumbnail_url = infoElement.select("img").first().attr("src")
        description = infoElement.select("p").last()?.text()
        author = infoElement.select("tr:contains(Издательство)").text()
        genre = infoElement.select("tr:contains(Жанр) a").joinToString { it.text() }
    }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val document = client.newCall(GET(baseUrl + PATH_URL + manga.url, headers)).execute().asJsoup()
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
                chapterListParse(client.newCall(chapterPageListRequest(manga, page)).execute(), manga)
            }.reversed()
        )
    }
    private fun chapterListParse(response: Response, manga: SManga): List<SChapter> {
        val document = response.asJsoup()
        return document.select(chapterListSelector()).map { chapterFromElement(it) }
    }
    private fun chapterPageListRequest(manga: SManga, page: Int): Request {
        return GET("$baseUrl$PATH_URL${manga.url}/page/$page", headers)
    }

    override fun chapterListSelector() = "div.right_comics"

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select(".button.online a").first()
        val chapter = SChapter.create()
        element.select(".list_title").first().text().let {
            chapter.name = it
            if (it.contains(" №")) {
                chapter.name = it.substringAfterLast(" ")
                chapter.chapter_number = it.substringAfter(" №").toFloatOrNull() ?: -1f
            }
        }
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        return chapter
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
    }
}
