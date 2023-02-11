package eu.kanade.tachiyomi.extension.all.comickfun

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

const val API_BASE = "https://api.comick.fun"

abstract class ComickFun(override val lang: String, private val comickFunLang: String) : HttpSource() {

    override val name = "Comick"

    override val baseUrl = "https://comick.app"

    override val supportsLatest = true

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = true
    }

    override fun headersBuilder() = Headers.Builder().apply {
        add("Referer", "$baseUrl/")
        add("User-Agent", "Tachiyomi ${System.getProperty("http.agent")}")
    }

    override val client: OkHttpClient = network.client.newBuilder().rateLimit(4, 1).build()

    /** Popular Manga **/
    override fun popularMangaRequest(page: Int): Request {
        return GET(
            API_BASE.toHttpUrl().newBuilder().apply {
                addPathSegment("v1.0")
                addPathSegment("search")
                addQueryParameter("sort", "follow")
                addQueryParameter("page", "$page")
                addQueryParameter("tachiyomi", "true")
            }.toString(),
            headers,
        )
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val result = json.decodeFromString<List<Manga>>(response.body.string())
        return MangasPage(
            result.map { data ->
                SManga.create().apply {
                    url = "/comic/${data.slug}"
                    title = data.title
                    thumbnail_url = data.cover_url
                }
            },
            hasNextPage = true,
        )
    }

    /** Latest Manga **/
    override fun latestUpdatesRequest(page: Int): Request {
        return GET(
            API_BASE.toHttpUrl().newBuilder().apply {
                addPathSegment("v1.0")
                addPathSegment("search")
                if (comickFunLang != "all") addQueryParameter("lang", comickFunLang)
                addQueryParameter("sort", "uploaded")
                addQueryParameter("page", "$page")
                addQueryParameter("tachiyomi", "true")
            }.toString(),
            headers,
        )
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = json.decodeFromString<List<Manga>>(response.body.string())
        return MangasPage(
            result.map { data ->
                SManga.create().apply {
                    url = "/comic/${data.slug}"
                    title = data.title
                    thumbnail_url = data.cover_url
                }
            },
            hasNextPage = true,
        )
    }

    /** Manga Search **/
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url: String = API_BASE.toHttpUrl().newBuilder().apply {
            addPathSegment("search")
            if (query.isEmpty()) {
                filters.forEach { it ->
                    when (it) {
                        is CompletedFilter -> {
                            if (it.state) {
                                addQueryParameter("completed", "true")
                            }
                        }
                        is GenreFilter -> {
                            it.state.filter { (it as TriState).isIncluded() }.forEach {
                                addQueryParameter(
                                    "genres",
                                    (it as TriState).value,
                                )
                            }

                            it.state.filter { (it as TriState).isExcluded() }.forEach {
                                addQueryParameter(
                                    "excludes",
                                    (it as TriState).value,
                                )
                            }
                        }
                        is DemographicFilter -> {
                            it.state.filter { (it as CheckBox).state }.forEach {
                                addQueryParameter(
                                    "demographic",
                                    (it as CheckBox).value,
                                )
                            }
                        }
                        is TypeFilter -> {
                            it.state.filter { (it as CheckBox).state }.forEach {
                                addQueryParameter(
                                    "country",
                                    (it as CheckBox).value,
                                )
                            }
                        }
                        is SortFilter -> {
                            addQueryParameter("sort", it.getValue())
                        }
                        is CreatedAtFilter -> {
                            if (it.state > 0) {
                                addQueryParameter("time", it.getValue())
                            }
                        }
                        is MinimumFilter -> {
                            if (it.state.isNotEmpty()) {
                                addQueryParameter("minimum", it.state)
                            }
                        }
                        is FromYearFilter -> {
                            if (it.state.isNotEmpty()) {
                                addQueryParameter("from", it.state)
                            }
                        }
                        is ToYearFilter -> {
                            if (it.state.isNotEmpty()) {
                                addQueryParameter("to", it.state)
                            }
                        }
                        is TagFilter -> {
                            if (it.state.isNotEmpty()) {
                                it.state.split(",").forEach {
                                    addQueryParameter("tags", it.trim())
                                }
                            }
                        }
                        else -> {}
                    }
                }
            } else {
                addQueryParameter("q", query)
            }
            addQueryParameter("tachiyomi", "true")
            addQueryParameter("page", "$page")
        }.toString()
        return GET(url, headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val result = json.decodeFromString<List<Manga>>(response.body.string())
        return MangasPage(
            result.map { data ->
                SManga.create().apply {
                    url = "/comic/${data.slug}"
                    title = data.title
                    thumbnail_url = data.cover_url
                }
            },
            hasNextPage = result.size >= 30,
        )
    }

    /** Manga Details **/
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(
            GET(
                "$API_BASE${manga.url}".toHttpUrl().newBuilder().apply {
                    addQueryParameter("tachiyomi", "true")
                }.toString(),
                headers,
            ),
        ).asObservableSuccess()
            .map { response -> mangaDetailsParse(response).apply { initialized = true } }
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        return GET("$baseUrl${manga.url}".toHttpUrl().toString())
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val mangaData = json.decodeFromString<MangaDetails>(response.body.string())
        return SManga.create().apply {
            url = "$baseUrl/comic/${mangaData.comic.slug}"
            title = mangaData.comic.title
            artist = mangaData.artists.joinToString { it.name.trim() }
            author = mangaData.authors.joinToString { it.name.trim() }
            description = beautifyDescription(mangaData.comic.desc)
            genre = mangaData.genres.joinToString { it.name.trim() }
            status = parseStatus(mangaData.comic.status)
            thumbnail_url = mangaData.comic.cover_url
        }
    }

    /** Manga Chapter List **/
    override fun chapterListRequest(manga: SManga): Request {
        return GET(
            "$API_BASE${manga.url}".toHttpUrl().newBuilder().apply {
                addQueryParameter("tachiyomi", "true")
            }.toString(),
            headers,
        )
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val mangaData = json.decodeFromString<MangaDetails>(response.body.string())
        val mangaHid = findCurrentSlug(mangaData.comic.slug)
        val chapterData = client.newCall(
            GET(
                API_BASE.toHttpUrl().newBuilder().apply {
                    addPathSegment("comic")
                    addPathSegments(mangaHid)
                    addPathSegments("chapters")
                    if (comickFunLang != "all") addQueryParameter("lang", comickFunLang)
                    addQueryParameter(
                        "limit",
                        mangaData.comic.chapter_count.toString(),
                    )
                }.toString(),
                headers,
            ),
        ).execute()
        val result = json.decodeFromString<ChapterList>(chapterData.body.string())
        return result.chapters.map { chapter ->
            SChapter.create().apply {
                url = "/comic/${mangaData.comic.slug}/${chapter.hid}-chapter-${chapter.chap}-$comickFunLang"
                name = beautifyChapterName(chapter.vol, chapter.chap, chapter.title)
                date_upload = chapter.created_at.let {
                    try {
                        DATE_FORMATTER.parse(it)?.time ?: 0L
                    } catch (e: ParseException) {
                        0L
                    }
                }
                scanlator = chapter.group_name.joinToString().takeUnless { it.isBlank() }
            }
        }
    }

    private val DATE_FORMATTER by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
    }

    /** Chapter Pages **/
    override fun pageListRequest(chapter: SChapter): Request {
        val chapterHid = chapter.url.substringAfterLast("/").substringBefore("-")
        return GET(
            API_BASE.toHttpUrl().newBuilder().apply {
                addPathSegment("chapter")
                addPathSegment(chapterHid)
                addQueryParameter("tachiyomi", "true")
            }.toString(),
            headers,
        )
    }

    override fun pageListParse(response: Response): List<Page> {
        val result = json.decodeFromString<PageList>(response.body.string())
        return result.chapter.images.mapIndexedNotNull { index, data ->
            if (data.url == null) null else Page(index = index, imageUrl = data.url)
        }
    }

    companion object {
        const val SLUG_SEARCH_PREFIX = "id:"
    }

    /** Don't touch this, Tachiyomi forces you to declare the following methods even I you don't use them **/
    override fun imageUrlParse(response: Response): String {
        return ""
    }

    override fun getFilterList() = FilterList(
        getFilters(),
    )

    /** Map the slug to comic ID as slug might be changes by comic ID will not. **/
    // TODO: Cleanup once ext-lib 1.4 is released.
    private fun findCurrentSlug(oldSlug: String): String {
        val response = client.newCall(
            GET(
                API_BASE.toHttpUrl().newBuilder().apply {
                    addPathSegment("tachiyomi")
                    addPathSegment("mapping")
                    addQueryParameter("slugs", oldSlug)
                }.toString(),
                headers,
            ),
        ).execute()

        /** If the API does not contain the ID for the slug, return the slug back **/
        return json.parseToJsonElement(response.body.string()).jsonObject[oldSlug]!!.jsonPrimitive.content
    }
}
