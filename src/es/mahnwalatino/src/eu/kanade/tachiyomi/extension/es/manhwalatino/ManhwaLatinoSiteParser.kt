package eu.kanade.tachiyomi.extension.es.manhwalatino

import android.net.Uri
import eu.kanade.tachiyomi.extension.es.manhwalatino.filters.UriFilter
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ManhwaLatinoSiteParser(
    private val baseUrl: String,
    private val client: OkHttpClient,
    private val headers: Headers,
) {

    /**
     * Search Type
     */
    enum class SearchType {
        SEARCH_FREE, SEARCH_FILTER
    }

    /**
     * Type of search ( FREE, FILTER)
     */
    private var searchType = SearchType.SEARCH_FREE

    /**
     * The Latest Updates are in a Slider, this Methods get a Manga from the slide
     */
    fun getMangaFromLastTranslatedSlide(element: Element): SManga {
        val manga = SManga.create()
        manga.url =
            getUrlWithoutDomain(
                element.select(MLConstants.latestUpdatesSelectorUrl).first()!!.attr("abs:href"),
            )
        manga.title = element.select(MLConstants.latestUpdatesSelectorTitle).text().trim()
        manga.thumbnail_url = getImage(element.select(MLConstants.latestUpdatesSelectorThumbnailUrl)).replace("//", "/")
        return manga
    }

    /**
     * The Latest Updates has only one site
     */
    fun latestUpdatesHasNextPages() = false

    /**
     * Search the information of a Manga with the URL-Address
     */
    fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return if (query.startsWith(MLConstants.PREFIX_MANGA_ID_SEARCH)) {
            val realQuery = query.removePrefix(MLConstants.PREFIX_MANGA_ID_SEARCH)
            client.newCall(GET("$baseUrl/$realQuery", headers))
                .asObservableSuccess().map { response ->
                    val details = getMangaDetails(response.asJsoup())
                    details.url = "/$realQuery"
                    MangasPage(listOf(details), false)
                }
        } else {
            val request = GET(searchMangaRequest(page, query, filters).toString(), headers)
            client.newCall(request)
                .asObservableSuccess().map { response ->
                    searchMangaParse(response)
                }
        }
    }

    /**
     * Get eine Liste mit Mangas from Search Site
     */
    private fun getMangasFromSearchSite(document: Document): List<SManga> {
        return document.select(MLConstants.searchSiteMangasHTMLSelector).map {
            val manga = SManga.create()
            manga.url = getUrlWithoutDomain(
                it.select(MLConstants.searchPageUrlHTMLSelector).attr("abs:href"),
            )
            manga.title = it.select(MLConstants.searchPageTitleHTMLSelector).text().trim()
            manga.thumbnail_url = getImage(it.select(MLConstants.searchPageThumbnailUrlMangaHTMLSelector))
            manga
        }
    }

    /**
     * Get eine Liste mit Mangas from Genre Site
     */
    private fun getMangasFromGenreSite(document: Document): List<SManga> {
        return document.select(MLConstants.genreSiteMangasHTMLSelector).map { getMangaFromList(it) }
    }

    /**
     * Parse The Information from Mangas From Popular or Genre Site
     * Title, Address and thumbnail_url
     */
    fun getMangaFromList(element: Element): SManga {
        val manga = SManga.create()
        manga.url = getUrlWithoutDomain(
            element.select(MLConstants.popularGenreUrlHTMLSelector).attr("abs:href"),
        )
        manga.title = element.select(MLConstants.popularGenreTitleHTMLSelector).text().trim()
        manga.thumbnail_url = getImage(element.select(MLConstants.popularGenreThumbnailUrlMangaHTMLSelector))
        return manga
    }

    /**
     * Get The Details of a Manga Main Website
     * Description, genre, tags, picture (thumbnail_url)
     * status...
     */
    fun getMangaDetails(document: Document): SManga {
        val manga = SManga.create()

        val titleElements = document.select("#manga-title h1")
        val descriptionList =
            document.select(MLConstants.mangaDetailsDescriptionHTMLSelector).map { it.text() }
        val author = document.select(MLConstants.mangaDetailsAuthorHTMLSelector).text()
        val artist = document.select(MLConstants.mangaDetailsArtistHTMLSelector).text()

        val genrelist = document.select(MLConstants.mangaDetailsGenreHTMLSelector).map { it.text() }
        val tagList = document.select(MLConstants.mangaDetailsTagsHTMLSelector).map { it.text() }
        val genreTagList = genrelist + tagList

        manga.title = titleElements.last()!!.ownText().trim()
        manga.thumbnail_url = getImage(document.select(MLConstants.mangaDetailsThumbnailUrlHTMLSelector))
        manga.description = descriptionList.joinToString("\n")
        manga.author = author.ifBlank { "Autor Desconocido" }
        manga.artist = artist
        manga.genre = genreTagList.joinToString(", ")
        manga.status = findMangaStatus(tagList, document.select(MLConstants.mangaDetailsAttributes))
        return manga
    }

    private fun findMangaStatus(tagList: List<String>, elements: Elements): Int {
        if (tagList.contains("Fin")) {
            return SManga.COMPLETED
        }
        elements.forEach { element ->
            val key = element.select("div.summary-heading h5").text().trim()
            val value = element.select("div.summary-content").text().trim()

            if (key == "Estado del comic") {
                return when (value) {
                    "Publicandose" -> SManga.ONGOING
                    else -> SManga.UNKNOWN
                }
            }
        }
        return SManga.UNKNOWN
    }

    /**
     * Parses the response from the site and returns a list of chapters.
     *
     * @param response the response from the site.
     */
    fun getChapterListParse(response: Response): List<SChapter> {
        return response.asJsoup().select(MLConstants.chapterListParseSelector).map { element ->
            // Link to the Chapter with the info (address and chapter title)
            val chapterInfo = element.select(MLConstants.chapterLinkParser)
            // Chaptername
            val chapterName = chapterInfo.text().trim()
            // release date came as text with format dd/mm/yyyy from a link or <i>dd/mm/yyyy</i>
            val chapterReleaseDate = getChapterReleaseDate(element)
            SChapter.create().apply {
                name = chapterName
                chapter_number = getChapterNumber(chapterName)
                url = getUrlWithoutDomain(chapterInfo.attr("abs:href"))
                date_upload = parseChapterReleaseDate(chapterReleaseDate)
            }
        }
    }

    /**
     * Get the number of Chapter from Chaptername
     */
    private fun getChapterNumber(chapterName: String): Float =
        Regex("""\d+""").find(chapterName)?.value.toString().trim().toFloat()

    /**
     * Get The String with the information about the Release date of the Chapter
     */
    private fun getChapterReleaseDate(element: Element): String {
        val chapterReleaseDateLink =
            element.select(MLConstants.chapterReleaseDateLinkParser).attr("title")
        val chapterReleaseDateI = element.select(MLConstants.chapterReleaseDateIParser).text()
        return when {
            chapterReleaseDateLink.isNotEmpty() -> chapterReleaseDateLink
            chapterReleaseDateI.isNotEmpty() -> chapterReleaseDateI
            else -> ""
        }
    }

    /**
     * Transform String with the Date of Release into Long format
     */
    private fun parseChapterReleaseDate(releaseDateStr: String): Long {
        val regExSecs = Regex("""hace\s+(\d+)\s+segundos?""")
        val regExMins = Regex("""hace\s+(\d+)\s+mins?""")
        val regExHours = Regex("""hace\s+(\d+)\s+horas?""")
        val regExDays = Regex("""hace\s+(\d+)\s+días?""")
        val regExDate = Regex("""\d+/\d+/\d+""")

        return when {
            regExSecs.containsMatchIn(releaseDateStr) ->
                getReleaseTime(releaseDateStr, Calendar.SECOND)

            regExMins.containsMatchIn(releaseDateStr) ->
                getReleaseTime(releaseDateStr, Calendar.MINUTE)

            regExHours.containsMatchIn(releaseDateStr) ->
                getReleaseTime(releaseDateStr, Calendar.HOUR)

            regExDays.containsMatchIn(releaseDateStr) ->
                getReleaseTime(releaseDateStr, Calendar.DAY_OF_YEAR)

            regExDate.containsMatchIn(releaseDateStr) ->
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(releaseDateStr)!!.time

            else -> 0
        }
    }

    /**
     * Extract the Release time from a Text String
     * Format of the String "hace\s+\d+\s(segundo|minuto|hora|dia)s?"
     */
    private fun getReleaseTime(releaseDateStr: String, timeType: Int): Long {
        val releaseTimeAgo = Regex("""\d+""").find(releaseDateStr)?.value.toString().toInt()
        val calendar = Calendar.getInstance()
        calendar.add(timeType, -releaseTimeAgo)
        return calendar.timeInMillis
    }

    /**
     * Parses the response from the site and returns the page list.
     * (Parse the comic pages from the website with the chapter)
     *
     * @param response the response from the site.
     */
    fun getPageListParse(response: Response): List<Page> {
        val list =
            response.asJsoup().select(MLConstants.pageListParseSelector)
                .mapIndexed { index, imgElement ->
                    Page(index, "", getImage(imgElement))
                }
        return list
    }

    /**
     * Returns the request for the search manga given the page.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    fun searchMangaRequest(page: Int, query: String, filters: FilterList): Uri.Builder {
        val uri = Uri.parse(baseUrl).buildUpon()
        if (query.isNotBlank()) {
            searchType = SearchType.SEARCH_FREE
            uri.appendQueryParameter("s", query)
                .appendQueryParameter("post_type", "wp-manga")
        } else {
            searchType = SearchType.SEARCH_FILTER
            // Append uri filters
            filters.forEach {
                if (it is UriFilter) {
                    it.addToUri(uri)
                }
            }
            uri.appendPath("page").appendPath(page.toString())
        }
        return uri
    }

    /**
     * Parses the response from the site and returns a [MangasPage] object.
     *
     * @param response the response from the site.
     */
    fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val hasNextPages = hasNextPages(document)

        val mangas: List<SManga> = when (searchType) {
            SearchType.SEARCH_FREE ->
                getMangasFromSearchSite(document)
            SearchType.SEARCH_FILTER ->
                getMangasFromGenreSite(document)
        }

        return MangasPage(mangas, hasNextPages)
    }

    /**
     * Check if there ir another page to show
     */
    private fun hasNextPages(document: Document): Boolean {
        return !document.select(MLConstants.searchMangaNextPageSelector).isEmpty()
    }

    /**
     * Create a Address url without the base url.
     */
    private fun getUrlWithoutDomain(url: String) = url.substringAfter(baseUrl)

    /**
     * Extract the Image from the Html Element
     * The website changes often the attr of the images
     * data-src or src
     */
    private fun getImage(elements: Elements): String {
        var imageUrl: String = elements.attr(MLConstants.imageAttribute)
        if (imageUrl.isEmpty()) {
            imageUrl = elements.attr("abs:src")
        }
        return imageUrl
    }

    /**
     * Extract the Image from the Html Element
     * The website changes often the attr of the images
     * data-src or src
     */
    private fun getImage(element: Element): String {
        var imageUrl = element.attr(MLConstants.imageAttribute)
        if (imageUrl.isEmpty()) {
            imageUrl = element.attr("abs:src")
        }
        return imageUrl
    }
}
