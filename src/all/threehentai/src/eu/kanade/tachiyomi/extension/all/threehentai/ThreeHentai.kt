package eu.kanade.tachiyomi.extension.all.threehentai

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.extension.all.threehentai.ThreeHentaiUtils.getArtists
import eu.kanade.tachiyomi.extension.all.threehentai.ThreeHentaiUtils.getCodes
import eu.kanade.tachiyomi.extension.all.threehentai.ThreeHentaiUtils.getGroups
import eu.kanade.tachiyomi.extension.all.threehentai.ThreeHentaiUtils.getNumPages
import eu.kanade.tachiyomi.extension.all.threehentai.ThreeHentaiUtils.getTagDescription
import eu.kanade.tachiyomi.extension.all.threehentai.ThreeHentaiUtils.getTags
import eu.kanade.tachiyomi.extension.all.threehentai.ThreeHentaiUtils.getTime
import eu.kanade.tachiyomi.lib.randomua.addRandomUAPreferenceToScreen
import eu.kanade.tachiyomi.lib.randomua.getPrefCustomUA
import eu.kanade.tachiyomi.lib.randomua.getPrefUAType
import eu.kanade.tachiyomi.lib.randomua.setRandomUserAgent
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

open class ThreeHentai(
    override val lang: String,
    private val h3Lang: String,
) : ConfigurableSource, ParsedHttpSource() {

    final override val baseUrl = "https://3hentai.net"

    override val name = "3Hentai"

    override val supportsLatest = true

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override val client: OkHttpClient by lazy {
        network.cloudflareClient.newBuilder()
            .setRandomUserAgent(
                userAgentType = preferences.getPrefUAType(),
                customUA = preferences.getPrefCustomUA(),
                filterInclude = listOf("chrome"),
            )
            .rateLimit(4)
            .build()
    }

    private var displayFullTitle: Boolean = when (preferences.getString(TITLE_PREF, "full")) {
        "full" -> true
        else -> false
    }

    private val shortenTitleRegex = Regex("""(\[[^]]*]|[({][^)}]*[)}])""")
    private fun String.shortenTitle() = this.replace(shortenTitleRegex, "").trim()

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = TITLE_PREF
            title = TITLE_PREF
            entries = arrayOf("Full Title", "Short Title")
            entryValues = arrayOf("full", "short")
            summary = "%s"
            setDefaultValue("full")

            setOnPreferenceChangeListener { _, newValue ->
                displayFullTitle = when (newValue) {
                    "full" -> true
                    else -> false
                }
                true
            }
        }.also(screen::addPreference)

        addRandomUAPreferenceToScreen(screen)
    }

    override fun latestUpdatesRequest(page: Int) = GET(if (h3Lang.isBlank()) "$baseUrl/$page" else "$baseUrl/language/$h3Lang/$page", headers)

    override fun latestUpdatesSelector() = "#main-content .listing-container .doujin"

    override fun latestUpdatesFromElement(element: Element) = SManga.create().apply {
        setUrlWithoutDomain(element.selectFirst("a")!!.attr("href"))
        title = element.selectFirst("a > .title")!!.text().replace("\"", "").let {
            if (displayFullTitle) it.trim() else it.shortenTitle()
        }
        thumbnail_url = element.selectFirst(".cover img")!!.let { img ->
            if (img.hasAttr("data-src")) img.attr("abs:data-src") else img.attr("abs:src")
        }
    }

    override fun latestUpdatesNextPageSelector() = "#main-content nav .pagination .page-item .page-link[rel=next]"

    override fun popularMangaRequest(page: Int) = GET(
        when {
            h3Lang.isBlank() -> "$baseUrl/search?q=pages%3A>0&sort=popular-7d&page=$page"
            page == 1 -> "$baseUrl/language/$h3Lang?sort=popular-7d"
            else -> "$baseUrl/language/$h3Lang/$page?sort=popular-7d"
        },
        headers,
    )

    override fun popularMangaFromElement(element: Element) = latestUpdatesFromElement(element)

    override fun popularMangaSelector() = latestUpdatesSelector()

    override fun popularMangaNextPageSelector() = latestUpdatesNextPageSelector()

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return when {
            query.startsWith(PREFIX_ID_SEARCH) -> {
                val id = query.removePrefix(PREFIX_ID_SEARCH)
                client.newCall(searchMangaByIdRequest(id))
                    .asObservableSuccess()
                    .map { response -> searchMangaByIdParse(response, id) }
            }
            query.toIntOrNull() != null -> {
                client.newCall(searchMangaByIdRequest(query))
                    .asObservableSuccess()
                    .map { response -> searchMangaByIdParse(response, query) }
            }
            else -> super.fetchSearchManga(page, query, filters)
        }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val fixedQuery = query.trim()
        val filterList = if (filters.isEmpty()) getFilterList() else filters
        val h3LangSearch = if (h3Lang.isBlank()) "" else "+$h3Lang "
        val advQuery = combineQuery(filterList)
        val favoriteFilter = filterList.findInstance<FavoriteFilter>()
        val isOkayToSort = filterList.findInstance<UploadedFilter>()?.state?.isBlank() ?: true
        val offsetPage =
            filterList.findInstance<OffsetPageFilter>()?.state?.toIntOrNull()?.plus(page) ?: page

        if (favoriteFilter?.state == true) {
            val url = "$baseUrl/user/panel/favorites".toHttpUrl().newBuilder()
                .addQueryParameter("q", "$fixedQuery $advQuery")
                .addQueryParameter("page", offsetPage.toString())

            return GET(url.build(), headers)
        } else {
            val url = "$baseUrl/search".toHttpUrl().newBuilder()
                .addQueryParameter("q", "$fixedQuery $h3LangSearch$advQuery")
                .addQueryParameter("page", offsetPage.toString())

            if (isOkayToSort) {
                filterList.findInstance<SortFilter>()?.let { f ->
                    url.addQueryParameter("sort", f.toUriPart())
                }
            }

            return GET(url.build(), headers)
        }
    }

    private fun combineQuery(filters: FilterList): String {
        val stringBuilder = StringBuilder()
        val advSearch = filters.filterIsInstance<AdvSearchEntryFilter>().flatMap { filter ->
            val splitState = filter.state.split(",").map(String::trim).filterNot(String::isBlank)
            splitState.map {
                AdvSearchEntry(filter.name, it.removePrefix("-"), it.startsWith("-"))
            }
        }

        advSearch.forEach { entry ->
            if (entry.exclude) stringBuilder.append("-")
            stringBuilder.append("${entry.name}:")
            stringBuilder.append(entry.text)
            stringBuilder.append(" ")
        }

        return stringBuilder.toString()
    }

    data class AdvSearchEntry(val name: String, val text: String, val exclude: Boolean)

    private fun searchMangaByIdRequest(id: String) = GET("$baseUrl/d/$id", headers)

    private fun searchMangaByIdParse(response: Response, id: String): MangasPage {
        val details = mangaDetailsParse(response)
        details.url = "/d/$id"
        return MangasPage(listOf(details), false)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        if (response.request.url.toString().contains("/login")) {
            val document = response.asJsoup()
            if (document.select("input[value=Login to my account]").isNotEmpty()) {
                throw Exception("Log in via WebView to view favorites")
            }
        }

        return super.searchMangaParse(response)
    }

    override fun searchMangaFromElement(element: Element) = latestUpdatesFromElement(element)

    override fun searchMangaSelector() = latestUpdatesSelector()

    override fun searchMangaNextPageSelector() = latestUpdatesNextPageSelector()

    override fun mangaDetailsParse(document: Document): SManga {
        val fullTitle = document.select("#main-info > h1").text().replace("\"", "").trim()

        return SManga.create().apply {
            title = if (displayFullTitle) fullTitle else fullTitle.shortenTitle()
            thumbnail_url = document.select("#main-cover img").attr("data-src")
            status = SManga.COMPLETED
            artist = getArtists(document)
            author = artist
            val code = getCodes(document)
            // Some people want these additional details in description
            description = "Full English and Japanese titles:\n"
                .plus("$fullTitle\n\n")
                .plus(code ?: "")
                .plus("Pages: ${getNumPages(document)}\n")
                .plus(getTagDescription(document))
            genre = getTags(document)
            update_strategy = UpdateStrategy.ONLY_FETCH_ONCE
        }
    }

    override fun chapterListRequest(manga: SManga): Request = GET("$baseUrl${manga.url}", headers)

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return listOf(
            SChapter.create().apply {
                name = "Chapter"
                scanlator = getGroups(document)
                date_upload = getTime(document)
                setUrlWithoutDomain(response.request.url.encodedPath)
            },
        )
    }

    override fun chapterFromElement(element: Element) = throw UnsupportedOperationException()

    override fun chapterListSelector() = throw UnsupportedOperationException()

    override fun pageListParse(document: Document): List<Page> {
        return document.select("#thumbnail-gallery .single-thumb a > img").mapIndexed { i, img ->
            Page(i, "", img.attr("abs:data-src").replace("t.", "."))
        }
    }

    override fun getFilterList(): FilterList = FilterList(
        Filter.Header("Separate tags with commas (,)"),
        Filter.Header("Prepend with dash (-) to exclude"),
        Filter.Header("Add quote (\"...\") for exact match"),
        TagFilter(),
        CategoryFilter(),
        GroupFilter(),
        ArtistFilter(),
        SeriesFilter(),
        CharactersFilter(),
        Filter.Header("Uploaded valid units are h, d, w, m, y."),
        Filter.Header("example: (>20d)"),
        UploadedFilter(),
        Filter.Header("Filter by pages, for example: (>20)"),
        PagesFilter(),

        Filter.Separator(),
        SortFilter(),
        OffsetPageFilter(),
        Filter.Header("Sort is ignored if favorites only"),
        FavoriteFilter(),
    )

    class TagFilter : AdvSearchEntryFilter("Tags")
    class CategoryFilter : AdvSearchEntryFilter("Categories")
    class GroupFilter : AdvSearchEntryFilter("Groups")
    class ArtistFilter : AdvSearchEntryFilter("Artists")
    class SeriesFilter : AdvSearchEntryFilter("Series")
    class CharactersFilter : AdvSearchEntryFilter("Characters")
    class UploadedFilter : AdvSearchEntryFilter("Uploaded")
    class PagesFilter : AdvSearchEntryFilter("Pages")
    open class AdvSearchEntryFilter(name: String) : Filter.Text(name)

    class OffsetPageFilter : Filter.Text("Offset results by # pages")

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException()

    private class FavoriteFilter : Filter.CheckBox("Show favorites only", false)

    private class SortFilter : UriPartFilter(
        "Sort By",
        arrayOf(
            Pair("Popular: All Time", "popular"),
            Pair("Popular: Month", "popular-month"),
            Pair("Popular: Week", "popular-week"),
            Pair("Popular: Today", "popular-today"),
            Pair("Recent", "date"),
        ),
    )

    private open class UriPartFilter(displayName: String, val pairs: Array<Pair<String, String>>) :
        Filter.Select<String>(displayName, pairs.map { it.first }.toTypedArray()) {
        fun toUriPart() = pairs[state].second
    }

    private inline fun <reified T> Iterable<*>.findInstance() = find { it is T } as? T

    companion object {
        const val PREFIX_ID_SEARCH = "id:"
        private const val TITLE_PREF = "Display manga title as:"
    }
}
