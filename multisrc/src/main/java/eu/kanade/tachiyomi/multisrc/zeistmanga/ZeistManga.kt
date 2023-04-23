package eu.kanade.tachiyomi.multisrc.zeistmanga

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy

abstract class ZeistManga(
    override val name: String,
    override val baseUrl: String,
    override val lang: String,
) : ParsedHttpSource() {

    override val supportsLatest = false
    open val hasFilters = false
    protected val json: Json by injectLazy()
    protected val intl by lazy { ZeistMangaIntl(lang) }

    open val chapterFeedRegex = """clwd\.run\('([^']+)'""".toRegex()
    open val scriptSelector = "#clwd > script"

    open val oldChapterFeedRegex = """([^']+)\?""".toRegex()
    open val oldScriptSelector = "#myUL > script"

    open val pageListSelector = "div.check-box div.separator"

    open fun getApiUrl(doc: Document): String {
        val script = doc.selectFirst(scriptSelector)

        if (script == null) {
            val altScript = doc.selectFirst(oldScriptSelector)!!.attr("src")
            val feed = oldChapterFeedRegex
                .find(altScript)
                ?.groupValues?.get(1)
                ?: throw Exception("Failed to find chapter feed")

            return "$baseUrl$feed?alt=json&start-index=2&max-results=999999"
        }

        val feed = chapterFeedRegex
            .find(script.html())
            ?.groupValues?.get(1)
            ?: throw Exception("Failed to find chapter feed")

        return apiUrl("Chapter")
            .addPathSegments(feed)
            .addQueryParameter("max-results", "999999") // Get all chapters
            .build().toString()
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()

        val url = getApiUrl(document)

        val req = GET(url, headers)
        val res = client.newCall(req).execute()

        val jsonString = res.body.string()
        val result = json.decodeFromString<ZeistMangaDto>(jsonString)

        return result.feed?.entry?.map { it.toSChapter(baseUrl) }
            ?: throw Exception("Failed to parse from chapter API")
    }

    override fun imageUrlParse(document: Document): String {
        throw UnsupportedOperationException("Not used.")
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        throw UnsupportedOperationException("Not used.")
    }

    override fun latestUpdatesRequest(page: Int): Request {
        throw UnsupportedOperationException("Not used.")
    }

    override fun chapterFromElement(element: Element): SChapter {
        throw UnsupportedOperationException("Not used.")
    }

    override fun chapterListSelector(): String {
        throw UnsupportedOperationException("Not used.")
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        throw UnsupportedOperationException("Not used.")
    }

    override fun latestUpdatesNextPageSelector(): String? {
        throw UnsupportedOperationException("Not used.")
    }

    override fun latestUpdatesSelector(): String {
        throw UnsupportedOperationException("Not used.")
    }

    override fun popularMangaFromElement(element: Element): SManga {
        throw UnsupportedOperationException("Not used.")
    }

    override fun popularMangaNextPageSelector(): String? {
        throw UnsupportedOperationException("Not used.")
    }

    override fun popularMangaSelector(): String {
        throw UnsupportedOperationException("Not used.")
    }

    override fun searchMangaFromElement(element: Element): SManga {
        throw UnsupportedOperationException("Not used.")
    }

    override fun searchMangaNextPageSelector(): String? {
        throw UnsupportedOperationException("Not used.")
    }

    override fun searchMangaSelector(): String {
        throw UnsupportedOperationException("Not used.")
    }

    override fun mangaDetailsParse(document: Document): SManga {
        val profileManga = document.selectFirst(".grid.gtc-235fr")!!
        return SManga.create().apply {
            thumbnail_url = profileManga.selectFirst("img")!!.attr("src")
            description = profileManga.select("#synopsis").text()
            genre = profileManga.select("div.mt-15 > a[rel=tag]")
                .joinToString { it.text() }
        }
    }

    override fun pageListParse(document: Document): List<Page> {
        val images = document.select(pageListSelector)
        return images.select("img[src]").mapIndexed { i, img ->
            Page(i, "", img.attr("src"))
        }
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val jsonString = response.body.string()
        val result = json.decodeFromString<ZeistMangaDto>(jsonString)

        val mangas = result.feed?.entry.orEmpty()
            .filter { !it.category.orEmpty().any { category -> category.term == "Anime" } } // Skip animes
            .map { it.toSManga(baseUrl) }

        val mangalist = mangas.toMutableList()
        if (mangas.size == maxResults + 1) {
            mangalist.removeLast()
            return MangasPage(mangalist, true)
        }

        return MangasPage(mangalist, false)
    }

    override fun popularMangaRequest(page: Int): Request {
        val startIndex = maxResults * (page - 1) + 1
        val url = apiUrl()
            .addQueryParameter("orderby", "published")
            .addQueryParameter("max-results", (maxResults + 1).toString())
            .addQueryParameter("start-index", startIndex.toString())
            .build()

        return GET(url, headers)
    }

    override fun searchMangaParse(response: Response) = popularMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val startIndex = maxResults * (page - 1) + 1
        val url = apiUrl()
            .addQueryParameter("max-results", (maxResults + 1).toString())
            .addQueryParameter("start-index", startIndex.toString())

        if (query.isNotBlank()) {
            url.addQueryParameter("q", query)
            return GET(url.build(), headers)
        }

        filters.forEach { filter ->
            when (filter) {
                is StatusList -> {
                    url.addPathSegment(filter.selected.value)
                }

                is TypeList -> {
                    url.addPathSegment(filter.selected.value)
                }

                is LanguageList -> {
                    url.addPathSegment(filter.selected.value)
                }

                is GenreList -> {
                    filter.state.forEach { genre ->
                        when (genre.state) {
                            true -> url.addPathSegment(genre.value)
                            false -> {}
                        }
                    }
                }

                else -> {}
            }
        }

        return GET(url.build(), headers)
    }

    open fun apiUrl(feed: String = "Series"): HttpUrl.Builder {
        return "$baseUrl/feeds/posts/default/-/".toHttpUrl().newBuilder()
            .addPathSegment(feed)
            .addQueryParameter("alt", "json")
    }

    override fun getFilterList(): FilterList {
        if (!hasFilters) {
            return FilterList(emptyList())
        }

        return FilterList(
            Filter.Header(intl.filterWarning),
            Filter.Separator(),
            StatusList(intl.statusFilterTitle, getStatusList()),
            TypeList(intl.typeFilterTitle, getTypeList()),
            LanguageList(intl.languageFilterTitle, getLanguageList()),
            GenreList(intl.genreFilterTitle, getGenreList()),
        )
    }

    // Theme Default Status
    protected open fun getStatusList(): List<Status> = listOf(
        Status(intl.all, ""),
        Status(intl.statusOngoing, "Ongoing"),
        Status(intl.statusCompleted, "Completed"),
        Status(intl.statusDropped, "Dropped"),
        Status(intl.statusUpcoming, "Upcoming"),
        Status(intl.statusHiatus, "Hiatus"),
        Status(intl.statusCancelled, "Cancelled"),
    )

    // Theme Default Types
    protected open fun getTypeList(): List<Type> = listOf(
        Type(intl.all, ""),
        Type(intl.typeManga, "Manga"),
        Type(intl.typeManhua, "Manhua"),
        Type(intl.typeManhwa, "Manhwa"),
        Type(intl.typeNovel, "Novel"),
        Type(intl.typeWebNovelJP, "Web Novel (JP)"),
        Type(intl.typeWebNovelKR, "Web Novel (KR)"),
        Type(intl.typeWebNovelCN, "Web Novel (CN)"),
        Type(intl.typeDoujinshi, "Doujinshi"),
    )

    // Theme Default Genres
    protected open fun getGenreList(): List<Genre> = listOf(
        Genre("Action", "Action"),
        Genre("Adventurer", "Adventurer"),
        Genre("Comedy", "Comedy"),
        Genre("Dementia", "Dementia"),
        Genre("Drama", "Drama"),
        Genre("Ecchi", "Ecchi"),
        Genre("Fantasy", "Fantasy"),
        Genre("Game", "Game"),
        Genre("Harem", "Harem"),
        Genre("Historical", "Historical"),
        Genre("Horror", "Horror"),
        Genre("Josei", "Josei"),
        Genre("Magic", "Magic"),
        Genre("Martial Arts", "Martial Arts"),
        Genre("Mecha", "Mecha"),
        Genre("Military", "Military"),
        Genre("Music", "Music"),
        Genre("Mystery", "Mystery"),
        Genre("Parody", "Parody"),
        Genre("Police", "Police"),
        Genre("Psychological", "Psychological"),
        Genre("Romance", "Romance"),
        Genre("Samurai", "Samurai"),
        Genre("School", "School"),
        Genre("Sci-fi", "Sci-fi"),
        Genre("Seinen", "Seinen"),
        Genre("Shoujo", "Shoujo"),
        Genre("Shoujo Ai", "Shoujo Ai"),
        Genre("Shounen", "Shounen"),
        Genre("Slice of Life", "Slice of Life"),
        Genre("Space", "Space"),
        Genre("Sports", "Sports"),
        Genre("Super Power", "Super Power"),
        Genre("SuperNatural", "SuperNatural"),
        Genre("Thriller", "Thriller"),
        Genre("Vampire", "Vampire"),
        Genre("Work Life", "Work Life"),
        Genre("Yuri", "Yuri"),
    )

    // Theme Default Languages
    protected open fun getLanguageList(): List<Language> = listOf(
        Language(intl.all, ""),
        Language("Indonesian", "Indonesian"),
        Language("English", "English"),
    )

    companion object {
        private const val maxResults = 20
    }
}
