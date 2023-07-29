package eu.kanade.tachiyomi.extension.en.templescan

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.util.Calendar
import kotlin.math.min

class TempleScan : HttpSource() {

    override val name = "Temple Scan"

    override val lang = "en"

    override val baseUrl = "https://templescan.net"

    override val supportsLatest = false

    override val versionId = 2

    override val client = network.cloudflareClient.newBuilder()
        .rateLimit(1)
        .build()

    private val json: Json by injectLazy()

    private val seriesCache: List<Series> by lazy {
        val response = client.newCall(GET(baseUrl, headers)).execute()

        if (response.isSuccessful.not()) {
            response.close()
            throw Exception("Http Error ${response.code}")
        }

        response.asJsoup()
            .selectFirst("script:containsData(proyectos)")
            ?.data()
            ?.let { proyectosRegex.find(it)?.groupValues?.get(1) }
            ?.let(json::decodeFromString)
            ?: throw Exception(SeriesCacheFailureException)
    }

    private lateinit var filteredSeriesCache: List<Series>

    private fun List<Series>.toMangasPage(page: Int): MangasPage {
        val end = min(page * limit, this.size)
        val entries = this.subList((page - 1) * limit, end)
            .map(Series::toSManga)

        return MangasPage(entries, end < this.size)
    }

    @Serializable
    data class Series(
        @SerialName("nombre") val name: String,
        val slug: String,
        @SerialName("portada") val cover: String,
    ) {
        fun toSManga() = SManga.create().apply {
            url = "/comic/$slug"
            title = name
            thumbnail_url = cover
        }
    }

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        val mangasPage = seriesCache.toMangasPage(page)

        return Observable.just(mangasPage)
    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        if (query.startsWith(SEARCH_PREFIX)) {
            val url = "/comic/${query.substringAfter(SEARCH_PREFIX)}"
            val manga = SManga.create().apply { this.url = url }
            return fetchMangaDetails(manga).map {
                val newManga = it.apply { this.url = url }
                MangasPage(listOf(newManga), false)
            }
        }

        if (page == 1) {
            filteredSeriesCache = seriesCache.filter {
                it.name.contains(query.trim(), true)
            }
        }

        val mangasPage = filteredSeriesCache.toMangasPage(page)

        return Observable.just(mangasPage)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()

        return SManga.create().apply {
            thumbnail_url = document.select(".max-w-80 > img").attr("abs:src")
            description = document.select("section[id=section-sinopsis] p").text()
            title = document.select("h1").text()
            genre = document.select("div.flex div:contains(gen) + div a").joinToString { it.text().trim() }
            author = document.selectFirst("div.flex div:contains(aut) + div")?.text()
        }
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val elements = response.asJsoup()
            .select("div.contenedor-capitulo-miniatura a")

        return elements.map { element ->
            SChapter.create().apply {
                setUrlWithoutDomain(element.attr("href"))
                name = element.select("div[id=name]").text()
                date_upload = element.select("time").text().let {
                    runCatching { it.parseRelativeDate() }.getOrDefault(0L)
                }
            }
        }
    }

    override fun pageListParse(response: Response): List<Page> {
        val elements = response.asJsoup()
            .select("main div img")

        return elements.mapIndexed { index, element ->
            Page(index, "", element.attr("abs:src"))
        }
    }

    private fun String.parseRelativeDate(): Long {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var parsedDate = 0L

        val relativeDate = try {
            this.split(" ")[0].trim().toInt()
        } catch (e: NumberFormatException) {
            return 0L
        }

        when {
            "second" in this -> {
                parsedDate = now.apply { add(Calendar.SECOND, -relativeDate) }.timeInMillis
            }
            "minute" in this -> {
                parsedDate = now.apply { add(Calendar.MINUTE, -relativeDate) }.timeInMillis
            }
            "hour" in this -> {
                parsedDate = now.apply { add(Calendar.HOUR, -relativeDate) }.timeInMillis
            }
            "day" in this -> {
                parsedDate = now.apply { add(Calendar.DAY_OF_YEAR, -relativeDate) }.timeInMillis
            }
            "week" in this -> {
                parsedDate = now.apply { add(Calendar.WEEK_OF_YEAR, -relativeDate) }.timeInMillis
            }
            "month" in this -> {
                parsedDate = now.apply { add(Calendar.MONTH, -relativeDate) }.timeInMillis
            }
            "year" in this -> {
                parsedDate = now.apply { add(Calendar.YEAR, -relativeDate) }.timeInMillis
            }
        }
        return parsedDate
    }

    companion object {
        private val proyectosRegex = Regex("""proyectos\s*=\s*([^\;]+)""")
        private const val SeriesCacheFailureException = "Unable to extract series information"

        private const val limit = 20
        const val SEARCH_PREFIX = "slug:"
    }

    override fun popularMangaRequest(page: Int) = throw UnsupportedOperationException("Not Used")
    override fun popularMangaParse(response: Response) = throw UnsupportedOperationException("Not Used")
    override fun latestUpdatesRequest(page: Int) = throw UnsupportedOperationException("Not Used")
    override fun latestUpdatesParse(response: Response) = throw UnsupportedOperationException("Not Used")
    override fun imageUrlParse(response: Response) = throw UnsupportedOperationException("Not Used")
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) = throw UnsupportedOperationException("Not Used")
    override fun searchMangaParse(response: Response) = throw UnsupportedOperationException("Not Used")
}
