package eu.kanade.tachiyomi.extension.en.manta

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

class MantaComics : HttpSource() {
    override val name = "Manta"

    override val lang = "en"

    override val baseUrl = "https://manta.net"

    override val supportsLatest = false

    private val json by injectLazy<Json>()

    override fun headersBuilder() = super.headersBuilder()
        .set("User-Agent", "Manta/167").set("Origin", baseUrl)

    override fun latestUpdatesRequest(page: Int) =
        GET("$baseUrl/manta/v1/search/series?cat=New", headers)

    override fun fetchPopularManga(page: Int) =
        latestUpdatesRequest(page).fetch(::searchMangaParse)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        filters.category.ifEmpty { if (query.isEmpty()) "New" else "" }.let {
            GET("$baseUrl/manta/v1/search/series?cat=$it&q=$query", headers)
        }

    override fun searchMangaParse(response: Response) =
        response.parse<List<Series<Title>>>().map {
            SManga.create().apply {
                title = it.toString()
                url = it.id.toString()
                thumbnail_url = it.image.toString()
            }
        }.let { MangasPage(it, false) }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList) =
        searchMangaRequest(page, query, filters).fetch(::searchMangaParse)

    // Request the actual manga URL for the webview
    override fun mangaDetailsRequest(manga: SManga) =
        GET("$baseUrl/series/${manga.url}")

    override fun mangaDetailsParse(response: Response) =
        SManga.create().apply {
            val data = response.parse<Series<Details>>().data
            description = data.toString()
            genre = data.tags.joinToString()
            artist = data.artists.joinToString()
            author = data.authors.joinToString()
            status = when (data.isCompleted) {
                true -> SManga.COMPLETED
                else -> SManga.ONGOING
            }
            initialized = true
        }

    override fun fetchMangaDetails(manga: SManga) =
        chapterListRequest(manga).fetch(::mangaDetailsParse)

    override fun chapterListRequest(manga: SManga) =
        GET("$baseUrl/front/v1/series/${manga.url}", headers)

    override fun chapterListParse(response: Response) =
        response.parse<Series<Title>>().episodes!!.map {
            SChapter.create().apply {
                name = it.toString()
                url = it.id.toString()
                date_upload = it.timestamp
                chapter_number = it.ord.toFloat()
            }
        }.reversed()

    override fun fetchChapterList(manga: SManga) =
        chapterListRequest(manga).fetch(::chapterListParse)

    override fun pageListRequest(chapter: SChapter) =
        GET("$baseUrl/front/v1/episodes/${chapter.url}", headers)

    override fun pageListParse(response: Response) =
        response.parse<Episode>().run {
            if (!isLocked) return@run cutImages!!
            error("This episode will be available $waitingTime.")
        }.mapIndexed { idx, img -> Page(idx, "", img.toString()) }

    override fun fetchPageList(chapter: SChapter) =
        pageListRequest(chapter).fetch(::pageListParse)

    override fun getFilterList() = FilterList(Category())

    override fun latestUpdatesParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    override fun popularMangaRequest(page: Int) =
        throw UnsupportedOperationException("Not used")

    override fun popularMangaParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    override fun imageUrlParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    private fun <R> Request.fetch(parse: (Response) -> R) =
        client.newCall(this).asObservable().map { res ->
            if (res.isSuccessful) return@map parse(res)
            error(res.parse<Status>("status").toString())
        }!!

    private inline fun <reified T> Response.parse(key: String = "data") =
        json.decodeFromJsonElement<T>(
            json.parseToJsonElement(body.string()).jsonObject[key]!!,
        )
}
