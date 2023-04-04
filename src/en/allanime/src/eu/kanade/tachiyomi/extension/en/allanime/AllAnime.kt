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
import eu.kanade.tachiyomi.source.model.Filter.TriState.Companion.STATE_EXCLUDE
import eu.kanade.tachiyomi.source.model.Filter.TriState.Companion.STATE_INCLUDE
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.Locale

class AllAnime : ConfigurableSource, HttpSource() {

    override val name = "AllAnime"

    override val lang = "en"

    override val supportsLatest = true

    private val json: Json by injectLazy()

    private val preferences: SharedPreferences =
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)

    private val domain = preferences.getString(DOMAIN_PREF, "allanime.to")

    override val baseUrl = "https://$domain"

    private val apiUrl = "https://api.$domain/allanimeapi"

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimitHost(apiUrl.toHttpUrl(), 1)
        .build()

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    /* Popular */
    override fun popularMangaRequest(page: Int): Request {
        val showAdult = preferences.getBoolean(SHOW_ADULT_PREF, false)

        val payload = buildJsonObject {
            putJsonObject("variables") {
                put("type", "manga")
                put("size", limit)
                put("dateRange", 0)
                put("page", page)
                put("allowAdult", showAdult)
                put("allowUnknown", false)
            }
            put("query", POPULAR_QUERY)
        }

        return apiRequest(payload)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val result = json.decodeFromString<ApiPopularResponse>(response.body.string())

        val mangaList = result.data.queryPopular.recommendations
            .mapNotNull { it.anyCard }
            .map { manga ->
                toSManga(manga)
            }

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
        val showAdult = preferences.getBoolean(SHOW_ADULT_PREF, false)
        var country = "ALL"
        val includeGenres = mutableListOf<String>()
        val excludeGenres = mutableListOf<String>()

        filters.forEach { filter ->
            when (filter) {
                is GenreFilter -> {
                    filter.state.forEach { genreState ->
                        when (genreState.state) {
                            STATE_INCLUDE -> includeGenres.add(genreState.name)
                            STATE_EXCLUDE -> excludeGenres.add(genreState.name)
                        }
                    }
                }
                is CountryFilter -> {
                    country = filter.getValue()
                }
                else -> {}
            }
        }

        val payload = buildJsonObject {
            putJsonObject("variables") {
                putJsonObject("search") {
                    if (includeGenres.isNotEmpty() || excludeGenres.isNotEmpty()) {
                        put("genres", JsonArray(includeGenres.map { JsonPrimitive(it) }))
                        put("excludeGenres", JsonArray(excludeGenres.map { JsonPrimitive(it) }))
                    }
                    if (query.isNotEmpty()) put("query", query)
                    put("allowAdult", showAdult)
                    put("allowUnknown", false)
                    put("isManga", true)
                }
                put("limit", limit)
                put("page", page)
                put("translationType", "sub")
                put("countryOrigin", country)
            }
            put("query", SEARCH_QUERY)
        }

        return apiRequest(payload)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val result = json.decodeFromString<ApiSearchResponse>(response.body.string())

        val mangaList = result.data.mangas.edges
            .map { manga ->
                toSManga(manga)
            }

        return MangasPage(mangaList, mangaList.size == limit)
    }

    override fun getFilterList() = filters

    /* Details */
    override fun mangaDetailsRequest(manga: SManga): Request {
        val mangaId = manga.url.split("/")[2]

        val payload = buildJsonObject {
            putJsonObject("variables") {
                put("_id", mangaId)
            }
            put("query", DETAILS_QUERY)
        }

        return apiRequest(payload)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val result = json.decodeFromString<ApiMangaDetailsResponse>(response.body.string())
        val manga = result.data.manga

        return toSManga(manga)
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

        val payload = buildJsonObject {
            putJsonObject("variables") {
                put("_id", mangaId)
            }
            put("query", CHAPTERS_QUERY)
        }

        return apiRequest(payload)
    }

    private fun chapterListParse(response: Response, manga: SManga): List<SChapter> {
        val result = json.decodeFromString<ApiChapterListResponse>(response.body.string())

        val chapters = result.data.manga.availableChaptersDetail.sub

        val mangaUrl = manga.url.substringAfter("/manga/")

        return chapters?.map { chapter ->
            SChapter.create().apply {
                name = "Chapter $chapter"
                url = "/read/$mangaUrl/chapter-$chapter-sub"
            }
        } ?: emptyList()
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

        val payload = buildJsonObject {
            putJsonObject("variables") {
                put("mangaId", mangaId)
                put("translationType", "sub")
                put("chapterString", chapterNo)
            }
            put("query", PAGE_QUERY)
        }

        return apiRequest(payload)
    }

    private fun pageListParse(response: Response, chapter: SChapter): List<Page> {
        val result = json.decodeFromString<ApiPageListResponse>(response.body.string())
        val pages = result.data.chapterPages?.edges?.get(0) ?: return emptyList()

        val imageDomain = if (!pages.pictureUrlHead.isNullOrEmpty()) {
            pages.pictureUrlHead.let { server ->
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
                    ?.substringBeforeLast(pages.pictureUrls.first().toString(), "")
            }
            url?.takeIf { it.isNotEmpty() } ?: return emptyList()
        }

        return pages.pictureUrls.mapIndexed { index, image ->
            Page(
                index = index,
                imageUrl = "$imageDomain${image.url}",
            )
        }
    }

    override fun pageListParse(response: Response): List<Page> {
        throw UnsupportedOperationException("Not used")
    }

    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException("Not used")
    }

    /* Helpers */
    private fun apiRequest(payload: JsonObject): Request {
        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", body.contentLength().toString())
            .add("Content-Type", body.contentType().toString())
            .build()

        return POST(apiUrl, newHeaders, body)
    }

    private fun toSManga(manga: Manga): SManga {
        val titleStyle = preferences.getString(TITLE_PREF, "romaji")!!

        return SManga.create().apply {
            title = when (titleStyle) {
                "romaji" -> manga.name
                "eng" -> manga.englishName ?: manga.name
                else -> manga.nativeName ?: manga.name
            }
            url = "/manga/${manga._id}/${manga.name.titleToSlug()}"
            thumbnail_url = manga.thumbnail.parseThumbnailUrl()
            description = Jsoup.parse(
                manga.description?.replace("<br>", "br2n") ?: "",
            ).text().replace("br2n", "\n")
            description += if (manga.altNames != null) {
                "\n\nAlternative Names: ${manga.altNames.joinToString { it.trim() }}"
            } else {
                ""
            }
            if (manga.authors?.isNotEmpty() == true) {
                author = manga.authors.first().trim()
                artist = author
            }
            genre = "${manga.genres?.joinToString { it.trim() }}, ${manga.tags?.joinToString { it.trim() }}"
            status = manga.status.parseStatus()
        }
    }

    private fun String.parseThumbnailUrl(): String {
        return if (this.matches(urlRegex)) {
            this
        } else {
            "$image_cdn$this?w=250"
        }
    }

    private fun String?.parseStatus(): Int {
        if (this == null) {
            return SManga.UNKNOWN
        }

        return when {
            this.contains("releasing", true) -> SManga.ONGOING
            this.contains("finished", true) -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
    }

    private fun String.titleToSlug() = this.trim()
        .lowercase(Locale.US)
        .replace(titleSpecialCharactersRegex, "-")

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = DOMAIN_PREF
            title = "Preferred domain"
            entries = arrayOf("allanime.to", "allanime.co")
            entryValues = arrayOf("allanime.to", "allanime.co")
            setDefaultValue("allanime.to")
            summary = "Requires App Restart"
        }.let { screen.addPreference(it) }

        ListPreference(screen.context).apply {
            key = TITLE_PREF
            title = "Preferred Title Style"
            entries = arrayOf("Romaji", "English", "Native")
            entryValues = arrayOf("romaji", "eng", "native")
            setDefaultValue("romaji")
            summary = "%s"
        }.let { screen.addPreference(it) }

        SwitchPreferenceCompat(screen.context).apply {
            key = SHOW_ADULT_PREF
            title = "Show Adult Content"
            setDefaultValue(false)
        }.let { screen.addPreference(it) }
    }

    companion object {
        private const val limit = 26
        const val SEARCH_PREFIX = "id:"
        private const val image_cdn = "https://wp.youtube-anime.com/aln.youtube-anime.com/"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaTypeOrNull()
        private val urlRegex = Regex("^https?://.*")
        private val titleSpecialCharactersRegex = Regex("[^a-z\\d]+")
        private val imageUrlFromPageRegex = Regex("selectedPicturesServer:\\[\\{.*?url:\"(.*?)\".*?\\}\\]")

        private const val DOMAIN_PREF = "pref_domain"
        private const val TITLE_PREF = "pref_title"
        private const val SHOW_ADULT_PREF = "pref_adult"
    }
}
