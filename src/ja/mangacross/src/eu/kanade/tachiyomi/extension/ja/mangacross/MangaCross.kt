package eu.kanade.tachiyomi.extension.ja.mangacross

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy
import kotlin.concurrent.thread

const val maxEntries = 9999

class MangaCross : HttpSource() {
    override val name = "Manga Cross"
    override val lang = "ja"
    override val baseUrl = "https://mangacross.jp"
    override val supportsLatest = true

    private val json: Json by injectLazy()

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/api/comics.json?count=$maxEntries", headers)

    override fun popularMangaParse(response: Response) = MangasPage(
        json.decodeFromString<MCComicList>(response.body!!.string()).comics.map(MCComic::toSManga),
        false // pagination does not work
    )

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/api/episodes.json?page=$page", headers)

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = json.decodeFromString<MCEpisodeList>(response.body!!.string())
        return MangasPage(result.episodes.map { it.comic!!.toSManga() }, result.current_page < result.total_pages)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        if (query.isNotEmpty()) {
            GET("$baseUrl/api/comics/keywords/$query.json", headers)
        } else when (val tag = filters.filterIsInstance<TagFilter>().firstOrNull()?.getTag()) {
            null -> popularMangaRequest(page)
            is MCComicCategory -> GET("$baseUrl/api/comics/categories/${tag.name}.json", headers)
            is MCComicGenre -> GET("$baseUrl/api/comics/tags/${tag.name}.json", headers)
        }

    override fun searchMangaParse(response: Response) = popularMangaParse(response)

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> =
        client.newCall(chapterListRequest(manga))
            .asObservableSuccess()
            .map { mangaDetailsParse(it).apply { initialized = true } }

    // mangaDetailsRequest untouched in order to let WebView open web page instead of json

    override fun mangaDetailsParse(response: Response) =
        json.decodeFromString<MCComicDetails>(response.body!!.string()).comic.toSManga()

    override fun chapterListRequest(manga: SManga) = GET("$baseUrl/api${manga.url}.json", headers)

    override fun chapterListParse(response: Response) =
        json.decodeFromString<MCComicDetails>(response.body!!.string()).comic.toSChapterList()

    override fun pageListParse(response: Response): List<Page> {
        return try {
            json.decodeFromString<MCViewer>(response.body!!.string()).episode_pages.mapIndexed { i, it ->
                Page(i, "", it.image.original_url)
            }
        } catch (e: SerializationException) {
            throw Exception("Chapter is no longer available!")
        }
    }

    override fun imageUrlParse(response: Response) = throw UnsupportedOperationException("Not used.")

    private lateinit var tags: List<Pair<String, MCComicTag?>>

    init {
        thread {
            val response = client.newCall(GET("$baseUrl/api/menus.json", headers)).execute()
            val filterList = json.decodeFromString<MCMenu>(response.body!!.string()).toFilterList()
            tags = listOf(Pair("None", null)) + filterList
        }
    }

    override fun getFilterList() =
        if (::tags.isInitialized) FilterList(
            Filter.Header("NOTE: Ignored if using text search!"),
            TagFilter("Tag", tags)
        ) else FilterList(
            Filter.Header("Tags not fetched yet. Go back and retry."),
        )

    private class TagFilter(displayName: String, private val tags: List<Pair<String, MCComicTag?>>) :
        Filter.Select<String>(displayName, tags.map { it.first }.toTypedArray()) {
        fun getTag() = tags[state].second
    }
}
