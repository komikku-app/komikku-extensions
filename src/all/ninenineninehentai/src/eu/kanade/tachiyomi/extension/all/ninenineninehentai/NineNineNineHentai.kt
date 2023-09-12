package eu.kanade.tachiyomi.extension.all.ninenineninehentai

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.extension.all.ninenineninehentai.Url.Companion.toAbsUrl
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimit
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
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

open class NineNineNineHentai(
    final override val lang: String,
    private val siteLang: String = lang,
) : HttpSource(), ConfigurableSource {

    override val name = "999Hentai"

    override val baseUrl = "https://999hentai.net"

    private val apiUrl = "https://api.newsmama.top/api"

    override val supportsLatest = true

    private val json: Json by injectLazy()

    override val client = network.cloudflareClient.newBuilder()
        .rateLimit(1)
        .build()

    private val preference by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override fun popularMangaRequest(page: Int): Request {
        val payload = GraphQL(
            PopularVariables(size, page, 1, siteLang),
            POPULAR_QUERY,
        )

        val requestBody = payload.toJsonRequestBody()

        val apiHeaders = headersBuilder().buildApiHeaders(requestBody)

        return POST(apiUrl, apiHeaders, requestBody)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val res = response.parseAs<ApiPopularResponse>()
        val mangas = res.data.popular.edges
        val dateMap = preference.dateMap
        val entries = mangas.map { manga ->
            manga.uploadDate?.let { dateMap[manga.id] = it }
            manga.toSManga()
        }
        preference.dateMap = dateMap
        val hasNextPage = mangas.size == size

        return MangasPage(entries, hasNextPage)
    }

    override fun latestUpdatesRequest(page: Int) = searchMangaRequest(page, "", FilterList())

    override fun latestUpdatesParse(response: Response) = searchMangaParse(response)

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return if (query.startsWith(SEARCH_PREFIX)) {
            val mangaId = query.substringAfter(SEARCH_PREFIX)
            client.newCall(mangaFromIDRequest(mangaId))
                .asObservableSuccess()
                .map(::searchMangaFromIDParse)
        } else {
            super.fetchSearchManga(page, query, filters)
        }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val payload = GraphQL(
            SearchVariables(
                size = size,
                page = page,
                search = SearchPayload(
                    query = query.trim().takeUnless { it.isEmpty() },
                    language = siteLang,
                    sortBy = filters.firstInstanceOrNull<SortFilter>()?.selected,
                    format = filters.firstInstanceOrNull<FormatFilter>()?.selected,
                    tags = filters.firstInstanceOrNull<IncludedTagFilter>()?.tags,
                    excludeTags = filters.firstInstanceOrNull<ExcludedTagFilter>()?.tags,
                    pagesRangeStart = filters.firstInstanceOrNull<MinPageFilter>()?.value,
                    pagesRangeEnd = filters.firstInstanceOrNull<MaxPageFilter>()?.value,
                ),
            ),
            SEARCH_QUERY,
        )

        val requestBody = payload.toJsonRequestBody()

        val apiHeaders = headersBuilder().buildApiHeaders(requestBody)

        return POST(apiUrl, apiHeaders, requestBody)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val res = response.parseAs<ApiSearchResponse>()
        val mangas = res.data.search.edges
        val dateMap = preference.dateMap
        val entries = mangas.map { manga ->
            manga.uploadDate?.let { dateMap[manga.id] = it }
            manga.toSManga()
        }
        preference.dateMap = dateMap
        val hasNextPage = mangas.size == size

        return MangasPage(entries, hasNextPage)
    }

    override fun getFilterList() = getFilters()

    private fun mangaFromIDRequest(id: String): Request {
        val payload = GraphQL(
            IdVariables(id),
            DETAILS_QUERY,
        )

        val requestBody = payload.toJsonRequestBody()

        val apiHeaders = headersBuilder().buildApiHeaders(requestBody)

        return POST(apiUrl, apiHeaders, requestBody)
    }

    private fun searchMangaFromIDParse(response: Response): MangasPage {
        val res = response.parseAs<ApiDetailsResponse>()

        val manga = res.data.details
            .takeIf { it.language == siteLang || lang == "all" }
            ?.let { manga ->
                preference.dateMap = preference.dateMap.also { dateMap ->
                    manga.uploadDate?.let { dateMap[manga.id] = it }
                }
                manga.toSManga()
            }

        return MangasPage(listOfNotNull(manga), false)
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        return mangaFromIDRequest(manga.url)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val res = response.parseAs<ApiDetailsResponse>()
        val manga = res.data.details

        preference.dateMap = preference.dateMap.also { dateMap ->
            manga.uploadDate?.let { dateMap[manga.id] = it }
        }

        return manga.toSManga()
    }

    override fun getMangaUrl(manga: SManga) = "$baseUrl/hchapter/${manga.url}"

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val group = manga.description
            ?.substringAfter("Group:", "")
            ?.substringBefore("\n")
            ?.trim()
            ?.takeUnless { it.isEmpty() }

        return Observable.just(
            listOf(
                SChapter.create().apply {
                    name = "Chapter"
                    url = manga.url
                    date_upload = preference.dateMap[manga.url].parseDate()
                    scanlator = group
                },
            ),
        )
    }

    override fun getChapterUrl(chapter: SChapter) = "$baseUrl/hchapter/${chapter.url}"

    override fun pageListRequest(chapter: SChapter): Request {
        val payload = GraphQL(
            IdVariables(chapter.url),
            PAGES_QUERY,
        )

        val requestBody = payload.toJsonRequestBody()

        val apiHeaders = headersBuilder().buildApiHeaders(requestBody)

        return POST(apiUrl, apiHeaders, requestBody)
    }

    override fun pageListParse(response: Response): List<Page> {
        val res = response.parseAs<ApiPageListResponse>()

        val pages = res.data.chapter.pages?.firstOrNull()
            ?: return emptyList()

        val cdn = pages.urlPart.toAbsUrl()

        val selectedImages = when (preference.getString(PREF_IMG_QUALITY_KEY, "original")) {
            "medium" -> pages.qualityMedium?.mapIndexed { i, it ->
                it ?: pages.qualityOriginal[i]
            }
            else -> pages.qualityOriginal
        } ?: pages.qualityOriginal

        return selectedImages.mapIndexed { index, image ->
            Page(index, "", "$cdn/${image.url}")
        }
    }

    private inline fun <reified T> String.parseAs(): T =
        json.decodeFromString(this)

    private inline fun <reified T> Response.parseAs(): T =
        use { body.string() }.parseAs()

    private inline fun <reified T> List<*>.firstInstanceOrNull(): T? =
        filterIsInstance<T>().firstOrNull()

    private inline fun <reified T : Any> T.toJsonRequestBody(): RequestBody =
        json.encodeToString(this)
            .toRequestBody(JSON_MEDIA_TYPE)

    private fun Headers.Builder.buildApiHeaders(requestBody: RequestBody) = this
        .add("Content-Length", requestBody.contentLength().toString())
        .add("Content-Type", requestBody.contentType().toString())
        .build()

    private fun String?.parseDate(): Long {
        return runCatching {
            dateFormat.parse(this!!.trim())!!.time
        }.getOrDefault(0L)
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = PREF_IMG_QUALITY_KEY
            title = "Default Image Quality"
            entries = arrayOf("Original", "Medium")
            entryValues = arrayOf("original", "medium")
            setDefaultValue("original")
            summary = "%s"
        }.also(screen::addPreference)
    }

    private var SharedPreferences.dateMap: MutableMap<String, String>
        get() {
            val jsonMap = getString(PREF_DATE_MAP_KEY, "{}")!!
            val dateMap = runCatching { jsonMap.parseAs<MutableMap<String, String>>() }
            return dateMap.getOrDefault(mutableMapOf())
        }

        @SuppressLint("ApplySharedPref")
        set(dateMap) {
            edit()
                .putString(PREF_DATE_MAP_KEY, json.encodeToString(dateMap))
                .commit()
        }

    override fun chapterListParse(response: Response) = throw UnsupportedOperationException("Not Used")
    override fun imageUrlParse(response: Response) = throw UnsupportedOperationException("Not Used")

    companion object {
        private const val size = 20
        const val SEARCH_PREFIX = "id:"

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaTypeOrNull()
        private val dateFormat by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
        }

        private const val PREF_DATE_MAP_KEY = "pref_date_map"
        private const val PREF_IMG_QUALITY_KEY = "pref_image_quality"
    }
}
