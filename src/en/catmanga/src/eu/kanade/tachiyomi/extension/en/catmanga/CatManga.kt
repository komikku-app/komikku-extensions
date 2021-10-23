package eu.kanade.tachiyomi.extension.en.catmanga

import android.app.Application
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import rx.Observable
import uy.kohesive.injekt.injectLazy

@ExperimentalSerializationApi
class CatManga : HttpSource() {

    private val application: Application by injectLazy()

    override val name = "CatManga"
    override val baseUrl = "https://catmanga.org"
    override val supportsLatest = false
    override val lang = "en"
    private val json: Json by injectLazy()

    private val allSeriesRequest = GET("$baseUrl/api/series/allSeries")

    override fun popularMangaRequest(page: Int) = allSeriesRequest

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) = allSeriesRequest

    override fun chapterListRequest(manga: SManga): Request {
        val seriesId = manga.url.substringAfter("/series/")
        return GET("$baseUrl/api/series/$seriesId")
    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return client.newCall(searchMangaRequest(page, query, filters))
            .asObservableSuccess()
            .map { response ->
                val manga = json.decodeFromString<List<CatSeries>>(response.body!!.string())
                    .filter {
                        if (query.startsWith(SERIES_ID_SEARCH_PREFIX)) {
                            return@filter it.series_id.contains(query.removePrefix(SERIES_ID_SEARCH_PREFIX), true)
                        }
                        sequence { yieldAll(it.alt_titles); yield(it.title) }
                            .any { title -> title.contains(query, true) }
                    }
                    .map { it.toSManga() }
                    .toList()
                MangasPage(manga, false)
            }
    }

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        val seriesId = manga.url.substringAfter("/series/")
        return client.newCall(allSeriesRequest)
            .asObservableSuccess()
            .map { response ->
                json.decodeFromString<List<CatSeries>>(response.body!!.string())
                    .find { it.series_id == seriesId }
                    ?.toSManga() ?: manga
            }
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val series = json.decodeFromString<CatSeries>(response.body!!.string())
        val seriesPrefs = application.getSharedPreferences("source_${id}_time_found:${series.series_id}", 0)
        val seriesPrefsEditor = seriesPrefs.edit()
        val chapters = series.chapters!!
            .asReversed()
            .map { chapter ->
                val title = chapter.title ?: ""
                val groups = chapter.groups.joinToString(", ")
                val numberUrl = chapter.number.chapterNumberToUrlPath()
                val displayNumber = chapter.display_number ?: numberUrl
                SChapter.create().apply {
                    url = "/series/${series.series_id}/$numberUrl"
                    chapter_number = chapter.number
                    scanlator = groups

                    name = if (chapter.volume != null) {
                        "Vol.${chapter.volume} "
                    } else {
                        ""
                    }
                    name += "Ch.$displayNumber"
                    if (title.isNotBlank()) {
                        name += " - $title"
                    }

                    // Save current time when a chapter is found for the first time, and reuse it on future
                    // checks to prevent manga entry without any new chapter bumped to the top of
                    // "Latest chapter" list when the library is updated.
                    val currentTimeMillis = System.currentTimeMillis()
                    if (!seriesPrefs.contains(numberUrl)) {
                        seriesPrefsEditor.putLong(numberUrl, currentTimeMillis)
                    }
                    date_upload = seriesPrefs.getLong(numberUrl, currentTimeMillis)
                }
            }
        seriesPrefsEditor.apply()
        return chapters
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val mangas = json.decodeFromString<List<CatSeries>>(response.body!!.string()).map { it.toSManga() }
        return MangasPage(mangas, false)
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return client.newCall(pageListRequest(chapter))
            .asObservableSuccess()
            .map {
                val doc = it.asJsoup().getDataJsonObject()
                val pages = if (doc["isFallback"]!!.jsonPrimitive.boolean) {
                    val buildId = doc["buildId"]!!.jsonPrimitive.content
                    val directRequest = GET("$baseUrl/_next/data/$buildId/${chapter.url}.json")
                    val directResponse = client.newCall(directRequest).execute()
                    json.parseToJsonElement(directResponse.body!!.string())
                } else {
                    doc["props"]!!
                }.jsonObject["pageProps"]!!.jsonObject["pages"]!!
                json.decodeFromJsonElement<List<String>>(pages)
                    .mapIndexed { index, s -> Page(index, "", s) }
            }
    }

    /**
     * Returns json object of site data
     */
    private fun Document.getDataJsonObject() = json.parseToJsonElement(getElementById("__NEXT_DATA__").html()).jsonObject

    /**
     * Returns string without decimal when it is not relevant
     */
    private fun Float.chapterNumberToUrlPath(): String {
        return if (toInt().toFloat() == this) toInt().toString() else toString()
    }

    override fun pageListParse(response: Response): List<Page> {
        throw UnsupportedOperationException("Not used.")
    }

    override fun latestUpdatesRequest(page: Int): Request {
        throw UnsupportedOperationException("Not used.")
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        throw UnsupportedOperationException("Not used.")
    }

    override fun mangaDetailsParse(response: Response): SManga {
        throw UnsupportedOperationException("Not used.")
    }

    override fun searchMangaParse(response: Response): MangasPage {
        throw UnsupportedOperationException("Not used.")
    }

    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException("Not used.")
    }

    companion object {
        const val SERIES_ID_SEARCH_PREFIX = "series_id:"
    }
}
