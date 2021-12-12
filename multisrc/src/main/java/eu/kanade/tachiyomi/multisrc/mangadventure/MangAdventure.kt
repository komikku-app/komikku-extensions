package eu.kanade.tachiyomi.multisrc.mangadventure

import android.net.Uri
import android.os.Build.VERSION
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import eu.kanade.tachiyomi.source.model.Page as SPage

/** MangAdventure base source. */
abstract class MangAdventure(
    override val name: String,
    override val baseUrl: String,
    override val lang: String = "en"
) : HttpSource() {
    /** The site's manga categories. */
    protected open val categories = DEFAULT_CATEGORIES

    /** The site's manga status names. */
    protected open val statuses = arrayOf("Any", "Completed", "Ongoing")

    /** The site's sort order labels that correspond to [SortOrder.values]. */
    protected open val orders = arrayOf(
        "Title", "Views", "Latest upload", "Chapter count"
    )

    /** A user agent representing Tachiyomi. */
    private val userAgent = "Mozilla/5.0 " +
        "(Android ${VERSION.RELEASE}; Mobile) " +
        "Tachiyomi/${BuildConfig.VERSION_NAME}"

    /** The URI of the site's API. */
    private val apiUri by lazy {
        Uri.parse("$baseUrl/api/v$versionId")!!
    }

    /** The JSON parser of the class. */
    private val json by injectLazy<Json>()

    override val versionId = 2

    override val supportsLatest = true

    override fun headersBuilder() =
        super.headersBuilder().set("User-Agent", userAgent)

    override fun latestUpdatesRequest(page: Int) =
        apiUri.buildUpon().appendEncodedPath("series").run {
            appendQueryParameter("page", page.toString())
            appendQueryParameter("sort", "-latest_upload")
            GET(toString(), headers)
        }

    override fun popularMangaRequest(page: Int) =
        apiUri.buildUpon().appendEncodedPath("series").run {
            appendQueryParameter("page", page.toString())
            appendQueryParameter("sort", "-views")
            GET(toString(), headers)
        }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        apiUri.buildUpon().appendEncodedPath("series").run {
            if (query.startsWith(SLUG_QUERY)) {
                appendQueryParameter("slug", query.substring(SLUG_QUERY.length))
            } else {
                appendQueryParameter("page", page.toString())
                appendQueryParameter("title", query)
                filters.filterIsInstance<UriFilter>().forEach {
                    appendQueryParameter(it.param, it.toString())
                }
            }
            GET(toString(), headers)
        }

    override fun chapterListRequest(manga: SManga) =
        apiUri.buildUpon().appendEncodedPath("chapters").run {
            appendQueryParameter("series", manga.slug)
            appendQueryParameter("date_format", "timestamp")
            GET(toString(), headers)
        }

    override fun pageListRequest(chapter: SChapter) =
        apiUri.buildUpon().appendEncodedPath("pages").run {
            val (slug, vol, num) = chapter.components
            appendQueryParameter("track", "true")
            appendQueryParameter("series", slug)
            appendQueryParameter("volume", vol)
            appendQueryParameter("number", num)
            GET(toString(), headers)
        }

    override fun latestUpdatesParse(response: Response) =
        response.decode<Paginator<Series>>().let {
            MangasPage(it.map(::mangaFromJSON), !it.last)
        }

    override fun searchMangaParse(response: Response) =
        latestUpdatesParse(response)

    override fun popularMangaParse(response: Response) =
        latestUpdatesParse(response)

    override fun chapterListParse(response: Response) =
        response.decode<Results<Chapter>>().map { chapter ->
            SChapter.create().apply {
                url = chapter.url
                name = buildString {
                    append(chapter.full_title)
                    if (chapter.final) append(" [END]")
                }
                chapter_number = chapter.number
                date_upload = chapter.published.toLong()
                scanlator = chapter.groups.joinToString()
            }
        }

    override fun mangaDetailsParse(response: Response) =
        response.decode<Series>().let(::mangaFromJSON)

    override fun pageListParse(response: Response) =
        response.decode<Results<Page>>().map { page ->
            SPage(page.number, page.url, page.image)
        }

    // Return the real URL for "Open in browser"
    override fun mangaDetailsRequest(manga: SManga) =
        GET(baseUrl + manga.url, headers)

    // Workaround to allow "Open in browser" to use the real URL
    override fun fetchMangaDetails(manga: SManga) =
        client.newCall(GET("$apiUri/series/${manga.slug}", headers))
            .asObservableSuccess().map {
                mangaDetailsParse(it).apply { initialized = true }
            }!!

    override fun imageUrlParse(response: Response) =
        throw UnsupportedOperationException("Not used!")

    override fun getFilterList() =
        FilterList(
            Author(),
            Artist(),
            SortOrder(orders),
            Status(statuses),
            CategoryList(categories)
        )

    /** The slug of the manga. */
    private inline val SManga.slug
        get() = url.split('/')[2]

    /** The components (series, volume, number) of the chapter. */
    private inline val SChapter.components
        get() = url.split('/').slice(2..5)

    /** Decodes the JSON response as an object. */
    private inline fun <reified T> Response.decode() =
        json.decodeFromJsonElement<T>(json.parseToJsonElement(body!!.string()))

    /** Converts a [Series] object to an [SManga]. */
    private fun mangaFromJSON(series: Series) =
        SManga.create().apply {
            url = series.url
            title = series.title
            thumbnail_url = series.cover
            description = buildString {
                series.description?.let(::append)
                series.aliases.let {
                    if (!it.isNullOrEmpty())
                        it.joinTo(this, "\n", "\n\nAlternative titles:\n")
                }
            }
            author = series.authors?.joinToString()
            artist = series.artists?.joinToString()
            genre = series.categories?.joinToString()
            status = if (series.licensed == true) {
                SManga.LICENSED
            } else when (series.completed) {
                true -> SManga.COMPLETED
                false -> SManga.ONGOING
                null -> SManga.UNKNOWN
            }
        }

    companion object {
        /** Manga categories from MangAdventure `categories.xml` fixture. */
        val DEFAULT_CATEGORIES = listOf(
            "4-Koma",
            "Action",
            "Adventure",
            "Comedy",
            "Doujinshi",
            "Drama",
            "Ecchi",
            "Fantasy",
            "Gender Bender",
            "Harem",
            "Hentai",
            "Historical",
            "Horror",
            "Josei",
            "Martial Arts",
            "Mecha",
            "Mystery",
            "Psychological",
            "Romance",
            "School Life",
            "Sci-Fi",
            "Seinen",
            "Shoujo",
            "Shoujo Ai",
            "Shounen",
            "Shounen Ai",
            "Slice of Life",
            "Smut",
            "Sports",
            "Supernatural",
            "Tragedy",
            "Yaoi",
            "Yuri"
        )

        /** Query to search by manga slug. */
        internal const val SLUG_QUERY = "slug:"
    }
}
