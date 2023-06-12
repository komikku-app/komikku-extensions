package eu.kanade.tachiyomi.extension.en.allanime

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Locale

class AllAnime : ConfigurableSource, HttpSource() {

    override val name = "AllAnime"

    override val lang = "en"

    override val supportsLatest = true

    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        coerceInputValues = true
    }

    private val preferences: SharedPreferences =
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)

    private val domain = preferences.domainPref

    override val baseUrl = "https://$domain"

    private val apiUrl = "https://api.$domain/allanimeapi"

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor { chain ->
            val request = chain.request()
            val frag = request.url.fragment
            val quality = preferences.imageQuality

            if (frag.isNullOrEmpty() || quality == IMAGE_QUALITY_PREF_DEFAULT) {
                return@addInterceptor chain.proceed(request)
            }

            val oldUrl = request.url.toString()
            val newUrl = oldUrl.replace(imageQualityRegex, "$image_cdn/$1?w=$quality")

            return@addInterceptor chain.proceed(
                request.newBuilder()
                    .url(newUrl)
                    .build(),
            )
        }
        .rateLimitHost(apiUrl.toHttpUrl(), 1)
        .build()

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    /* Popular */
    override fun popularMangaRequest(page: Int): Request {
        val payloadObj = ApiPopularPayload(
            size = limit,
            dateRange = 0,
            page = page,
            allowAdult = preferences.allowAdult,
        )

        return apiRequest(payloadObj)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val result = response.parseAs<ApiPopularResponse>()
        val titleStyle = preferences.titlePref

        val mangaList = result.data.popular.mangas
            .mapNotNull { it.manga?.toSManga(titleStyle) }

        return MangasPage(mangaList, mangaList.size == limit)
    }

    /* Latest */
    override fun latestUpdatesRequest(page: Int) = searchMangaRequest(page, "", FilterList())

    override fun latestUpdatesParse(response: Response) = searchMangaParse(response)

    /* Search */
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        if (!query.startsWith(SEARCH_PREFIX)) {
            return super.fetchSearchManga(page, query, filters)
        }

        val url = "/manga/${query.substringAfter(SEARCH_PREFIX)}/"
        return fetchMangaDetails(SManga.create().apply { this.url = url }).map {
            MangasPage(listOf(it), false)
        }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val payloadObj = ApiSearchPayload(
            query = query,
            size = limit,
            page = page,
            genres = filters.firstInstanceOrNull<GenreFilter>()?.included,
            excludeGenres = filters.firstInstanceOrNull<GenreFilter>()?.excluded,
            translationType = "sub",
            countryOrigin = filters.firstInstanceOrNull<CountryFilter>()?.getValue() ?: "ALL",
            allowAdult = preferences.allowAdult,
        )

        return apiRequest(payloadObj)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val result = response.parseAs<ApiSearchResponse>()
        val titleStyle = preferences.titlePref

        val mangaList = result.data.mangas.mangas
            .map { it.toSManga(titleStyle) }

        return MangasPage(mangaList, mangaList.size == limit)
    }

    override fun getFilterList() = getFilters()

    /* Details */
    override fun mangaDetailsRequest(manga: SManga): Request {
        val mangaId = manga.url.split("/")[2]
        val payloadObj = ApiIDPayload(mangaId, DETAILS_QUERY)

        return apiRequest(payloadObj)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val result = response.parseAs<ApiMangaDetailsResponse>()

        return result.data.manga.toSManga(preferences.titlePref)
    }

    override fun getMangaUrl(manga: SManga): String {
        return "$baseUrl${manga.url}"
    }

    /* Chapters */
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        return client.newCall(chapterListRequest(manga))
            .asObservableSuccess()
            .map { response ->
                chapterListParse(response, manga)
            }
    }

    override fun chapterListRequest(manga: SManga): Request {
        val mangaId = manga.url.split("/")[2]
        val payloadObj = ApiIDPayload(mangaId, CHAPTERS_QUERY)

        return apiRequest(payloadObj)
    }

    private fun chapterDetailsRequest(manga: SManga, start: String, end: String): Request {
        val mangaId = manga.url.split("/")[2]
        val payloadObj = ApiChapterListDetailsPayload(mangaId, start.toFloat(), end.toFloat())

        return apiRequest(payloadObj)
    }

    private fun chapterListParse(response: Response, manga: SManga): List<SChapter> {
        val result = response.parseAs<ApiChapterListResponse>()
        val chapters = result.data.manga.chapters.sub
            ?.sortedBy { it.toFloat() }
            ?: return emptyList()

        val chapterDetails = client.newCall(
            chapterDetailsRequest(manga, chapters.first(), chapters.last()),
        ).execute()
            .use {
                it.parseAs<ApiChapterListDetailsResponse>()
            }.data.chapterList
            ?.sortedBy { it.chapterNum }

        val mangaUrl = manga.url.substringAfter("/manga/")

        return chapterDetails?.zip(chapters)?.map { (details, chapterNum) ->
            SChapter.create().apply {
                name = "Chapter $chapterNum"
                if (!details.title.isNullOrEmpty() && !details.title.contains(numberRegex)) {
                    name += ": ${details.title}"
                }
                url = "/read/$mangaUrl/chapter-$chapterNum-sub"
                date_upload = details.uploadDates?.sub.parseDate()
            }
        }?.reversed() ?: emptyList()
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        throw UnsupportedOperationException("Not used")
    }

    override fun getChapterUrl(chapter: SChapter): String {
        return "$baseUrl${chapter.url}"
    }

    /* Pages */
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return client.newCall(pageListRequest(chapter))
            .asObservableSuccess()
            .map { response ->
                pageListParse(response, chapter)
            }
    }

    override fun pageListRequest(chapter: SChapter): Request {
        val chapterUrl = chapter.url.split("/")
        val mangaId = chapterUrl[2]
        val chapterNo = chapterUrl[4].split("-")[1]

        val payloadObj = ApiPageListPayload(
            id = mangaId,
            chapterNum = chapterNo,
            translationType = "sub",
        )

        return apiRequest(payloadObj)
    }

    private fun pageListParse(response: Response, chapter: SChapter): List<Page> {
        val result = json.decodeFromString<ApiPageListResponse>(response.body.string())
        val pages = result.data.pageList?.serverList?.get(0) ?: return emptyList()

        val imageDomain = if (!pages.serverUrl.isNullOrEmpty()) {
            pages.serverUrl.let { server ->
                if (server.matches(urlRegex)) {
                    server
                } else {
                    "https://$server"
                }
            }
        } else {
            // in rare cases, the api doesn't return server url
            // for that, we try to parse the frontend html to get it
            val chapterUrl = getChapterUrl(chapter)
            val frontendRequest = GET(chapterUrl, headers)
            val url = client.newCall(frontendRequest).execute().use { frontendResponse ->
                val document = frontendResponse.asJsoup()
                val script = document.select("script:containsData(window.__NUXT__)").firstOrNull()
                imageUrlFromPageRegex.matchEntire(script.toString())
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.replace("\\u002F", "/")
                    ?.substringBeforeLast(pages.pictureUrls?.first().toString(), "")
            }
            url?.takeIf { it.isNotEmpty() } ?: return emptyList()
        }

        return pages.pictureUrls?.mapIndexed { index, image ->
            Page(
                index = index,
                imageUrl = "$imageDomain${image.url}#page",
            )
        } ?: emptyList()
    }

    override fun pageListParse(response: Response): List<Page> {
        throw UnsupportedOperationException("Not used")
    }

    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException("Not used")
    }

    /* Helpers */
    private inline fun <reified T> apiRequest(payloadObj: T): Request {
        val payload = json.encodeToString(payloadObj)
            .toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", payload.contentLength().toString())
            .add("Content-Type", payload.contentType().toString())
            .build()

        return POST(apiUrl, newHeaders, payload)
    }

    private inline fun <reified T> Response.parseAs(): T = json.decodeFromString(body.string())

    private inline fun <reified R> List<*>.firstInstanceOrNull(): R? =
        filterIsInstance<R>().firstOrNull()

    private fun String?.parseDate(): Long {
        return runCatching {
            dateFormat.parse(this!!)!!.time
        }.getOrDefault(0L)
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = DOMAIN_PREF
            title = "Preferred domain"
            entries = arrayOf("allanime.to", "allanime.co")
            entryValues = arrayOf("allanime.to", "allanime.co")
            setDefaultValue(DOMAIN_PREF_DEFAULT)
            summary = "Requires App Restart"
        }.let { screen.addPreference(it) }

        ListPreference(screen.context).apply {
            key = IMAGE_QUALITY_PREF
            title = "Image Quality"
            entries = arrayOf("Original", "Wp-800", "Wp-480")
            entryValues = arrayOf("original", "800", "480")
            setDefaultValue(IMAGE_QUALITY_PREF_DEFAULT)
            summary = "Warning: Wp quality servers can be slow and might not work sometimes"
        }.let { screen.addPreference(it) }

        ListPreference(screen.context).apply {
            key = TITLE_PREF
            title = "Preferred Title Style"
            entries = arrayOf("Romaji", "English", "Native")
            entryValues = arrayOf("romaji", "eng", "native")
            setDefaultValue(TITLE_PREF_DEFAULT)
            summary = "%s"
        }.let { screen.addPreference(it) }

        SwitchPreferenceCompat(screen.context).apply {
            key = SHOW_ADULT_PREF
            title = "Show Adult Content"
            setDefaultValue(SHOW_ADULT_PREF_DEFAULT)
        }.let { screen.addPreference(it) }
    }

    private val SharedPreferences.domainPref
        get() = getString(DOMAIN_PREF, DOMAIN_PREF_DEFAULT)!!

    private val SharedPreferences.titlePref
        get() = getString(TITLE_PREF, TITLE_PREF_DEFAULT)

    private val SharedPreferences.allowAdult
        get() = getBoolean(SHOW_ADULT_PREF, SHOW_ADULT_PREF_DEFAULT)

    private val SharedPreferences.imageQuality
        get() = getString(IMAGE_QUALITY_PREF, IMAGE_QUALITY_PREF_DEFAULT)!!

    companion object {
        private const val limit = 20
        private val numberRegex by lazy { Regex("\\d") }
        val whitespace by lazy { Regex("\\s+") }
        val dateFormat by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
        }
        const val SEARCH_PREFIX = "id:"
        const val thumbnail_cdn = "https://wp.youtube-anime.com/aln.youtube-anime.com/"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaTypeOrNull()
        val urlRegex = Regex("^https?://.*")
        private const val image_cdn = "https://wp.youtube-anime.com"
        private val imageQualityRegex = Regex("^https?://(.*)#.*")
        val titleSpecialCharactersRegex = Regex("[^a-z\\d]+")
        private val imageUrlFromPageRegex = Regex("selectedPicturesServer:\\[\\{.*?url:\"(.*?)\".*?\\}\\]")

        private const val DOMAIN_PREF = "pref_domain"
        private const val DOMAIN_PREF_DEFAULT = "allanime.to"
        private const val TITLE_PREF = "pref_title"
        private const val TITLE_PREF_DEFAULT = "romaji"
        private const val SHOW_ADULT_PREF = "pref_adult"
        private const val SHOW_ADULT_PREF_DEFAULT = false
        private const val IMAGE_QUALITY_PREF = "pref_quality"
        private const val IMAGE_QUALITY_PREF_DEFAULT = "original"
    }
}
