package eu.kanade.tachiyomi.multisrc.zeistmanga

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import uy.kohesive.injekt.injectLazy

abstract class ZeistManga(
    override val name: String,
    override val baseUrl: String,
    override val lang: String,
) : HttpSource() {

    override val supportsLatest = false

    private val json: Json by injectLazy()

    private val intl by lazy { ZeistMangaIntl(lang) }

    override fun popularMangaRequest(page: Int): Request {
        val startIndex = maxMangaResults * (page - 1) + 1
        val url = apiUrl()
            .addQueryParameter("orderby", "published")
            .addQueryParameter("max-results", (maxMangaResults + 1).toString())
            .addQueryParameter("start-index", startIndex.toString())
            .build()

        return GET(url, headers)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val jsonString = response.body.string()
        val result = json.decodeFromString<ZeistMangaDto>(jsonString)

        val mangas = result.feed?.entry.orEmpty()
            .filter { it.category.orEmpty().any { category -> category.term == "Series" } }
            .filter { !it.category.orEmpty().any { category -> category.term == "Anime" } }
            .map { it.toSManga(baseUrl) }

        val mangalist = mangas.toMutableList()
        if (mangas.size == maxMangaResults + 1) {
            mangalist.removeLast()
            return MangasPage(mangalist, true)
        }

        return MangasPage(mangalist, false)
    }

    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException("Not used.")
    override fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException("Not used.")

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val startIndex = maxMangaResults * (page - 1) + 1
        val url = apiUrl()
            .addQueryParameter("max-results", (maxMangaResults + 1).toString())
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

    override fun searchMangaParse(response: Response) = popularMangaParse(response)

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        val profileManga = document.selectFirst(".grid.gtc-235fr")!!
        return SManga.create().apply {
            thumbnail_url = profileManga.selectFirst("img")!!.attr("abs:src")
            description = profileManga.select("#synopsis").text()
            genre = profileManga.select("div.mt-15 > a[rel=tag]")
                .joinToString { it.text() }
        }
    }

    protected open val chapterCategory = "Chapter"

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()

        val url = getChapterFeedUrl(document)

        val req = GET(url, headers)
        val res = client.newCall(req).execute()

        val jsonString = res.body.string()
        val result = json.decodeFromString<ZeistMangaDto>(jsonString)

        return result.feed?.entry?.filter { it.category.orEmpty().any { category -> category.term == chapterCategory } }
            ?.map { it.toSChapter(baseUrl) }
            ?: throw Exception("Failed to parse from chapter API")
    }

    protected open val useNewChapterFeed = false
    protected open val useOldChapterFeed = false

    private val chapterFeedRegex = """clwd\.run\('([^']+)'""".toRegex()
    private val scriptSelector = "#clwd > script"

    open fun getChapterFeedUrl(doc: Document): String {
        if (useNewChapterFeed) return newChapterFeedUrl(doc)
        if (useOldChapterFeed) return oldChapterFeedUrl(doc)

        val script = doc.selectFirst(scriptSelector)
            ?: return runCatching { oldChapterFeedUrl(doc) }
                .getOrElse { newChapterFeedUrl(doc) }

        val feed = chapterFeedRegex
            .find(script.html())
            ?.groupValues?.get(1)
            ?: throw Exception("Failed to find chapter feed")

        return apiUrl(chapterCategory)
            .addPathSegments(feed)
            .addQueryParameter("max-results", maxChapterResults.toString())
            .build().toString()
    }

    private val oldChapterFeedRegex = """([^']+)\?""".toRegex()
    private val oldScriptSelector = "#myUL > script"

    open fun oldChapterFeedUrl(doc: Document): String {
        val script = doc.selectFirst(oldScriptSelector)!!.attr("src")
        val feed = oldChapterFeedRegex
            .find(script)
            ?.groupValues?.get(1)
            ?: throw Exception("Failed to find chapter feed")

        return "$baseUrl$feed?alt=json&start-index=1&max-results=$maxChapterResults"
    }

    private val newChapterFeedRegex = """label\s*=\s*'([^']+)'""".toRegex()
    private val newScriptSelector = "#latest > script"

    private fun newChapterFeedUrl(doc: Document): String {
        var chapterRegex = chapterFeedRegex
        var script = doc.selectFirst(scriptSelector)

        if (script == null) {
            script = doc.selectFirst(newScriptSelector)!!
            chapterRegex = newChapterFeedRegex
        }

        val feed = chapterRegex
            .find(script.html())
            ?.groupValues?.get(1)
            ?: throw Exception("Failed to find chapter feed")

        val url = apiUrl(feed)
            .addQueryParameter("start-index", "1")
            .addQueryParameter("max-results", "999999")
            .build()

        return url.toString()
    }

    open val pageListSelector = "div.check-box div.separator"

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val images = document.select(pageListSelector)
        return images.select("img[src]").mapIndexed { i, img ->
            Page(i, "", img.attr("abs:src"))
        }
    }

    override fun imageUrlParse(response: Response) = throw UnsupportedOperationException("Not used.")

    open fun apiUrl(feed: String = "Series"): HttpUrl.Builder {
        return "$baseUrl/feeds/posts/default/-/".toHttpUrl().newBuilder()
            .addPathSegment(feed)
            .addQueryParameter("alt", "json")
    }

    protected open val hasFilters = false

    protected open val hasStatusFilter = true
    protected open val hasTypeFilter = true
    protected open val hasLanguageFilter = true
    protected open val hasGenreFilter = true

    override fun getFilterList(): FilterList {
        val filterList = mutableListOf<Filter<*>>()

        if (!hasFilters) {
            return FilterList(emptyList())
        }

        if (hasStatusFilter) filterList.add(StatusList(intl.statusFilterTitle, getStatusList()))
        if (hasTypeFilter) filterList.add(TypeList(intl.typeFilterTitle, getTypeList()))
        if (hasLanguageFilter) filterList.add(LanguageList(intl.languageFilterTitle, getLanguageList()))
        if (hasGenreFilter) filterList.add(GenreList(intl.genreFilterTitle, getGenreList()))

        return FilterList(filterList)
    }

    protected open fun getStatusList(): List<Status> = listOf(
        Status(intl.all, ""),
        Status(intl.statusOngoing, "Ongoing"),
        Status(intl.statusCompleted, "Completed"),
        Status(intl.statusDropped, "Dropped"),
        Status(intl.statusUpcoming, "Upcoming"),
        Status(intl.statusHiatus, "Hiatus"),
        Status(intl.statusCancelled, "Cancelled"),
    )

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

    protected open fun getLanguageList(): List<Language> = listOf(
        Language(intl.all, ""),
        Language("Indonesian", "Indonesian"),
        Language("English", "English"),
    )

    companion object {
        private const val maxMangaResults = 20
        private const val maxChapterResults = 999999
    }
}
