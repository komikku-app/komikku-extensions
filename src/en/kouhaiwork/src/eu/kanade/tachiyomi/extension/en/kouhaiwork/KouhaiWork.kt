package eu.kanade.tachiyomi.extension.en.kouhaiwork

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

class KouhaiWork : HttpSource() {
    override val name = "Kouhai Scanlations"

    override val baseUrl = "https://kouhai.work"

    override val lang = "en"

    override val supportsLatest = true

    private val json by injectLazy<Json>()

    override fun latestUpdatesRequest(page: Int) =
        GET("$API_URL/manga/week", headers)

    override fun latestUpdatesParse(response: Response) =
        response.parse()["data"]?.jsonArray?.map {
            val arr = it.jsonArray
            SManga.create().apply {
                url = arr[0].jsonPrimitive.content
                title = arr[1].jsonPrimitive.content
                thumbnail_url = arr.last().jsonPrimitive.content
            }
        }.let { MangasPage(it ?: emptyList(), false) }

    override fun popularMangaRequest(page: Int) =
        GET("$API_URL/manga/all", headers)

    override fun popularMangaParse(response: Response) =
        latestUpdatesParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        FormBody.Builder().add("search", query).add("tags", filters.json())
            .let { POST("$API_URL/manga/search", headers, it.build()) }

    override fun searchMangaParse(response: Response) =
        latestUpdatesParse(response)

    // Request the actual manga URL for the webview
    override fun mangaDetailsRequest(manga: SManga) =
        GET("$baseUrl/series/${manga.url}", headers)

    override fun fetchMangaDetails(manga: SManga) =
        client.newCall(chapterListRequest(manga)).asObservableSuccess().map {
            val series = it.data<KouhaiSeries>()
            manga.description = series.synopsis
            manga.author = series.authors.joinToString()
            manga.artist = series.artists.joinToString()
            manga.genre = series.genres.orEmpty()
                .plus(series.themes.orEmpty())
                .plus(series.demographics.orEmpty())
                .joinToString()
            manga.status = when (series.status) {
                "ongoing" -> SManga.ONGOING
                "finished" -> SManga.COMPLETED
                else -> SManga.UNKNOWN
            }
            manga.initialized = true
            return@map manga
        }!!

    override fun chapterListRequest(manga: SManga) =
        GET("$API_URL/mangas/${manga.url}", headers)

    override fun chapterListParse(response: Response) =
        response.data<KouhaiSeries>().chapters.map {
            SChapter.create().apply {
                url = it.id.toString()
                scanlator = it.group
                chapter_number = it.number
                name = "Chapter ${decimalFormat.format(it.number)}" +
                    if (it.name == null) "" else " - ${it.name}"
                date_upload = dateFormat.parse(it.updated_at)?.time ?: 0L
            }
        }

    override fun pageListRequest(chapter: SChapter) =
        GET("$API_URL/chapters/${chapter.url}", headers)

    override fun pageListParse(response: Response) =
        response.parse()["chapter"]!!.jsonObject["pages"]!!
            .jsonArray.mapIndexed { idx, obj ->
                Page(idx, "", obj.jsonObject["media"]!!.jsonPrimitive.content)
            }

    override fun getFilterList() =
        FilterList(GenresFilter(), ThemesFilter(), DemographicsFilter(), StatusFilter())

    override fun mangaDetailsParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    override fun imageUrlParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    @Suppress("NOTHING_TO_INLINE")
    private inline fun FilterList.json() =
        json.encodeToJsonElement(
            KouhaiTagList(
                find<GenresFilter>()?.state?.filter { it.state }
                    ?.map { KouhaiTag(it.id) } ?: emptyList(),
                find<ThemesFilter>()?.state?.filter { it.state }
                    ?.map { KouhaiTag(it.id) } ?: emptyList(),
                find<DemographicsFilter>()?.state?.takeIf { it != 0 }
                    ?.let { listOf(KouhaiTag(it)) } ?: emptyList(),
                find<StatusFilter>()?.state?.takeIf { it != 0 }
                    ?.let { KouhaiTag(it - 1) }
            )
        ).toString()

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Response.parse() =
        json.parseToJsonElement(body!!.string()).jsonObject

    private inline fun <reified T> Response.data() =
        json.decodeFromJsonElement<T>(parse()["data"]!!)

    private inline fun <reified T> FilterList.find() = find { it is T } as? T

    companion object {
        private const val API_URL = "https://api.kouhai.work/v2"

        private const val ISO_DATE = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"

        private val dateFormat = SimpleDateFormat(ISO_DATE, Locale.ROOT)

        private val decimalFormat = DecimalFormat("#.##")
    }
}
