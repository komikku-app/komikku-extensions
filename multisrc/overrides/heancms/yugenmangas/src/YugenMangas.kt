package eu.kanade.tachiyomi.extension.es.yugenmangas

import eu.kanade.tachiyomi.multisrc.heancms.Genre
import eu.kanade.tachiyomi.multisrc.heancms.GenreFilter
import eu.kanade.tachiyomi.multisrc.heancms.HeanCms
import eu.kanade.tachiyomi.multisrc.heancms.HeanCmsSeriesDto
import eu.kanade.tachiyomi.multisrc.heancms.SortByFilter
import eu.kanade.tachiyomi.multisrc.heancms.StatusFilter
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class YugenMangas :
    HeanCms(
        "YugenMangas",
        "https://yugenmangas.net",
        "es",
        "https://api.yugenmangas.net",
    ) {

    // Site changed from Madara to HeanCms.
    override val versionId = 2

    override val client = super.client.newBuilder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .rateLimitHost(apiUrl.toHttpUrl(), 2, 3)
        .build()

    override val coverPath: String = ""

    override val dateFormat: SimpleDateFormat = super.dateFormat.apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun popularMangaRequest(page: Int): Request {
        val url = "$apiUrl/query".toHttpUrl().newBuilder()
            .addQueryParameter("query_string", "")
            .addQueryParameter("series_status", "All")
            .addQueryParameter("order", "desc")
            .addQueryParameter("orderBy", "total_views")
            .addQueryParameter("series_type", "Comic")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("perPage", "12")
            .addQueryParameter("tags_ids", "[]")

        return GET(url.build(), headers)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val url = "$apiUrl/query".toHttpUrl().newBuilder()
            .addQueryParameter("query_string", "")
            .addQueryParameter("series_status", "All")
            .addQueryParameter("order", "desc")
            .addQueryParameter("orderBy", "latest")
            .addQueryParameter("series_type", "Comic")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("perPage", "12")
            .addQueryParameter("tags_ids", "[]")

        return GET(url.build(), headers)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val sortByFilter = filters.firstInstanceOrNull<SortByFilter>()
        val statusFilter = filters.firstInstanceOrNull<StatusFilter>()

        val tagIds = filters.firstInstanceOrNull<GenreFilter>()?.state.orEmpty()
            .filter(Genre::state)
            .map(Genre::id)
            .joinToString(",", prefix = "[", postfix = "]")

        val url = "$apiUrl/query".toHttpUrl().newBuilder()
            .addQueryParameter("query_string", query)
            .addQueryParameter("series_status", statusFilter?.selected?.value ?: "All")
            .addQueryParameter("order", if (sortByFilter?.state?.ascending == true) "asc" else "desc")
            .addQueryParameter("orderBy", sortByFilter?.selected ?: "total_views")
            .addQueryParameter("series_type", "Comic")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("perPage", "12")
            .addQueryParameter("tags_ids", tagIds)

        return GET(url.build(), headers)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = response.parseAs<HeanCmsSeriesDto>()

        val currentTimestamp = System.currentTimeMillis()

        return result.seasons.orEmpty()
            .flatMap { it.chapters.orEmpty() }
            .filterNot { it.price == 1 }
            .map { it.toSChapter(result.slug, dateFormat) }
            .filter { it.date_upload <= currentTimestamp }
    }

    override fun pageListRequest(chapter: SChapter) = GET(baseUrl + chapter.url, headers)

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()

        val images = document.selectFirst("div.min-h-screen > div.container > p.items-center")

        return images?.select("img").orEmpty().mapIndexed { i, img ->
            val imageUrl = if (img.hasClass("lazy")) img.absUrl("data-src") else img.absUrl("src")
            Page(i, "", imageUrl)
        }
    }

    override fun getGenreList(): List<Genre> = listOf(
        Genre("+18", 1),
        Genre("Acción", 36),
        Genre("Adulto", 38),
        Genre("Apocalíptico", 3),
        Genre("Artes marciales (1)", 16),
        Genre("Artes marciales (2)", 37),
        Genre("Aventura", 2),
        Genre("Boys Love", 4),
        Genre("Ciencia ficción", 39),
        Genre("Comedia", 5),
        Genre("Demonios", 6),
        Genre("Deporte", 26),
        Genre("Drama", 7),
        Genre("Ecchi", 8),
        Genre("Familia", 9),
        Genre("Fantasía", 10),
        Genre("Girls Love", 11),
        Genre("Gore", 12),
        Genre("Harem", 13),
        Genre("Harem inverso", 14),
        Genre("Histórico", 48),
        Genre("Horror", 41),
        Genre("Isekai", 40),
        Genre("Josei", 15),
        Genre("Maduro", 42),
        Genre("Magia", 17),
        Genre("MangoScan", 35),
        Genre("Mecha", 18),
        Genre("Militar", 19),
        Genre("Misterio", 20),
        Genre("Psicológico", 21),
        Genre("Realidad virtual", 46),
        Genre("Recuentos de la vida", 25),
        Genre("Reencarnación", 22),
        Genre("Regresion", 23),
        Genre("Romance", 24),
        Genre("Seinen", 27),
        Genre("Shonen", 28),
        Genre("Shoujo", 29),
        Genre("Sistema", 45),
        Genre("Smut", 30),
        Genre("Supernatural", 31),
        Genre("Supervivencia", 32),
        Genre("Tragedia", 33),
        Genre("Transmigración", 34),
        Genre("Vida Escolar", 47),
        Genre("Yaoi", 43),
        Genre("Yuri", 44),
    )
}
