package eu.kanade.tachiyomi.extension.en.solarandsundry

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Locale

private const val ACCEPT_IMAGE = "image/avif,image/webp,image/*,*/*"

private const val ARCHIVE_URL = "https://sas.ewanb.me"

class SolarAndSundry : HttpSource() {

    override val name = "Solar and Sundry"

    override val baseUrl = "https://solar-and-sundry-worker.giraugh.workers.dev"

    override val lang = "en"

    override val supportsLatest = false

    override val client: OkHttpClient = network.cloudflareClient

    private fun createManga(): SManga {
        return SManga.create().apply {
            title = "Solar and Sundry"
            url = "/chapter"
            author = "Ewan Breakey"
            artist = author
            status = SManga.ONGOING
            description = "a sci-fi webcomic about creating an ecosystem where there shouldn't be one"
            thumbnail_url = "https://imagedelivery.net/zthi1l8fKrUGB5ig08mq-Q/4b15df30-85c7-429e-d062-bf1d19f0fd00/public"
        }
    }

    private val imgHeaders by lazy {
        headersBuilder().set("Accept", ACCEPT_IMAGE).build()
    }

    private fun parseDate(dateStr: String): Long {
        return runCatching { DATE_FORMATTER.parse(dateStr)?.time }
            .getOrNull() ?: 0L
    }

    companion object {
        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        }
    }

    // Popular

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        return Observable.just(MangasPage(listOf(createManga()), false))
    }

    override fun popularMangaRequest(page: Int): Request = throw UnsupportedOperationException("Not used")

    override fun popularMangaParse(response: Response): MangasPage = throw UnsupportedOperationException("Not used")

    // Latest

    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException("Not used")

    override fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException("Not used")

    // Search

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> = Observable.just(MangasPage(emptyList(), false))

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw UnsupportedOperationException("Not used")

    override fun searchMangaParse(response: Response): MangasPage = throw UnsupportedOperationException("Not used")

    // Details

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return Observable.just(createManga().apply { initialized = true })
    }

    override fun mangaDetailsParse(response: Response): SManga = throw UnsupportedOperationException("Not used")

    override fun getMangaUrl(manga: SManga): String {
        return ARCHIVE_URL
    }

    // Chapters

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapters = JSONArray(response.body.string())
        val pages = ArrayList<JSONObject>()
        for (i in 0 until chapters.length()) {
            val chapterPages = chapters.getJSONObject(i).getJSONArray("pages")
            for (j in 0 until chapterPages.length()) {
                pages.add(chapterPages.getJSONObject(j))
            }
        }
        return pages.map { page ->
            SChapter.create().apply {
                name = page.getString("name")
                setUrlWithoutDomain(baseUrl + "/page/" + page.getInt("page_number"))
                chapter_number = page.getInt("page_number").toFloat()
                date_upload = parseDate(page.getString("published_at"))
            }
        }.reversed()
    }

    override fun getChapterUrl(chapter: SChapter): String {
        return ARCHIVE_URL + "/comic/" + chapter.chapter_number
    }

    // Pages

    override fun pageListParse(response: Response): List<Page> {
        val page = JSONObject(response.body.string())

        return listOf(Page(0, "", page.getString("image_url")))
    }

    override fun imageRequest(page: Page) = GET(page.imageUrl!!, imgHeaders)

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException("Not used")
}
