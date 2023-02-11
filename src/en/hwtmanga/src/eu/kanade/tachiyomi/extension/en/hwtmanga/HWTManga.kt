package eu.kanade.tachiyomi.extension.en.hwtmanga

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
import kotlinx.serialization.json.jsonObject
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

class HWTManga : HttpSource() {
    override val name = "Hardworking Translations"

    override val baseUrl = "https://www.hwtmanga.com/hwt/"

    override val lang = "en"

    override val supportsLatest = true

    override val client = network.client.newBuilder().cookieJar(
        object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {}

            override fun loadForRequest(url: HttpUrl) =
                listOf(
                    Cookie.Builder()
                        .domain("www.hwtmanga.com")
                        .path("/hwt")
                        .name("PHPSESSID")
                        .value(sessionID)
                        .build(),
                    Cookie.Builder()
                        .domain("www.hwtmanga.com")
                        .path("/")
                        .name("manga_security_id")
                        .value(postID)
                        .build(),
                )
        },
    ).build()

    private var postID = ""

    private var sessionID = ""

    private val json by injectLazy<Json>()

    override fun latestUpdatesRequest(page: Int) =
        FormBody.Builder().search(order = "newest", pid = page)

    override fun latestUpdatesParse(response: Response) =
        searchMangaParse(response)

    override fun popularMangaRequest(page: Int) =
        FormBody.Builder().search(order = "viewed", pid = page)

    override fun popularMangaParse(response: Response) =
        searchMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        FormBody.Builder().search(
            query = query,
            pid = page,
            tags = filters.get<TagFilter>("all;"),
            state = filters.get<StateFilter>("all"),
            order = filters.get<OrderFilter>("az"),
        )

    override fun searchMangaParse(response: Response) =
        response.parse<List<HWTQuery>>("query").map {
            SManga.create().apply {
                title = it.title
                thumbnail_url = it.cimage
                url = "?page=manga&vid=${it.postID}"
            }
        }.let { MangasPage(it, false) }

    override fun fetchMangaDetails(manga: SManga) =
        FormBody.Builder().post("GET_MANGA_INFO") {
            add("scom", "0")
            add("pageid", "1")
            add("pid", manga.id)
        }.let(client::newCall).asObservableSuccess().map { res ->
            // Session cookie is required to view pages
            if (sessionID == "") {
                val request = Request.Builder()
                    .url(baseUrl)
                    .headers(headers)
                    .head().build()
                client.newCall(request).execute().header("Set-Cookie")?.let {
                    sessionID = Cookie.parse(request.url, it)?.value ?: ""
                }
            }

            val info = res.parse<HWTMangaInfo>("mangaInfo")
            info.tags[0].value = info.mtag.value
            manga.title = info.title
            manga.thumbnail_url = info.cover
            manga.description = info.desc + "\n\n\n" +
                info.onames.replace(",", " | ")
            manga.genre = info.tags.joinToString { it.value!! }
            manga.status = when (info.statue) {
                1 -> SManga.ONGOING
                else -> SManga.UNKNOWN
            }
            manga.initialized = true
            return@map manga
        }!!

    override fun chapterListRequest(manga: SManga) =
        FormBody.Builder().post("GET_CHAPTER_LIST") {
            add("pageid", "1")
            add("pid", manga.id)
        }

    override fun chapterListParse(response: Response) =
        response.parse<HWTChapterList>("all_data").mapIndexed { idx, ch ->
            SChapter.create().apply {
                chapter_number = idx + 1f
                url = "?page=watch_manga&cid=${ch.fid}&pid=${ch.pid}"
                date_upload = dateFormat.parse(ch.cdate)?.time ?: 0L
                name = buildString {
                    append("Chapter %.0f".format(chapter_number))
                    if (ch.name != "-") append(" | ${ch.name}")
                    if (ch.is_locked != "false") append(LOCK)
                }
            }
        }

    override fun pageListRequest(chapter: SChapter) =
        FormBody.Builder().post("GET_CHA_DATA", "manga_viewer") {
            val tokens = chapter.tokens
            postID = tokens[5]
            add("pageid", "1")
            add("cid", tokens[3])
            add("pid", postID)
        }

    override fun pageListParse(response: Response) =
        response.parse<List<HWTPage>>("clist")
            .mapIndexed { idx, page -> Page(idx, "", page.image) }

    override fun getFilterList() =
        FilterList(TagFilter(), StateFilter(), OrderFilter())

    override fun mangaDetailsParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    override fun imageUrlParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    private inline val SManga.id: String
        get() = url.substringAfterLast('=')

    private inline val SChapter.tokens: List<String>
        get() = url.split('&', '=')

    private inline val HWTPage.image: String
        get() = if (base.startsWith("http")) base else baseUrl + base

    private fun FormBody.Builder.post(
        subpage: String,
        page: String = "mangaData",
        block: FormBody.Builder.() -> FormBody.Builder,
    ) = add("page", page).add("subpage", subpage).run {
        POST(baseUrl + "callback.php", headers, block().build())
    }

    private fun FormBody.Builder.search(
        query: String = "",
        tags: String = "all;",
        state: String = "all",
        order: String = "az",
        pid: Int = 1,
    ) = post("MANGASEARCH") {
        add("searchbox", query)
        add("byg", tags)
        add("bys", state)
        add("byo", order)
        add("pid", pid.toString())
    }

    private inline fun <reified T> Response.parse(key: String) =
        body.string().let { body ->
            if ("success" !in body) error(body)
            json.decodeFromJsonElement<T>(
                json.parseToJsonElement(body).jsonObject[key]!!,
            )
        }

    private inline fun <reified T> FilterList.get(default: String) =
        find { it is T }?.toString() ?: default

    companion object {
        private const val LOCK = " \uD83D\uDD12"

        private val dateFormat by lazy {
            SimpleDateFormat("MMM dd, yyyy", Locale.ROOT)
        }
    }
}
