package eu.kanade.tachiyomi.extension.all.pixiv

import android.util.LruCache
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

class Pixiv(override val lang: String) : HttpSource() {
    override val name = "Pixiv"
    override val baseUrl = "https://www.pixiv.net"
    override val supportsLatest = true

    private val siteLang: String = if (lang == "all") "ja" else lang
    private val illustCache by lazy { LruCache<String, PixivIllust>(50) }

    private val json: Json by injectLazy()
    private val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH) }

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", "$baseUrl/")
        .add("Accept-Language", siteLang)

    private fun apiRequest(method: String, path: String, params: Map<String, String> = emptyMap()) = Request(
        url = baseUrl.toHttpUrl().newBuilder()
            .addEncodedPathSegments("ajax$path")
            .addEncodedQueryParameter("lang", siteLang)
            .apply { params.forEach { (k, v) -> addEncodedQueryParameter(k, v) } }
            .build(),

        headers = headersBuilder().add("Accept", "application/json").build(),
        method = method,
    )

    private inline fun <reified T> apiResponseParse(response: Response): T {
        if (!response.isSuccessful) {
            throw Exception(response.message)
        }

        return response.body.string()
            .let { json.decodeFromString<PixivApiResponse<T>>(it) }
            .apply { if (error) throw Exception(message ?: response.message) }
            .let { it.body!! }
    }

    private fun illustUrlToId(url: String): String =
        url.substringAfterLast("/")

    private fun urlEncode(string: String): String =
        URLEncoder.encode(string, "UTF-8").replace("+", "%20")

    private fun parseTimestamp(string: String) =
        runCatching { dateFormat.parse(string)?.time!! }.getOrDefault(0)

    private fun parseSearchResult(result: PixivSearchResult) = SManga.create().apply {
        url = "/artworks/${result.id!!}"
        title = result.title ?: ""
        thumbnail_url = result.url
    }

    private fun fetchIllust(url: String): Observable<PixivIllust> =
        Observable.fromCallable { illustCache.get(url) }.filter { it != null }.switchIfEmpty(
            Observable.defer {
                client.newCall(illustRequest(url)).asObservable()
                    .map { illustParse(it) }
                    .doOnNext { illustCache.put(url, it) }
            },
        )

    private fun illustRequest(url: String): Request =
        apiRequest("GET", "/illust/${illustUrlToId(url)}")

    private fun illustParse(response: Response): PixivIllust =
        apiResponseParse(response)

    override fun popularMangaRequest(page: Int): Request =
        searchMangaRequest(page, "", FilterList())

    override fun popularMangaParse(response: Response) = MangasPage(
        mangas = apiResponseParse<PixivSearchResults>(response)
            .popular?.run { recent.orEmpty() + permanent.orEmpty() }
            ?.map(::parseSearchResult)
            .orEmpty(),

        hasNextPage = false,
    )

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val word = urlEncode(query.ifBlank { "漫画" })

        val parameters = mutableMapOf(
            "word" to query,
            "order" to "date_d",
            "mode" to "all",
            "p" to page.toString(),
            "s_mode" to "s_tag_full",
            "type" to "manga",
        )

        filters.forEach { filter ->
            when (filter) {
                is FilterType -> parameters["type"] = filter.value
                is FilterRating -> parameters["mode"] = filter.value
                is FilterSearchMode -> parameters["s_mode"] = filter.value
                is FilterOrder -> parameters["order"] = filter.value
                is FilterDateBefore -> filter.value?.let { parameters["ecd"] = it }
                is FilterDateAfter -> filter.value?.let { parameters["scd"] = it }
                else -> {}
            }
        }

        val endpoint = when (parameters["type"]) {
            "all" -> "artworks"
            "illust" -> "illustrations"
            "manga" -> "manga"
            else -> ""
        }

        return apiRequest("GET", "/search/$endpoint/$word", parameters)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val mangas = apiResponseParse<PixivSearchResults>(response)
            .run { illustManga ?: illust ?: manga }?.data
            ?.filter { it.isAdContainer != true }
            ?.map(::parseSearchResult)
            .orEmpty()

        return MangasPage(mangas, hasNextPage = mangas.isNotEmpty())
    }

    override fun latestUpdatesRequest(page: Int): Request =
        searchMangaRequest(page, "", FilterList())

    override fun latestUpdatesParse(response: Response): MangasPage =
        searchMangaParse(response)

    override fun mangaDetailsRequest(manga: SManga): Request =
        illustRequest(manga.url)

    override fun mangaDetailsParse(response: Response) = SManga.create().apply {
        val illust = illustParse(response)

        url = "/artworks/${illust.id!!}"
        title = illust.title ?: ""
        artist = illust.userName
        author = illust.userName
        description = illust.description?.let { Jsoup.parseBodyFragment(it).wholeText() }
        genre = illust.tags?.tags?.mapNotNull { it.tag }?.joinToString()
        thumbnail_url = illust.urls?.thumb
        update_strategy = UpdateStrategy.ONLY_FETCH_ONCE
    }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> =
        fetchIllust(manga.url).map { illust ->
            listOf(
                SChapter.create().apply {
                    url = manga.url
                    name = "Oneshot"
                    date_upload = illust.uploadDate?.let(::parseTimestamp) ?: 0
                    chapter_number = 0F
                },
            )
        }

    override fun chapterListRequest(manga: SManga): Request =
        throw IllegalStateException("Not used")

    override fun chapterListParse(response: Response): List<SChapter> =
        throw IllegalStateException("Not used")

    override fun pageListRequest(chapter: SChapter): Request =
        apiRequest("GET", "/illust/${illustUrlToId(chapter.url)}/pages")

    override fun pageListParse(response: Response): List<Page> =
        apiResponseParse<List<PixivPage>>(response)
            .mapIndexed { i, it -> Page(index = i, imageUrl = it.urls?.original) }

    override fun imageUrlRequest(page: Page): Request =
        throw IllegalStateException("Not used")

    override fun imageUrlParse(response: Response): String =
        throw IllegalStateException("Not used")

    override fun getMangaUrl(manga: SManga): String =
        baseUrl + manga.url

    override fun getChapterUrl(chapter: SChapter): String =
        baseUrl + chapter.url

    override fun getFilterList() = FilterList(
        listOf(
            FilterType(),
            FilterRating(),
            FilterSearchMode(),
            FilterOrder(),
            FilterDateBefore(),
            FilterDateAfter(),
        ),
    )
}
