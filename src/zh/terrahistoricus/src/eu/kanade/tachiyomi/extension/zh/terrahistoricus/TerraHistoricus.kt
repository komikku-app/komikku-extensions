package eu.kanade.tachiyomi.extension.zh.terrahistoricus

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy

class TerraHistoricus : HttpSource() {
    override val name = "泰拉记事社"
    override val lang = "zh"
    override val baseUrl = "https://terra-historicus.hypergryph.com"
    override val supportsLatest = true

    private val json: Json by injectLazy()

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/api/comic")

    override fun popularMangaParse(response: Response) = MangasPage(
        json.decodeFromString<THResult<List<THComic>>>(response.body!!.string()).data.map(THComic::toSManga),
        false
    )

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/api/recentUpdate")

    override fun latestUpdatesParse(response: Response) = MangasPage(
        json.decodeFromString<THResult<List<THRecentUpdate>>>(response.body!!.string()).data.map(THRecentUpdate::toSManga),
        false
    )

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        throw UnsupportedOperationException("没有搜索功能")

    override fun searchMangaParse(response: Response) = throw UnsupportedOperationException("没有搜索功能")

    override fun mangaDetailsParse(response: Response) =
        json.decodeFromString<THResult<THComic>>(response.body!!.string()).data.toSManga()

    override fun chapterListParse(response: Response) =
        json.decodeFromString<THResult<THComic>>(response.body!!.string()).data.toSChapterList().orEmpty()

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return client.newCall(pageListRequest(chapter))
            .asObservableSuccess()
            .map { response ->
                (0 until json.decodeFromString<THResult<THEpisode>>(response.body!!.string()).data.pageInfos!!.size).map {
                    Page(it, "$baseUrl${chapter.url}/page?pageNum=${it + 1}")
                }
            }
    }

    override fun pageListParse(response: Response) = throw UnsupportedOperationException("Not used.")

    override fun imageUrlParse(response: Response) =
        json.decodeFromString<THResult<THPage>>(response.body!!.string()).data.url
}
