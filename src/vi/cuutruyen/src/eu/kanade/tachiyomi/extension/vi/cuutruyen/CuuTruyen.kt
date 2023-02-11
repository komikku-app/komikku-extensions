package eu.kanade.tachiyomi.extension.vi.cuutruyen

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.extension.vi.cuutruyen.dto.ChapterDto
import eu.kanade.tachiyomi.extension.vi.cuutruyen.dto.MangaDto
import eu.kanade.tachiyomi.extension.vi.cuutruyen.dto.ResponseDto
import eu.kanade.tachiyomi.network.GET
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
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class CuuTruyen : HttpSource(), ConfigurableSource {

    override val name = "Cứu Truyện"

    override val lang = "vi"

    override val baseUrl = "https://cuutruyen.net"
    private val apiUrl = "https://kakarot.cuutruyen.net/api/v2"

    override val supportsLatest = true

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    private val json: Json by injectLazy()

    override fun headersBuilder() = super.headersBuilder().add("Referer", "$baseUrl/")

    override val client = network.client.newBuilder()
        .rateLimit(3)
        .addInterceptor(CuuTruyenImageInterceptor())
        .build()

    override fun popularMangaRequest(page: Int): Request {
        val url = apiUrl.toHttpUrl().newBuilder().apply {
            addPathSegments("mangas/top")
            addQueryParameter("duration", "all")
            addQueryParameter("page", page.toString())
            addQueryParameter("per_page", "24")
        }.build().toString()
        return GET(url, headers = headers, cache = CacheControl.FORCE_NETWORK)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        if (response.code == 500) {
            return MangasPage(emptyList(), false)
        }

        val responseDto = response.parseAs<ResponseDto<List<MangaDto>>>()
        val hasMoreResults = responseDto.metadata!!.currentPage < responseDto.metadata.totalPages

        val coverKey = preferences.coverQuality
        return MangasPage(
            responseDto.data.map { it.toSManga(coverKey) },
            hasMoreResults,
        )
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val url = apiUrl.toHttpUrl().newBuilder().apply {
            addPathSegments("mangas/recently_updated")
            addQueryParameter("page", page.toString())
            addQueryParameter("per_page", "24")
        }.build().toString()
        return GET(url, headers = headers, cache = CacheControl.FORCE_NETWORK)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return when {
            query.startsWith(PREFIX_ID_SEARCH) -> {
                val id = query.removePrefix(PREFIX_ID_SEARCH).trim()
                if (id.toIntOrNull() == null) {
                    throw Exception("ID tìm kiếm không hợp lệ (phải là một số).")
                }
                val url = "/mangas/$id"
                fetchMangaDetails(
                    SManga.create().apply {
                        this.url = url
                    },
                )
                    .map {
                        it.url = url
                        MangasPage(listOf(it), false)
                    }
            }
            else -> super.fetchSearchManga(page, query, filters)
        }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = apiUrl.toHttpUrl().newBuilder().apply {
            addPathSegments("mangas/search")
            addQueryParameter("q", query)
            addQueryParameter("page", page.toString())
            addQueryParameter("per_page", "24")
        }.build().toString()
        return GET(url, headers = headers, cache = CacheControl.FORCE_NETWORK)
    }

    override fun searchMangaParse(response: Response) = popularMangaParse(response)

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> =
        client.newCall(GET("$apiUrl${manga.url}", headers = headers, cache = CacheControl.FORCE_NETWORK))
            .asObservableSuccess()
            .map { mangaDetailsParse(it) }

    override fun mangaDetailsRequest(manga: SManga): Request = GET("$baseUrl${manga.url}")

    override fun mangaDetailsParse(response: Response): SManga {
        val responseDto = response.parseAs<ResponseDto<MangaDto>>()
        return responseDto.data.toSManga(preferences.coverQuality)
    }

    override fun chapterListRequest(manga: SManga): Request =
        GET("$apiUrl${manga.url}/chapters", headers = headers, cache = CacheControl.FORCE_NETWORK)

    override fun chapterListParse(response: Response): List<SChapter> {
        val segments = response.request.url.pathSegments
        val lastIndex = segments.lastIndex
        val mangaUrl = "/${segments[lastIndex - 2]}/${segments[lastIndex - 1]}"
        return response.parseAs<ResponseDto<List<ChapterDto>>>().data.map { it.toSChapter(mangaUrl) }
    }

    override fun pageListRequest(chapter: SChapter): Request {
        val url = apiUrl.toHttpUrl().newBuilder().apply {
            val chapterId = chapter.url.split("/").last()
            addPathSegment("chapters")
            addPathSegment(chapterId)
        }.build().toString()
        return GET(url, headers = headers, cache = CacheControl.FORCE_NETWORK)
    }

    override fun pageListParse(response: Response): List<Page> {
        val chapterDto = response.parseAs<ResponseDto<ChapterDto>>()
        return chapterDto.data.pages!!.map { it.toPage() }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException("Not used")

    private inline fun <reified T> Response.parseAs(): T = use {
        json.decodeFromString(body.string())
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val coverQualityPref = ListPreference(screen.context).apply {
            key = "coverQuality"
            title = "Chất lượng ảnh bìa"
            entries = arrayOf("Chất lượng cao", "Di động")
            entryValues = arrayOf("cover_url", "cover_mobile_url")
            setDefaultValue("cover_url")

            setOnPreferenceChangeListener { _, newValue ->
                val selected = newValue as String
                val index = findIndexOfValue(selected)
                val entry = entryValues[index] as String

                preferences.edit()
                    .putString("coverQuality", entry)
                    .commit()
            }
        }

        screen.addPreference(coverQualityPref)
    }

    private val SharedPreferences.coverQuality
        get() = getString("coverQuality", "")

    companion object {
        const val PREFIX_ID_SEARCH = "id:"
    }
}
