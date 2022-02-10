package eu.kanade.tachiyomi.multisrc.mangahub

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

abstract class MangaHub(
    override val name: String,
    override val baseUrl: String,
    override val lang: String,
    private val dateFormat: SimpleDateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.US)
) : ParsedHttpSource() {

    override val supportsLatest = true

    private val json: Json by injectLazy()

    protected abstract val serverId: String

    protected open val cdnHost = " https://img.mghubcdn.com/".toHttpUrl()

    // Popular
    override fun popularMangaRequest(page: Int): Request =
        GET("$baseUrl/popular/page/$page", headers)

    override fun popularMangaParse(response: Response): MangasPage =
        searchMangaParse(response)

    override fun popularMangaSelector(): String =
        "#mangalist div.media-manga.media"

    override fun popularMangaFromElement(element: Element): SManga =
        searchMangaFromElement(element)

    override fun popularMangaNextPageSelector(): String? =
        searchMangaNextPageSelector()

    // Latest
    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/updates/page/$page", headers)

    override fun latestUpdatesParse(response: Response): MangasPage =
        searchMangaParse(response)

    override fun latestUpdatesSelector(): String =
        popularMangaSelector()

    override fun latestUpdatesFromElement(element: Element): SManga =
        searchMangaFromElement(element)

    override fun latestUpdatesNextPageSelector(): String? =
        searchMangaNextPageSelector()

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        // https://mangahub.io/search/page/1?q=a&order=POPULAR&genre=all
        val url = "$baseUrl/search/page/$page".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
        (if (filters.isEmpty()) getFilterList() else filters).forEach { filter ->
            when (filter) {
                is OrderBy -> {
                    val order = filter.values[filter.state]
                    url.addQueryParameter("order", order.key)
                }
                is GenreList -> {
                    val genre = filter.values[filter.state]
                    url.addQueryParameter("genre", genre.key)
                }
                else -> {}
            }
        }
        return GET(url.toString(), headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        /*
         * To remove duplicates we group by the thumbnail_url, which is
         * common between duplicates. The duplicates have a suffix in the
         * url "-by-{name}". Here we select the shortest url, to avoid
         * removing manga that has "by" in the title already.
         * Example:
         * /manga/tales-of-demons-and-gods (kept)
         * /manga/tales-of-demons-and-gods-by-mad-snail (removed)
         * /manga/leveling-up-by-only-eating (kept)
         */
        val mangas = document.select(searchMangaSelector()).map { element ->
            searchMangaFromElement(element)
        }.groupBy { it.thumbnail_url }.mapValues { (_, values) ->
            values.minByOrNull { it.url.length }!!
        }.values.toList()

        val hasNextPage = searchMangaNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return MangasPage(mangas, hasNextPage)
    }

    override fun searchMangaSelector() = "div#mangalist div.media-manga.media"

    override fun searchMangaFromElement(element: Element): SManga = SManga.create().apply {
        val titleElement = element.select(".media-heading > a").first()
        setUrlWithoutDomain(titleElement.attr("abs:href"))

        title = titleElement.text()
        thumbnail_url = element
            .select("img.manga-thumb.list-item-thumb")
            ?.first()?.attr("abs:src")
    }

    override fun searchMangaNextPageSelector() = "ul.pager li.next > a"

    // Details
    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = document.select("h1._3xnDj").first().ownText()
        author = document
            .select("._3QCtP > div:nth-child(2) > div:nth-child(1) > span:nth-child(2)")
            ?.first()?.text()
        artist = document
            .select("._3QCtP > div:nth-child(2) > div:nth-child(2) > span:nth-child(2)")
            ?.first()?.text()
        genre = document.select("._3Czbn a")?.joinToString { it.text() }
        description = document.select("div#noanim-content-tab-pane-99 p.ZyMp7")?.first()?.text()
        thumbnail_url = document.select("img.img-responsive")?.first()?.attr("abs:src")

        document.select("._3QCtP > div:nth-child(2) > div:nth-child(3) > span:nth-child(2)")
            ?.first()?.text()?.also { statusText ->
                status = when {
                    statusText.contains("ongoing", true) -> SManga.ONGOING
                    statusText.contains("completed", true) -> SManga.COMPLETED
                    else -> SManga.UNKNOWN
                }
            }

        // add alternative name to manga description
        val altName = "Alternative Name: "
        document.select("h1 small").firstOrNull()?.ownText()?.let {
            if (it.isBlank().not()) {
                description = when {
                    description.isNullOrBlank() -> altName + it
                    else -> description + "\n\n$altName" + it
                }
            }
        }
    }

    // Chapters
    override fun chapterListSelector() = ".tab-content .tab-pane li.list-group-item > a"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        setUrlWithoutDomain(element.attr("abs:href"))
        name = element.select("span._8Qtbo").text()
        date_upload = element.select("small.UovLc").first()?.text()
            ?.let { parseChapterDate(it) } ?: 0
    }

    private fun parseChapterDate(date: String): Long {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var parsedDate = 0L
        when {
            "just now" in date || "less than an hour" in date -> {
                parsedDate = now.timeInMillis
            }
            // parses: "1 hour ago" and "2 hours ago"
            "hour" in date -> {
                val hours = date.replaceAfter(" ", "").trim().toInt()
                parsedDate = now.apply { add(Calendar.HOUR, -hours) }.timeInMillis
            }
            // parses: "Yesterday" and "2 days ago"
            "day" in date -> {
                val days = date.replace("days ago", "").trim().toIntOrNull() ?: 1
                parsedDate = now.apply { add(Calendar.DAY_OF_YEAR, -days) }.timeInMillis
            }
            // parses: "2 weeks ago"
            "weeks" in date -> {
                val weeks = date.replace("weeks ago", "").trim().toInt()
                parsedDate = now.apply { add(Calendar.WEEK_OF_YEAR, -weeks) }.timeInMillis
            }
            // parses: "12-20-2019" and defaults everything that wasn't taken into account to 0
            else -> {
                try {
                    parsedDate = dateFormat.parse(date)?.time ?: 0L
                } catch (e: ParseException) {
                    /* nothing to do, parsedDate is initialized with 0L */
                }
            }
        }
        return parsedDate
    }

    // Pages
    override fun pageListRequest(chapter: SChapter): Request {
        val jsonHeaders = headers.newBuilder().add("Content-Type", "application/json").build()

        val slug = chapter.url
            .substringAfter("chapter/")
            .substringBefore("/")
        val number = chapter.url
            .substringAfter("chapter-")
            .removeSuffix("/")
        val body =
            "{\"query\":\"{chapter(x:$serverId,slug:\\\"$slug\\\",number:$number){id,title,mangaID,number,slug,date,pages,noAd,manga{id,title,slug,mainSlug,author,isWebtoon,isYaoi,isPorn,isSoftPorn,unauthFile,isLicensed}}}\"}".toRequestBody(
                null
            )

        return POST("https://api.mghubcdn.com/graphql", jsonHeaders, body)
    }

    override fun pageListParse(response: Response): List<Page> {
        val cdn = "https://img.mghubcdn.com/file/imghub"
        val chapterObject = json
            .decodeFromString<GraphQLDataDto<ChapterDto>>(response.body!!.string())

        val pagesObject = json
            .decodeFromString<JsonObject>(chapterObject.data.chapter.pages)
        val pages = pagesObject.values.map { it.jsonPrimitive.content }

        return pages.mapIndexed { i, path -> Page(i, "", "$cdn/$path") }
    }

    override fun pageListParse(document: Document): List<Page> =
        throw UnsupportedOperationException("Not used.")

    // Image
    override fun imageUrlParse(document: Document): String =
        throw UnsupportedOperationException("Not used.")

    // Filters
    private class Genre(title: String, val key: String) : Filter.TriState(title) {
        override fun toString(): String = name
    }

    private class Order(title: String, val key: String) : Filter.TriState(title) {
        override fun toString(): String = name
    }

    private class OrderBy(orders: Array<Order>) : Filter.Select<Order>("Order", orders, 0)
    private class GenreList(genres: Array<Genre>) : Filter.Select<Genre>("Genres", genres, 0)

    override fun getFilterList() = FilterList(
        OrderBy(orderBy),
        GenreList(genres)
    )

    private val orderBy = arrayOf(
        Order("Popular", "POPULAR"),
        Order("Updates", "LATEST"),
        Order("A-Z", "ALPHABET"),
        Order("New", "NEW"),
        Order("Completed", "COMPLETED")
    )

    private val genres = arrayOf(
        Genre("All Genres", "all"),
        Genre("[no chapters]", "no-chapters"),
        Genre("4-Koma", "4-koma"),
        Genre("Action", "action"),
        Genre("Adult", "adult"),
        Genre("Adventure", "adventure"),
        Genre("Award Winning", "award-winning"),
        Genre("Comedy", "comedy"),
        Genre("Cooking", "cooking"),
        Genre("Crime", "crime"),
        Genre("Demons", "demons"),
        Genre("Doujinshi", "doujinshi"),
        Genre("Drama", "drama"),
        Genre("Ecchi", "ecchi"),
        Genre("Fantasy", "fantasy"),
        Genre("Food", "food"),
        Genre("Game", "game"),
        Genre("Gender bender", "gender-bender"),
        Genre("Harem", "harem"),
        Genre("Historical", "historical"),
        Genre("Horror", "horror"),
        Genre("Isekai", "isekai"),
        Genre("Josei", "josei"),
        Genre("Kids", "kids"),
        Genre("Magic", "magic"),
        Genre("Magical Girls", "magical-girls"),
        Genre("Manhua", "manhua"),
        Genre("Manhwa", "manhwa"),
        Genre("Martial arts", "martial-arts"),
        Genre("Mature", "mature"),
        Genre("Mecha", "mecha"),
        Genre("Medical", "medical"),
        Genre("Military", "military"),
        Genre("Music", "music"),
        Genre("Mystery", "mystery"),
        Genre("One shot", "one-shot"),
        Genre("Oneshot", "oneshot"),
        Genre("Parody", "parody"),
        Genre("Police", "police"),
        Genre("Psychological", "psychological"),
        Genre("Romance", "romance"),
        Genre("School life", "school-life"),
        Genre("Sci fi", "sci-fi"),
        Genre("Seinen", "seinen"),
        Genre("Shotacon", "shotacon"),
        Genre("Shoujo", "shoujo"),
        Genre("Shoujo ai", "shoujo-ai"),
        Genre("Shoujoai", "shoujoai"),
        Genre("Shounen", "shounen"),
        Genre("Shounen ai", "shounen-ai"),
        Genre("Shounenai", "shounenai"),
        Genre("Slice of life", "slice-of-life"),
        Genre("Smut", "smut"),
        Genre("Space", "space"),
        Genre("Sports", "sports"),
        Genre("Super Power", "super-power"),
        Genre("Superhero", "superhero"),
        Genre("Supernatural", "supernatural"),
        Genre("Thriller", "thriller"),
        Genre("Tragedy", "tragedy"),
        Genre("Vampire", "vampire"),
        Genre("Webtoon", "webtoon"),
        Genre("Webtoons", "webtoons"),
        Genre("Wuxia", "wuxia"),
        Genre("Yaoi", "yaoi"),
        Genre("Yuri", "yuri")
    )
}

// DTO
@Serializable
data class GraphQLDataDto<T>(
    val data: T
)

@Serializable
data class ChapterDto(
    val chapter: ChapterInnerDto
)

@Serializable
data class ChapterInnerDto(
    val date: String,
    val id: Int,
    val manga: MangaInnerDto,
    @SerialName("mangaID") val mangaId: Int,
    val noAd: Boolean,
    val number: Float,
    val pages: String,
    val slug: String,
    val title: String
)

@Serializable
data class MangaInnerDto(
    val author: String,
    val id: Int,
    val isLicensed: Boolean,
    val isPorn: Boolean,
    val isSoftPorn: Boolean,
    val isWebtoon: Boolean,
    val isYaoi: Boolean,
    val mainSlug: String,
    val slug: String,
    val title: String,
    val unauthFile: Boolean
)
