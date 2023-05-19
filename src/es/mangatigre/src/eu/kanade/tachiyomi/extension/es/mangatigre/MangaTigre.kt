package eu.kanade.tachiyomi.extension.es.mangatigre

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import eu.kanade.tachiyomi.source.model.Filter
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import org.jsoup.nodes.Document
import uy.kohesive.injekt.injectLazy
import java.util.Calendar

class MangaTigre : HttpSource() {

    override val name = "MangaTigre"

    override val baseUrl = "https://www.mangatigre.net"

    override val lang = "es"

    override val supportsLatest = true

    private val json: Json by injectLazy()

    private val imgCDNUrl = "https://i2.mtcdn.xyz"

    private var mtToken = ""

    override val client: OkHttpClient = network.client.newBuilder()
        .addInterceptor { chain ->
            val request = chain.request()

            if (request.method == "POST") {
                val response = chain.proceed(request)

                if (response.code == 419) {
                    response.close()
                    getToken()

                    val newBody = json.parseToJsonElement(request.bodyString).jsonObject.toMutableMap().apply {
                        this["_token"] = JsonPrimitive(mtToken)
                    }

                    val payload = Json.encodeToString(JsonObject(newBody)).toRequestBody(JSON_MEDIA_TYPE)

                    val apiHeaders = headersBuilder()
                        .add("Accept", ACCEPT_JSON)
                        .add("Content-Type", payload.contentType().toString())
                        .build()

                    val newRequest = request.newBuilder()
                        .headers(apiHeaders)
                        .method(request.method, payload)
                        .build()

                    return@addInterceptor chain.proceed(newRequest)
                }
                return@addInterceptor response
            }
            chain.proceed(request)
        }
        .rateLimitHost(baseUrl.toHttpUrl(), 1, 2)
        .build()

    private fun getToken() {
        val document = client.newCall(GET(baseUrl, headers)).execute().asJsoup()
        mtToken = document.selectFirst("input.input-search[data-csrf]")!!.attr("data-csrf")
    }

    override fun popularMangaRequest(page: Int): Request {
        val payloadObj = PayloadManga(
            page = page,
            token = mtToken,
        )

        val payload = json.encodeToString(payloadObj).toRequestBody(JSON_MEDIA_TYPE)

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Content-Type", payload.contentType().toString())
            .build()

        return POST("$baseUrl/mangas?sort=views", apiHeaders, payload)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val jsonString = response.body.string()

        val result = json.decodeFromString<MangasDto>(jsonString)

        val mangas = result.mangas.map {
            SManga.create().apply {
                setUrlWithoutDomain("$baseUrl/manga/${it.slug}")
                title = it.title
                thumbnail_url = "$imgCDNUrl/mangas/${it.thumbnailFileName}"
            }
        }
        val hasNextPage = result.totalPages > result.page

        return MangasPage(mangas, hasNextPage)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val payloadObj = PayloadManga(
            page = page,
            token = mtToken,
        )

        val payload = json.encodeToString(payloadObj).toRequestBody(JSON_MEDIA_TYPE)

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Content-Type", payload.contentType().toString())
            .build()

        return POST("$baseUrl/mangas?sort=date", apiHeaders, payload)
    }

    override fun latestUpdatesParse(response: Response) = popularMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isNotEmpty()) {
            if (query.length < 2) throw Exception("La cadena de búsqueda debe tener por lo menos 2 caracteres")

            val payloadObj = PayloadSearch(
                query = query,
                token = mtToken,
            )
            val payload = json.encodeToString(payloadObj).toRequestBody(JSON_MEDIA_TYPE)

            val apiHeaders = headersBuilder()
                .add("Accept", ACCEPT_JSON)
                .add("Content-Type", payload.contentType().toString())
                .build()

            return POST("$baseUrl/mangas/search#$query", apiHeaders, payload)
        }

        val url = "$baseUrl/mangas".toHttpUrlOrNull()!!.newBuilder()

        filters.forEach { filter ->
            when (filter) {
                is OrderFilter -> {
                    url.addQueryParameter("sort", filter.toUriPart())
                }
                is TypeFilter -> {
                    filter.state.forEach { content ->
                        if (content.state) url.addQueryParameter("type[]", content.id)
                    }
                }
                is StatusFilter -> {
                    filter.state.forEach { content ->
                        if (content.state) url.addQueryParameter("status[]", content.id)
                    }
                }
                is DemographicFilter -> {
                    filter.state.forEach { content ->
                        if (content.state) url.addQueryParameter("demographic[]", content.id)
                    }
                }
                is ContentFilter -> {
                    filter.state.forEach { content ->
                        if (content.state) url.addQueryParameter("content[]", content.id)
                    }
                }
                is FormatFilter -> {
                    filter.state.forEach { content ->
                        if (content.state) url.addQueryParameter("format[]", content.id)
                    }
                }
                is GenreFilter -> {
                    filter.state.forEach { content ->
                        if (content.state) url.addQueryParameter("genre[]", content.id)
                    }
                }
                is ThemeFilter -> {
                    filter.state.forEach { content ->
                        if (content.state) url.addQueryParameter("theme[]", content.id)
                    }
                }
                else -> {}
            }
        }

        val payloadObj = PayloadManga(
            page = page,
            token = mtToken,
        )
        val payload = json.encodeToString(payloadObj).toRequestBody(JSON_MEDIA_TYPE)

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Content-Type", payload.contentType().toString())
            .build()

        return POST(url.build().toString(), apiHeaders, payload)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val query = response.request.url.fragment
        val jsonString = response.body.string()

        if (!query.isNullOrEmpty()) {
            val result = json.decodeFromString<SearchDto>(jsonString)

            val mangas = result.result.map {
                SManga.create().apply {
                    setUrlWithoutDomain("$baseUrl/manga/${it.slug}")
                    title = it.title
                    thumbnail_url = "$imgCDNUrl/mangas/${it.thumbnailFileName}"
                }
            }

            return MangasPage(mangas, false)
        }

        val result = json.decodeFromString<MangasDto>(jsonString)

        val mangas = result.mangas.map {
            SManga.create().apply {
                setUrlWithoutDomain("$baseUrl/manga/${it.slug}")
                title = it.title
                thumbnail_url = "$imgCDNUrl/mangas/${it.thumbnailFileName}"
            }
        }
        val hasNextPage = result.totalPages > result.page

        return MangasPage(mangas, hasNextPage)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        return SManga.create().apply {
            description = createDescription(document)
            genre = createGenres(document)
            thumbnail_url = document.selectFirst("div.manga-image > img")!!.attr("abs:data-src")
            author = document.selectFirst("li.list-group-item:has(strong:contains(Autor)) > a")?.ownText()?.trim()
            artist = document.selectFirst("li.list-group-item:has(strong:contains(Artista)) > a")?.ownText()?.trim()
            status = document.selectFirst("li.list-group-item:has(strong:contains(Estado))")?.ownText()?.trim()!!.toStatus()
        }
    }

    private fun createGenres(document: Document): String {
        val demographic = document.select("li.list-group-item:has(strong:contains(Demografía)) a").joinToString { it.text() }
        val genres = document.select("li.list-group-item:has(strong:contains(Géneros)) a").joinToString { it.text() }
        val themes = document.select("li.list-group-item:has(strong:contains(Temas)) a").joinToString { it.text() }
        val content = document.select("li.list-group-item:has(strong:contains(Contenido)) a").joinToString { it.text() }
        return listOf(demographic, genres, themes, content).joinToString(", ")
    }

    private fun createDescription(document: Document): String {
        val originalName = document.selectFirst("li.list-group-item:has(strong:contains(Original))")?.ownText()?.trim() ?: ""
        val alternativeName = document.select("li.list-group-item:has(strong:contains(Alternativo)) span.alter-name").text()
        val year = document.selectFirst("li.list-group-item:has(strong:contains(Año))")?.ownText()?.trim() ?: ""
        val animeAdaptation = document.selectFirst("li.list-group-item:has(strong:contains(Anime))")?.ownText()?.trim() ?: ""
        val country = document.selectFirst("li.list-group-item:has(strong:contains(País))")?.ownText()?.trim() ?: ""
        val summary = document.selectFirst("div.synopsis > p")?.ownText()?.trim() ?: ""
        return StringBuilder()
            .appendLine("Nombre Original: $originalName")
            .appendLine("Títulos Alternativos: $alternativeName")
            .appendLine("Año: $year")
            .appendLine("Adaptación al Anime: $animeAdaptation")
            .appendLine("País: $country")
            .appendLine()
            .appendLine("Sinopsis: $summary")
            .toString()
    }

    override fun chapterListRequest(manga: SManga): Request {
        val payloadObj = PayloadChapter(
            token = mtToken,
        )

        val payload = json.encodeToString(payloadObj).toRequestBody(JSON_MEDIA_TYPE)

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Content-Type", payload.contentType().toString())
            .build()

        return POST("$baseUrl${manga.url}", apiHeaders, payload)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        return response.asJsoup().select("li").map {
            SChapter.create().apply {
                setUrlWithoutDomain(it.select("a").attr("href"))
                name = it.selectFirst("a")!!.ownText().trim()
                date_upload = parseRelativeDate(it.selectFirst("span")!!.ownText().trim())
            }
        }
    }

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val script = document.selectFirst("script:containsData(window.chapter)")!!.data()
        val jsonString = CHAPTERS_REGEX.find(script)!!.groupValues[1]

        val result = json.decodeFromString<ChapterDto>(jsonString)
        val slug = result.manga.slug
        val number = result.number

        return result.images.map {
            val imageUrl = "$imgCDNUrl/chapters/$slug/$number/${it.value.name}.${it.value.format}"
            Page(it.key.toInt(), "", imageUrl)
        }.sortedBy { it.index }
    }

    override fun getFilterList() = FilterList(
        Filter.Header("Los filtros serán ignorados si se realiza una búsqueda textual"),
        Filter.Separator(),
        OrderFilter(),
        Filter.Separator(),
        TypeFilter(getFilterTypeList()),
        Filter.Separator(),
        StatusFilter(getFilterStatusList()),
        Filter.Separator(),
        DemographicFilter(getFilterDemographicList()),
        Filter.Separator(),
        ContentFilter(getFilterContentList()),
        Filter.Separator(),
        FormatFilter(getFilterFormatList()),
        Filter.Separator(),
        GenreFilter(getFilterGenreList()),
        Filter.Separator(),
        ThemeFilter(getFilterThemeList()),
    )

    override fun imageUrlParse(response: Response) = throw UnsupportedOperationException("Not used.")

    private fun parseRelativeDate(date: String): Long {
        val number = Regex("""(\d+)""").find(date)?.value?.toIntOrNull() ?: return 0
        val cal = Calendar.getInstance()

        return when {
            WordSet("segundo").anyWordIn(date) -> cal.apply { add(Calendar.SECOND, -number) }.timeInMillis
            WordSet("minuto").anyWordIn(date) -> cal.apply { add(Calendar.MINUTE, -number) }.timeInMillis
            WordSet("hora").anyWordIn(date) -> cal.apply { add(Calendar.HOUR, -number) }.timeInMillis
            WordSet("día").anyWordIn(date) -> cal.apply { add(Calendar.DAY_OF_MONTH, -number) }.timeInMillis
            WordSet("semana").anyWordIn(date) -> cal.apply { add(Calendar.DAY_OF_MONTH, -number * 7) }.timeInMillis
            WordSet("mes").anyWordIn(date) -> cal.apply { add(Calendar.MONTH, -number) }.timeInMillis
            WordSet("año").anyWordIn(date) -> cal.apply { add(Calendar.YEAR, -number) }.timeInMillis
            else -> 0
        }
    }
    class WordSet(private vararg val words: String) {
        fun anyWordIn(dateString: String): Boolean = words.any { dateString.contains(it, ignoreCase = true) }
    }

    private val Request.bodyString: String
        get() {
            val requestCopy = newBuilder().build()
            val buffer = Buffer()

            return runCatching { buffer.apply { requestCopy.body!!.writeTo(this) }.readUtf8() }
                .getOrNull() ?: ""
        }

    companion object {
        private const val ACCEPT_JSON = "application/json, text/plain, */*"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        private val CHAPTERS_REGEX = """window\.chapter\s*=\s*'(.+?)';""".toRegex()
    }
}
