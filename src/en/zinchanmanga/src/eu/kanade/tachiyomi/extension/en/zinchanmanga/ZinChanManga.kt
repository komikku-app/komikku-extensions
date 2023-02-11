package eu.kanade.tachiyomi.extension.en.zinchanmanga

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

class ZinChanManga : HttpSource() {
    override val lang = "en"

    override val name = "ZinChanManga"

    override val baseUrl = "https://zinchanmanga.net"

    override val supportsLatest = true

    private val json by injectLazy<Json>()

    private val apiClient by lazy {
        network.client.newBuilder()
            .sslSocketFactory(ZinChanCert.factory, ZinChanCert.manager)
            .rateLimit(5, 50)
            .build()
    }

    private val apiHeaders by lazy {
        headers.newBuilder().add("Origin", baseUrl).build()
    }

    override fun latestUpdatesRequest(page: Int) =
        GET("$API_URL/latest-manga-updates?page=$page&total=10", apiHeaders)

    override fun popularMangaRequest(page: Int) =
        GET("$API_URL/all?page=$page&total=10", apiHeaders)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        filters.find { it is ZinChanGenre }?.takeIf { it.state != 0 }?.let {
            GET("$API_URL/category?id_cate=$it&page=$page&total=10", apiHeaders)
        } ?: GET("$API_URL/search?keyword=$query&page=$page&total=10", apiHeaders)

    // Request the frontend URL for the webview
    override fun mangaDetailsRequest(manga: SManga) =
        GET("$baseUrl/manga/${manga.url}", headers)

    override fun chapterListRequest(manga: SManga) =
        GET("$API_URL/list-chapters?id_story=${manga.id}&id_user=-1", apiHeaders)

    override fun pageListRequest(chapter: SChapter) =
        GET("$API_URL/reading${chapter.url}", apiHeaders)

    override fun fetchLatestUpdates(page: Int) =
        fetchManga(latestUpdatesRequest(page), page)

    override fun fetchPopularManga(page: Int) =
        fetchManga(popularMangaRequest(page), page)

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList) =
        fetchManga(searchMangaRequest(page, query, filters), page)

    override fun fetchMangaDetails(manga: SManga) =
        rx.Observable.just(manga.apply { initialized = true })!!

    override fun fetchChapterList(manga: SManga) =
        apiClient.newCall(chapterListRequest(manga)).asObservableSuccess().map { res ->
            res.parse<Data<Chapter>>().map {
                SChapter.create().apply {
                    name = it.title
                    chapter_number = it.number
                    date_upload = it.timestamp
                    url = "${it.params}&id_story=${manga.id}"
                }
            }
        }!!

    override fun fetchPageList(chapter: SChapter) =
        apiClient.newCall(pageListRequest(chapter)).asObservableSuccess().map { res ->
            res.parse<Data<PageList>>().single()
                .mapIndexed { idx, img -> Page(idx, "", img) }
        }!!

    override fun getFilterList() = FilterList(
        ZinChanGenre.Companion.Note,
        ZinChanGenre(ZinChanGenre.genres),
    )

    private inline val SManga.id: String
        get() = url.substringAfter("?id=")

    private fun fetchManga(request: okhttp3.Request, page: Int) =
        apiClient.newCall(request).asObservableSuccess().map { res ->
            res.parse<SeriesList>().run {
                val manga = map {
                    SManga.create().apply {
                        url = it.url
                        title = it.title
                        thumbnail_url = it.cover
                        description = it.description
                        genre = it.genres
                        author = it.authors
                        artist = author
                        status = when (it.status) {
                            "on-going" -> SManga.ONGOING
                            else -> SManga.UNKNOWN
                        }
                    }
                }
                MangasPage(manga, pages != page)
            }
        }!!

    private inline fun <reified T> Response.parse() =
        json.decodeFromString<T>(body.string())

    override fun latestUpdatesParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    override fun popularMangaParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    override fun searchMangaParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    override fun mangaDetailsParse(response: Response): SManga =
        throw UnsupportedOperationException("Not used")

    override fun chapterListParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    override fun pageListParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    override fun imageUrlParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    companion object {
        private const val API_URL = "https://api.zinchanmanga.net:5555/api/web/manga"
    }
}
