package eu.kanade.tachiyomi.extension.en.toptoonplus

import android.util.Base64
import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
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
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class TopToonPlus : HttpSource() {

    override val name = "TOPTOON+"

    override val baseUrl = "https://toptoonplus.com"

    override val lang = "en"

    override val supportsLatest = true

    override val client: OkHttpClient = network.client.newBuilder()
        .addInterceptor(TopToonPlusTokenInterceptor(baseUrl, headersBuilder().build()))
        .addInterceptor(TopToonPlusViewerInterceptor(baseUrl, headersBuilder().build()))
        .addInterceptor(RateLimitInterceptor(2, 1, TimeUnit.SECONDS))
        .build()

    private val json: Json by injectLazy()

    private val day: String
        get() = Calendar.getInstance()
            .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US)!!
            .toUpperCase(Locale.US)

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Origin", baseUrl)
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Language", lang)
            .add("UA", "web")
            .add("X-Api-Key", API_KEY)
            .build()

        return GET("$API_URL/api/v1/page/ranking", newHeaders, CacheControl.FORCE_NETWORK)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val result = response.parseAs<TopToonRanking>()

        if (result.data == null) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        val comicList = result.data.ranking.map(::popularMangaFromObject)

        return MangasPage(comicList, hasNextPage = false)
    }

    private fun popularMangaFromObject(comic: TopToonComic) = SManga.create().apply {
        title = comic.information?.title.orEmpty()
        thumbnail_url = comic.firstAvailableThumbnail
        url = "/comic/${comic.comicId}"
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Language", lang)
            .add("UA", "web")
            .add("X-Api-Key", API_KEY)
            .build()

        return GET("$API_URL/api/v1/page/daily/$day", newHeaders, CacheControl.FORCE_NETWORK)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = response.parseAs<TopToonDaily>()

        if (result.data == null) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        val comicList = result.data.daily.map(::latestMangaFromObject)

        return MangasPage(comicList, hasNextPage = false)
    }

    private fun latestMangaFromObject(comic: TopToonComic) = popularMangaFromObject(comic)

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return super.fetchSearchManga(page, query, filters)
            .map { result ->
                val filteredList = result.mangas.filter { it.title.contains(query, true) }
                MangasPage(filteredList, result.hasNextPage)
            }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Language", lang)
            .add("UA", "web")
            .add("X-Api-Key", API_KEY)
            .build()

        return GET("$API_URL/api/v1/search/totalsearch", newHeaders, CacheControl.FORCE_NETWORK)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        if (response.request.url.toString().contains("ranking")) {
            return popularMangaParse(response)
        }

        val result = response.parseAs<List<TopToonComic>>()

        if (result.data == null) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        val comicList = result.data.map(::searchMangaFromObject)

        return MangasPage(comicList, hasNextPage = false)
    }

    private fun searchMangaFromObject(comic: TopToonComic) = popularMangaFromObject(comic)

    // Workaround to allow "Open in browser" use the real URL.
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(mangaDetailsApiRequest(manga.url))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }

    private fun mangaDetailsApiRequest(mangaUrl: String): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Language", lang)
            .add("UA", "web")
            .add("X-Api-Key", API_KEY)
            .build()

        val comicId = mangaUrl.substringAfterLast("/")

        return GET("$API_URL/api/v1/page/episode?comicId=$comicId", newHeaders)
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        val newHeaders = headersBuilder()
            .removeAll("Accept")
            .build()

        return GET(baseUrl + manga.url, newHeaders)
    }

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val result = response.parseAs<TopToonDetails>()

        if (result.data == null) {
            throw Exception(COULD_NOT_PARSE_RESPONSE)
        }

        val comic = result.data.comic!!

        title = comic.information?.title.orEmpty()
        thumbnail_url = comic.firstAvailableThumbnail
        description = comic.information?.description
        genre = comic.genres
        status = if (result.data.isCompleted) SManga.COMPLETED else SManga.ONGOING
        author = comic.author.joinToString { it.trim() }
    }

    override fun chapterListRequest(manga: SManga): Request = mangaDetailsApiRequest(manga.url)

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = response.parseAs<TopToonDetails>()

        if (result.data == null) {
            throw Exception(COULD_NOT_PARSE_RESPONSE)
        }

        return result.data.availableEpisodes
            .map(::chapterFromObject)
            .reversed()
    }

    private fun chapterFromObject(chapter: TopToonEpisode): SChapter = SChapter.create().apply {
        name = chapter.information?.title.orEmpty() +
            (if (chapter.information?.subTitle.isNullOrEmpty().not()) " - " + chapter.information?.subTitle else "")
        chapter_number = chapter.order.toFloat()
        scanlator = this@TopToonPlus.name
        date_upload = chapter.information?.publishedAt?.date.orEmpty().toDate()
        url = "/comic/${chapter.comicId}/${chapter.episodeId}"
    }

    override fun pageListRequest(chapter: SChapter): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Language", lang)
            .add("UA", "web")
            .add("X-Api-Key", API_KEY)
            .build()

        val comicId = chapter.url
            .substringAfter("/comic/")
            .substringBefore("/")
        val episodeId = chapter.url.substringAfterLast("/")

        val apiUrl = "$API_URL/check/isUsableEpisode".toHttpUrl().newBuilder()
            .addQueryParameter("comicId", comicId)
            .addQueryParameter("episodeId", episodeId)
            .addQueryParameter("location", "episode")
            .addQueryParameter("action", "episode_click")
            .toString()

        return GET(apiUrl, newHeaders, CacheControl.FORCE_NETWORK)
    }

    override fun pageListParse(response: Response): List<Page> {
        val result = response.parseAs<TopToonUsableEpisode>()

        if (result.data == null) {
            throw Exception(COULD_NOT_PARSE_RESPONSE)
        }

        val usableEpisode = result.data

        if (usableEpisode.isFree.not() && usableEpisode.isOwn.not()) {
            throw Exception(CHAPTER_NOT_FREE)
        }

        val viewerRequest = viewerRequest(usableEpisode.comicId, usableEpisode.episodeId)
        val viewerResponse = client.newCall(viewerRequest).execute()

        if (!viewerResponse.isSuccessful) {
            throw Exception(COULD_NOT_GET_CHAPTER_IMAGES)
        }

        val viewerResult = viewerResponse.parseAs<TopToonDetails>()

        return viewerResult.data!!.episode
            .find { episode -> episode.episodeId == usableEpisode.episodeId }
            .let { episode -> episode?.contentImage?.jpeg.orEmpty() }
            .mapIndexed { i, page -> Page(i, baseUrl, page.path) }
    }

    private fun viewerRequest(comicId: Int, episodeId: Int): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Language", lang)
            .add("UA", "web")
            .add("X-Api-Key", API_KEY)
            .build()

        val apiUrl = "$API_URL/api/v1/page/viewer".toHttpUrl().newBuilder()
            .addQueryParameter("comicId", comicId.toString())
            .addQueryParameter("episodeId", episodeId.toString())
            .toString()

        return GET(apiUrl, newHeaders)
    }

    override fun fetchImageUrl(page: Page): Observable<String> = Observable.just(page.imageUrl!!)

    override fun imageUrlParse(response: Response): String = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .set("Accept", ACCEPT_IMAGE)
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private inline fun <reified T> Response.parseAs(): TopToonResult<T> = use {
        json.decodeFromString(it.body?.string().orEmpty())
    }

    private fun String.toDate(): Long {
        return runCatching { DATE_FORMATTER.parse(this)?.time }
            .getOrNull() ?: 0L
    }

    companion object {
        const val API_URL = "https://api.toptoonplus.com"

        private val API_KEY by lazy {
            Base64.decode("U1VQRVJDT09MQVBJS0VZMjAyMSNAIyg=", Base64.DEFAULT)
                .toString(charset("UTF-8"))
        }

        private const val ACCEPT_JSON = "application/json, text/plain, */*"
        private const val ACCEPT_IMAGE = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"

        private const val COULD_NOT_PARSE_RESPONSE = "Could not parse the API response."
        private const val COULD_NOT_GET_CHAPTER_IMAGES = "Could not get the chapter images."
        private const val CHAPTER_NOT_FREE = "This chapter is not free to read."

        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        }
    }
}
