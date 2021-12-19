package eu.kanade.tachiyomi.extension.all.bilibili

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.lib.ratelimit.SpecificHostRateLimitInterceptor
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

abstract class Bilibili(
    override val name: String,
    final override val baseUrl: String,
    override val lang: String
) : HttpSource(), ConfigurableSource {

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(::expiredTokenIntercept)
        .addInterceptor(SpecificHostRateLimitInterceptor(baseUrl.toHttpUrl(), 1))
        .addInterceptor(SpecificHostRateLimitInterceptor(CDN_URL.toHttpUrl(), 2))
        .addInterceptor(SpecificHostRateLimitInterceptor(COVER_CDN_URL.toHttpUrl(), 2))
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Accept", ACCEPT_JSON)
        .add("Origin", baseUrl)
        .add("Referer", "$baseUrl/")

    protected open val statusLabel: String = "Status"

    protected open val sortLabel: String = "Sort by"

    protected open val genreLabel: String = "Genre"

    protected open val areaLabel: String = "Area"

    protected open val priceLabel: String = "Price"

    protected open val episodePrefix: String = "Ep. "

    protected open val defaultPopularSort: Int = 1

    protected open val defaultLatestSort: Int = 2

    protected open val hasPaidChaptersWarning: String = "This series has paid chapters that " +
        "were filtered out from the chapter list. Use the BILIBILI website or the official app " +
        "to read them for now."

    protected open val imageQualityPrefTitle: String = "Chapter image quality"

    protected open val imageQualityPrefEntries: Array<String> = arrayOf("Raw", "HD", "SD")

    protected open val imageFormatPrefTitle: String = "Chapter image format"

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    private val json: Json by injectLazy()

    private val chapterImageQuality: String
        get() = preferences.getString("${IMAGE_QUALITY_PREF_KEY}_$lang", IMAGE_QUALITY_PREF_DEFAULT_VALUE)!!

    private val chapterImageFormat: String
        get() = preferences.getString("${IMAGE_FORMAT_PREF_KEY}_$lang", IMAGE_FORMAT_PREF_DEFAULT_VALUE)!!

    override fun popularMangaRequest(page: Int): Request {
        val requestPayload = buildJsonObject {
            put("area_id", -1)
            put("is_finish", -1)
            put("is_free", -1)
            put("order", defaultPopularSort)
            put("page_num", page)
            put("page_size", POPULAR_PER_PAGE)
            put("style_id", -1)
            put("style_prefer", "[]")
        }
        val requestBody = requestPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .build()

        return POST(
            "$baseUrl/$BASE_API_ENDPOINT/ClassPage?device=pc&platform=web",
            headers = newHeaders,
            body = requestBody
        )
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val result = response.parseAs<List<BilibiliComicDto>>()

        if (result.code != 0) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        val comicList = result.data!!.map(::popularMangaFromObject)
        val hasNextPage = comicList.size == POPULAR_PER_PAGE

        return MangasPage(comicList, hasNextPage)
    }

    private fun popularMangaFromObject(comic: BilibiliComicDto): SManga = SManga.create().apply {
        title = comic.title
        thumbnail_url = comic.verticalCover + THUMBNAIL_RESOLUTION
        url = "/detail/mc${comic.seasonId}"
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val requestPayload = buildJsonObject {
            put("area_id", -1)
            put("is_finish", -1)
            put("is_free", -1)
            put("order", defaultLatestSort)
            put("page_num", page)
            put("page_size", POPULAR_PER_PAGE)
            put("style_id", -1)
            put("style_prefer", "[]")
        }
        val requestBody = requestPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .build()

        return POST(
            "$baseUrl/$BASE_API_ENDPOINT/ClassPage?device=pc&platform=web",
            headers = newHeaders,
            body = requestBody
        )
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = response.parseAs<List<BilibiliComicDto>>()

        if (result.code != 0) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        val comicList = result.data!!.map(::latestMangaFromObject)
        val hasNextPage = comicList.size == POPULAR_PER_PAGE

        return MangasPage(comicList, hasNextPage)
    }

    private fun latestMangaFromObject(comic: BilibiliComicDto): SManga = SManga.create().apply {
        title = comic.title
        thumbnail_url = comic.verticalCover + THUMBNAIL_RESOLUTION
        url = "/detail/mc${comic.seasonId}"
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.startsWith(PREFIX_ID_SEARCH) && query.matches(ID_SEARCH_PATTERN)) {
            val comicId = query
                .removePrefix(PREFIX_ID_SEARCH)
                .removePrefix("mc")
            return mangaDetailsApiRequest("/detail/mc$comicId")
        }

        val order = filters.filterIsInstance<SortFilter>()
            .firstOrNull()?.state ?: 0

        val status = filters.filterIsInstance<StatusFilter>()
            .firstOrNull()?.state?.minus(1) ?: -1

        val price = filters.filterIsInstance<PriceFilter>()
            .firstOrNull()?.state ?: 0

        val styleId = filters.filterIsInstance<GenreFilter>()
            .firstOrNull()?.selected?.id ?: -1

        val areaId = filters.filterIsInstance<AreaFilter>()
            .firstOrNull()?.selected?.id ?: -1

        val pageSize = if (query.isBlank()) POPULAR_PER_PAGE else SEARCH_PER_PAGE

        val jsonPayload = buildJsonObject {
            put("area_id", areaId)
            put("is_finish", status)
            put("is_free", if (price == 0) -1 else price)
            put("order", order)
            put("page_num", page)
            put("page_size", pageSize)
            put("style_id", styleId)
            put("style_prefer", "[]")

            if (query.isNotBlank()) {
                put("need_shield_prefer", true)
                put("key_word", query)
            }
        }
        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
        Log.d("Bilibili", jsonPayload.toString())

        val refererUrl = if (query.isBlank()) "$baseUrl/genre" else
            "$baseUrl/search".toHttpUrl().newBuilder()
                .addQueryParameter("keyword", query)
                .toString()
        val newHeaders = headersBuilder()
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .set("Referer", refererUrl)
            .build()

        val apiPath = if (query.isBlank()) "ClassPage" else "Search"

        return POST(
            "$baseUrl/$BASE_API_ENDPOINT/$apiPath?device=pc&platform=web",
            headers = newHeaders,
            body = requestBody
        )
    }

    override fun searchMangaParse(response: Response): MangasPage {
        if (response.request.url.toString().contains("ComicDetail")) {
            val comic = mangaDetailsParse(response)
            return MangasPage(listOf(comic), hasNextPage = false)
        }

        if (response.request.url.toString().contains("ClassPage")) {
            val result = response.parseAs<List<BilibiliComicDto>>()

            if (result.code != 0) {
                return MangasPage(emptyList(), hasNextPage = false)
            }

            val comicList = result.data!!.map(::searchMangaFromObject)
            val hasNextPage = comicList.size == POPULAR_PER_PAGE

            return MangasPage(comicList, hasNextPage)
        }

        val result = response.parseAs<BilibiliSearchDto>()

        if (result.code != 0) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        val comicList = result.data!!.list.map(::searchMangaFromObject)
        val hasNextPage = comicList.size == SEARCH_PER_PAGE

        return MangasPage(comicList, hasNextPage)
    }

    private fun searchMangaFromObject(comic: BilibiliComicDto): SManga = SManga.create().apply {
        title = Jsoup.parse(comic.title).text()
        thumbnail_url = comic.verticalCover + THUMBNAIL_RESOLUTION

        val comicId = if (comic.id == 0) comic.seasonId else comic.id
        url = "/detail/mc$comicId"
    }

    // Workaround to allow "Open in browser" use the real URL.
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(mangaDetailsApiRequest(manga.url))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }

    private fun mangaDetailsApiRequest(mangaUrl: String): Request {
        val comicId = mangaUrl.substringAfterLast("/mc").toInt()

        val jsonPayload = buildJsonObject { put("comic_id", comicId) }
        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .set("Referer", baseUrl + mangaUrl)
            .build()

        return POST(
            "$baseUrl/$BASE_API_ENDPOINT/ComicDetail?device=pc&platform=web",
            headers = newHeaders,
            body = requestBody
        )
    }

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val result = response.parseAs<BilibiliComicDto>()
        val comic = result.data!!

        title = comic.title
        author = comic.authorName.joinToString()
        status = if (comic.isFinish == 1) SManga.COMPLETED else SManga.ONGOING
        genre = comic.styles.joinToString()
        description = comic.classicLines
        thumbnail_url = comic.verticalCover + THUMBNAIL_RESOLUTION
        url = "/detail/mc" + comic.id

        if (comic.episodeList.any { episode -> episode.payMode == 1 && episode.payGold > 0 }) {
            description += "\n\n$hasPaidChaptersWarning"
        }
    }

    // Chapters are available in the same url of the manga details.
    override fun chapterListRequest(manga: SManga): Request = mangaDetailsApiRequest(manga.url)

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = response.parseAs<BilibiliComicDto>()

        if (result.code != 0)
            return emptyList()

        return result.data!!.episodeList
            .filter { episode -> episode.payMode == 0 && episode.payGold == 0 }
            .map { ep -> chapterFromObject(ep, result.data.id) }
    }

    private fun chapterFromObject(episode: BilibiliEpisodeDto, comicId: Int): SChapter = SChapter.create().apply {
        name = episodePrefix + episode.shortTitle +
            (if (episode.title.isNotBlank()) " - " + episode.title else "")
        date_upload = episode.publicationTime.substringBefore("T").toDate()
        url = "/mc$comicId/${episode.id}"
    }

    override fun pageListRequest(chapter: SChapter): Request {
        val chapterId = chapter.url.substringAfterLast("/").toInt()

        val jsonPayload = buildJsonObject {
            put("credential", "")
            put("ep_id", chapterId)
        }
        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .set("Referer", baseUrl + chapter.url)
            .build()

        return POST(
            "$baseUrl/$BASE_API_ENDPOINT/GetImageIndex?device=pc&platform=web",
            headers = newHeaders,
            body = requestBody
        )
    }

    override fun pageListParse(response: Response): List<Page> {
        val result = response.parseAs<BilibiliReader>()

        if (result.code != 0) {
            return emptyList()
        }

        val imageQuality = chapterImageQuality
        val imageFormat = chapterImageFormat

        val imageUrls = result.data!!.images.map { "${it.path}@$imageQuality.$imageFormat" }
        val imageTokenRequest = imageTokenRequest(imageUrls)
        val imageTokenResponse = client.newCall(imageTokenRequest).execute()
        val imageTokenResult = imageTokenResponse.parseAs<List<BilibiliPageDto>>()

        return imageTokenResult.data!!
            .mapIndexed { i, page -> Page(i, "", "${page.url}?token=${page.token}") }
    }

    protected open fun imageTokenRequest(urls: List<String>): Request {
        val jsonPayload = buildJsonObject {
            put("urls", json.encodeToString(urls))
        }
        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .build()

        return POST(
            "$baseUrl/$BASE_API_ENDPOINT/ImageToken?device=pc&platform=web",
            headers = newHeaders,
            body = requestBody
        )
    }

    override fun imageUrlParse(response: Response): String = ""

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val imageQualityPref = ListPreference(screen.context).apply {
            key = "${IMAGE_QUALITY_PREF_KEY}_$lang"
            title = imageQualityPrefTitle
            entries = imageQualityPrefEntries
            entryValues = IMAGE_QUALITY_PREF_ENTRY_VALUES
            setDefaultValue(IMAGE_QUALITY_PREF_DEFAULT_VALUE)
            summary = "%s"

            setOnPreferenceChangeListener { _, newValue ->
                val selected = newValue as String
                val index = findIndexOfValue(selected)
                val entry = entryValues[index] as String

                preferences.edit()
                    .putString("${IMAGE_QUALITY_PREF_KEY}_$lang", entry)
                    .commit()
            }
        }

        val imageFormatPref = ListPreference(screen.context).apply {
            key = "${IMAGE_FORMAT_PREF_KEY}_$lang"
            title = imageFormatPrefTitle
            entries = IMAGE_FORMAT_PREF_ENTRIES
            entryValues = IMAGE_FORMAT_PREF_ENTRY_VALUES
            setDefaultValue(IMAGE_FORMAT_PREF_DEFAULT_VALUE)
            summary = "%s"

            setOnPreferenceChangeListener { _, newValue ->
                val selected = newValue as String
                val index = findIndexOfValue(selected)
                val entry = entryValues[index] as String

                preferences.edit()
                    .putString("${IMAGE_FORMAT_PREF_KEY}_$lang", entry)
                    .commit()
            }
        }

        screen.addPreference(imageQualityPref)
        screen.addPreference(imageFormatPref)
    }

    abstract fun getAllGenres(): Array<BilibiliTag>

    protected open fun getAllAreas(): Array<BilibiliTag> = emptyArray()

    protected open fun getAllSortOptions(): Array<String> = arrayOf("Interest", "Popular", "Updated")

    protected open fun getAllStatus(): Array<String> = arrayOf("All", "Ongoing", "Completed")

    protected open fun getAllPrices(): Array<String> = arrayOf("All", "Free", "Paid")

    override fun getFilterList(): FilterList {
        val filters = mutableListOf(
            StatusFilter(statusLabel, getAllStatus()),
            SortFilter(sortLabel, getAllSortOptions(), defaultPopularSort),
            PriceFilter(priceLabel, getAllPrices()),
            GenreFilter(genreLabel, getAllGenres())
        )

        val allAreas = getAllAreas()

        if (allAreas.isNotEmpty()) {
            filters.add(AreaFilter(areaLabel, allAreas))
        }

        return FilterList(filters)
    }

    private fun expiredTokenIntercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        // Get a new image token if the current one expired.
        if (response.code == 403 && chain.request().url.toString().contains(CDN_URL)) {
            val imagePath = chain.request().url.toString()
                .substringAfter(CDN_URL)
                .substringBefore("?token=")
            val imageTokenRequest = imageTokenRequest(listOf(imagePath))
            val imageTokenResponse = chain.proceed(imageTokenRequest)
            val imageTokenResult = imageTokenResponse.parseAs<List<BilibiliPageDto>>()

            val newPage = imageTokenResult.data!![0]
            val newPageUrl = "${newPage.url}?token=${newPage.token}"

            val newRequest = imageRequest(Page(0, "", newPageUrl))

            response.close()
            return chain.proceed(newRequest)
        }

        return response
    }

    private inline fun <reified T> Response.parseAs(): BilibiliResultDto<T> = use {
        json.decodeFromString(it.body?.string().orEmpty())
    }

    private fun String.toDate(): Long {
        return runCatching { DATE_FORMATTER.parse(this)?.time }
            .getOrNull() ?: 0L
    }

    companion object {
        private const val CDN_URL = "https://manga.hdslb.com"
        private const val COVER_CDN_URL = "https://i0.hdslb.com"

        private const val BASE_API_ENDPOINT = "twirp/comic.v1.Comic"

        private const val ACCEPT_JSON = "application/json, text/plain, */*"

        private val JSON_MEDIA_TYPE = "application/json;charset=UTF-8".toMediaType()

        private const val POPULAR_PER_PAGE = 18
        private const val SEARCH_PER_PAGE = 9

        const val PREFIX_ID_SEARCH = "id:"
        private val ID_SEARCH_PATTERN = "^id:(mc)?(\\d+)$".toRegex()

        private const val IMAGE_QUALITY_PREF_KEY = "chapterImageResolution"
        private val IMAGE_QUALITY_PREF_ENTRY_VALUES = arrayOf("1200w", "800w", "600w_50q")
        private val IMAGE_QUALITY_PREF_DEFAULT_VALUE = IMAGE_QUALITY_PREF_ENTRY_VALUES[0]

        private const val IMAGE_FORMAT_PREF_KEY = "chapterImageFormat"
        private val IMAGE_FORMAT_PREF_ENTRIES = arrayOf("JPG", "WEBP", "PNG")
        private val IMAGE_FORMAT_PREF_ENTRY_VALUES = arrayOf("jpg", "webp", "png")
        private val IMAGE_FORMAT_PREF_DEFAULT_VALUE = IMAGE_FORMAT_PREF_ENTRY_VALUES[0]

        private const val THUMBNAIL_RESOLUTION = "@512w.jpg"

        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        }
    }
}
