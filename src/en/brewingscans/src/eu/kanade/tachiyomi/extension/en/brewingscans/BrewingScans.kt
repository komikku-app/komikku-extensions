package eu.kanade.tachiyomi.extension.en.brewingscans

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
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

class BrewingScans : HttpSource() {
    override val lang = "en"

    override val name = "Brewing Scans"

    override val baseUrl = "https://www.brewingscans.com"

    override val supportsLatest = false

    private val json by injectLazy<Json>()

    override fun popularMangaRequest(page: Int) =
        GET("$baseUrl/api/series", headers)

    // Request the frontend URL for the webview
    override fun mangaDetailsRequest(manga: SManga) =
        GET("$baseUrl/menu/${manga.url}", headers)

    override fun chapterListRequest(manga: SManga) =
        GET("$baseUrl/api/series/${manga.url}", headers)

    override fun pageListRequest(chapter: SChapter) =
        GET("$baseUrl/api/series/${chapter.url}", headers)

    override fun popularMangaParse(response: Response) =
        response.toMangasPage { sortedByDescending { it.view_count } }

    override fun pageListParse(response: Response) =
        json.decodeFromString<List<String>>(response.body.string())
            .mapIndexed { idx, url -> Page(idx, "", url) }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList) =
        client.newCall(popularMangaRequest(page)).asObservableSuccess().map { res ->
            res.toMangasPage { filter { it.title.contains(query, true) } }
        }!!

    override fun fetchMangaDetails(manga: SManga) =
        client.newCall(chapterListRequest(manga)).asObservableSuccess().map { res ->
            val series = json.decodeFromString<BrewingSeries>(res.body.string())
            manga.description = series.description
            manga.author = series.author
            manga.artist = series.artist
            manga.genre = series.genres?.joinToString()
            manga.status = SManga.UNKNOWN
            manga.initialized = true
            return@map manga
        }!!

    override fun fetchChapterList(manga: SManga) =
        client.newCall(chapterListRequest(manga)).asObservableSuccess().map { res ->
            json.decodeFromString<BrewingSeries>(res.body.string()).chapters.map {
                SChapter.create().apply {
                    url = "${manga.url}/chapter/${it.key}"
                    chapter_number = it.key.toFloat()
                    name = it.value
                }
            }
        }!!

    private inline fun Response.toMangasPage(
        crossinline func: Collection<BrewingSeries>.() -> List<BrewingSeries>,
    ) =
        json.decodeFromString<Map<String, BrewingSeries>>(body.string())
            .values.func().map {
                SManga.create().apply {
                    url = it.id!!
                    thumbnail_url = it.cover
                    title = it.title
                }
            }.let { MangasPage(it, false) }

    override fun latestUpdatesRequest(page: Int) =
        throw UnsupportedOperationException("Not used!")

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        throw UnsupportedOperationException("Not used!")

    override fun latestUpdatesParse(response: Response) =
        throw UnsupportedOperationException("Not used!")

    override fun searchMangaParse(response: Response) =
        throw UnsupportedOperationException("Not used!")

    override fun mangaDetailsParse(response: Response) =
        throw UnsupportedOperationException("Not used!")

    override fun chapterListParse(response: Response) =
        throw UnsupportedOperationException("Not used!")

    override fun imageUrlParse(response: Response) =
        throw UnsupportedOperationException("Not used!")
}
