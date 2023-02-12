package eu.kanade.tachiyomi.extension.en.dilbert

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Dilbert : ParsedHttpSource() {
    override val name = "Dilbert"

    override val baseUrl = "https://dilbert.com"

    override val lang = "en"

    override val supportsLatest = false

    override val client = network.client.newBuilder()
        .rateLimit(3)
        .build()

    override fun fetchPopularManga(page: Int) = (currentYear downTo 1989).map {
        SManga.create().apply {
            url = it.toString()
            title = "$name ($it)"
            author = "Scott Adams"
            thumbnail_url = FAVICON
            status = if (it != currentYear) SManga.COMPLETED else SManga.ONGOING
            description = "$SUMMARY (This entry includes all the chapters published in $it.)"
        }
    }.let { Observable.just(MangasPage(it, false))!! }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList) =
        fetchPopularManga(page).map { mp ->
            mp.copy(mp.mangas.filter { it.title == query })
        }!!

    override fun mangaDetailsRequest(manga: SManga) = GET("", headers)

    override fun fetchMangaDetails(manga: SManga) =
        Observable.just(manga.apply { initialized = true })!!

    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        val date = element.first(".comic-title-date")!!.text()
        url = element.first(".img-comic-link")!!.attr("href")
        name = element.first(".comic-title-name")!!.text().ifBlank { date }
        date_upload = dateFormat.parse(date)?.time ?: 0L
    }

    override fun chapterListSelector() = ".pagination > li:nth-last-child(2) > a"

    override fun fetchChapterList(manga: SManga) =
        List(manga.pages) { chapterListParse(manga, it + 1) }
            .flatMapIndexed { i, ch ->
                ch.map { it.apply { chapter_number = i + 1f } }
            }.let { Observable.just(it)!! }

    override fun fetchPageList(chapter: SChapter) =
        Observable.just(listOf(Page(0, chapter.url)))!!

    override fun imageUrlParse(document: Document): String = document.first(".img-comic")!!.attr("src")

    private fun chapterListRequest(manga: SManga, page: Int) =
        GET("$baseUrl/search_results?year=${manga.url}&page=$page&sort=date_desc", headers)

    private fun chapterListParse(manga: SManga, page: Int) =
        client.newCall(chapterListRequest(manga, page)).execute().run {
            if (!isSuccessful) {
                close()
                throw Exception("HTTP error $code")
            }
            asJsoup().select(".comic-item").map(::chapterFromElement)
        }

    private inline val SManga.pages: Int
        get() = when (url.toInt()) {
            currentYear -> currentDay / 10 + 1
            1989 -> 26
            else -> 37
        }

    private fun Element.first(selector: String) = select(selector).first()

    override fun popularMangaSelector() = ""

    override fun popularMangaNextPageSelector() = ""

    override fun searchMangaSelector() = ""

    override fun searchMangaNextPageSelector() = ""

    override fun latestUpdatesSelector() = ""

    override fun latestUpdatesNextPageSelector() = ""

    override fun popularMangaRequest(page: Int) =
        throw UnsupportedOperationException("This method should not be called!")

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        throw UnsupportedOperationException("This method should not be called!")

    override fun latestUpdatesRequest(page: Int) =
        throw UnsupportedOperationException("This method should not be called!")

    override fun chapterListRequest(manga: SManga) =
        throw UnsupportedOperationException("This method should not be called!")

    override fun mangaDetailsParse(document: Document) =
        throw UnsupportedOperationException("This method should not be called!")

    override fun pageListParse(document: Document) =
        throw UnsupportedOperationException("This method should not be called!")

    override fun popularMangaFromElement(element: Element) =
        throw UnsupportedOperationException("This method should not be called!")

    override fun searchMangaFromElement(element: Element) =
        throw UnsupportedOperationException("This method should not be called!")

    override fun latestUpdatesFromElement(element: Element) =
        throw UnsupportedOperationException("This method should not be called!")

    companion object {
        private const val FAVICON =
            "https://dilbert.com/assets/favicon/favicon-196x196-" +
                "cf4d86b485e628a034ab8b961c1c3520b5969252400a80b9eed544d99403e037.png"

        private const val SUMMARY =
            "A satirical comic strip featuring Dilbert, a competent, but seldom recognized engineer."

        private val dateFormat = SimpleDateFormat("EEEE MMMM dd, yyyy", Locale.US)

        private val currentYear by lazy {
            Calendar.getInstance()[Calendar.YEAR]
        }

        private val currentDay by lazy {
            Calendar.getInstance()[Calendar.DAY_OF_YEAR]
        }
    }
}
