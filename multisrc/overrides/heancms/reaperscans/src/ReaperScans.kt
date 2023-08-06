package eu.kanade.tachiyomi.extension.pt.reaperscans

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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.TimeZone

class ReaperScans : HeanCms(
    "Reaper Scans",
    "https://reaperscans.net",
    "pt-BR",
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimitHost(apiUrl.toHttpUrl(), 1, 2)
        .build()

    // Site changed from Madara to HeanCms.
    override val versionId = 2

    override val coverPath: String = ""

    override val dateFormat: SimpleDateFormat = super.dateFormat.apply {
        timeZone = TimeZone.getTimeZone("GMT+01:00")
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
        Genre("Artes Marciais", 2),
        Genre("Aventura", 10),
        Genre("Ação", 9),
        Genre("Comédia", 14),
        Genre("Drama", 15),
        Genre("Escolar", 7),
        Genre("Fantasia", 11),
        Genre("Ficção científica", 16),
        Genre("Guerra", 17),
        Genre("Isekai", 18),
        Genre("Jogo", 12),
        Genre("Mangá", 24),
        Genre("Manhua", 23),
        Genre("Manhwa", 22),
        Genre("Mecha", 19),
        Genre("Mistério", 20),
        Genre("Nacional", 8),
        Genre("Realidade Virtual", 21),
        Genre("Retorno", 3),
        Genre("Romance", 5),
        Genre("Segunda vida", 4),
        Genre("Seinen", 1),
        Genre("Shounen", 13),
        Genre("Terror", 6),
    )
}
