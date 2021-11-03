package eu.kanade.tachiyomi.extension.es.manhwalatino

import eu.kanade.tachiyomi.extension.es.manhwalatino.filters.GenreTagFilter
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
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

class ManhwaLatino : ParsedHttpSource() {

    /**
     * Name of the source.
     */
    override val name = "Manhwa-Latino"

    /**
     * Base url of the website without the trailing slash, like: http://mysite.com
     */
    override val baseUrl = "https://manhwa-latino.com"

    /**
     * Parser for Mainsite or Genre Site
     */
    val manhwaLatinoSiteParser = ManhwaLatinoSiteParser(baseUrl)

    /**
     * An ISO 639-1 compliant language code (two letters in lower case).
     */
    override val lang = "es"

    /**
     * Whether the source has support for latest updates.
     */
    override val supportsLatest = true

    /**
     * User Agent for this wWebsite
     */
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36"

    override fun headersBuilder(): Headers.Builder {
        return Headers.Builder()
            .add("User-Agent", userAgent)
            .add("Referer", "$baseUrl/")
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each manga.
     */
    override fun popularMangaSelector(): String {
        return manhwaLatinoSiteParser.popularMangaSelector
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each manga.
     */
    override fun latestUpdatesSelector(): String {
        return manhwaLatinoSiteParser.latestUpdatesSelector
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each manga.
     */
    override fun searchMangaSelector(): String {
        return manhwaLatinoSiteParser.searchMangaSelector
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each chapter.
     */
    override fun chapterListSelector() =
        throw Exception("Not Used")

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    override fun popularMangaNextPageSelector(): String {
        return manhwaLatinoSiteParser.popularMangaNextPageSelector
    }

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    override fun latestUpdatesNextPageSelector(): String {
        return manhwaLatinoSiteParser.latestUpdatesNextPageSelector
    }

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    override fun searchMangaNextPageSelector(): String {
        return manhwaLatinoSiteParser.searchMangaNextPageSelector
    }

    /**
     * Returns the request for the popular manga given the page.
     *
     * @param page the page number to retrieve.
     */
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/page/$page/", headers)
    }

    /**
     * Returns the request for latest manga given the page.
     *
     * @param page the page number to retrieve.
     */
    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/page/$page/", headers)
    }

    /**
     * Parses the response from the site and returns a [MangasPage] object.
     *
     * @param response the response from the site.
     */
    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(latestUpdatesSelector()).map { latestUpdatesFromElement(it) }
        return MangasPage(mangas, manhwaLatinoSiteParser.latestUpdatesHasNextPages())
    }

    /**
     * Returns a manga from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [latestUpdatesSelector].
     */
    override fun latestUpdatesFromElement(element: Element): SManga {
        return manhwaLatinoSiteParser.getMangaFromLastTranslatedSlide(element)
    }

    /**
     * Returns the request for the search manga given the page.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val uri = manhwaLatinoSiteParser.searchMangaRequest(page, query, filters)
        return GET(uri.toString(), headers)
    }

    /**
     * Parses the response from the site and returns a [MangasPage] object.
     *
     * @param response the response from the site.
     */
    override fun searchMangaParse(response: Response): MangasPage {
        return manhwaLatinoSiteParser.searchMangaParse(response)
    }

//    /**
//     * Returns the request for the details of a manga. Override only if it's needed to change the
//     * url, send different headers or request method like POST.
//     *
//     * @param manga the manga to be updated.
//     */
//    override fun mangaDetailsRequest(manga: SManga) = GET(baseUrl + manga.url, headers)

    /**
     * Returns the request for updating the chapter list. Override only if it's needed to override
     * the url, send different headers or request method like POST.
     *
     * @param manga the manga to look for chapters.
     */
    override fun chapterListRequest(manga: SManga): Request {
        return GET(baseUrl + manga.url, headers)
    }

    /**
     * Returns a manga from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [popularMangaSelector].
     */
    override fun popularMangaFromElement(element: Element) = mangaFromElement(element)

    /**
     * Returns a manga from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [searchMangaSelector].
     */
    override fun searchMangaFromElement(element: Element) = mangaFromElement(element)

    /**
     * Returns a manga from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [searchMangaSelector].
     */
    private fun mangaFromElement(element: Element): SManga {
        return manhwaLatinoSiteParser.getMangaFromList(element)
    }

    /**
     * Parses the response from the site and returns a list of chapters.
     *
     * @param response the response from the site.
     */
    override fun chapterListParse(response: Response): List<SChapter> {
        return manhwaLatinoSiteParser.getChapterListParse(response)
    }

    /**
     * Returns a chapter from the given element.
     *
     * @param element an element obtained from [chapterListSelector].
     */
    override fun chapterFromElement(element: Element) = throw Exception("Not used")

    /**
     * Returns the details of the manga from the given [document].
     *
     * @param document the parsed document.
     */
    override fun mangaDetailsParse(document: Document): SManga {
        return manhwaLatinoSiteParser.getMangaDetails(document)
    }

    /**
     * Returns the request for getting the page list. Override only if it's needed to override the
     * url, send different headers or request method like POST.
     * (Request to Webseite with comic)
     *
     * @param chapter the chapter whose page list has to be fetched.
     */
    override fun pageListRequest(chapter: SChapter): Request {
        return GET(baseUrl + chapter.url, headers)
    }

    /**
     * Parses the response from the site and returns the page list.
     * (Parse the comic pages from the website with the chapter)
     *
     * @param response the response from the site.
     */
    override fun pageListParse(response: Response): List<Page> {
        return manhwaLatinoSiteParser.getPageListParse(response)
    }

    /**
     * Returns a page list from the given document.
     *
     * @param document the parsed document.
     */
    override fun pageListParse(document: Document) = throw Exception("Not Used")

    override fun imageUrlParse(document: Document) = throw Exception("Not Used")

    /**
     * Returns the list of filters for the source.
     */
    override fun getFilterList() = FilterList(
        Filter.Header("NOTA: ¡La búsqueda de títulos no funciona!"), // "Title search not working"
        Filter.Separator(),
        GenreTagFilter(),
//        LetterFilter(),
//        StatusFilter(),
//        SortFilter()
    )
}
