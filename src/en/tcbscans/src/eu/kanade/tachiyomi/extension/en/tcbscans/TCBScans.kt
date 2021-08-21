package eu.kanade.tachiyomi.extension.en.tcbscans

import android.app.Application
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TCBScans : ParsedHttpSource() {

    override val name = "TCB Scans"
    override val baseUrl = "https://onepiecechapters.com"
    override val lang = "en"
    override val supportsLatest = false
    override val client: OkHttpClient = network.cloudflareClient

    // popular
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/projects")
    }

    override fun popularMangaSelector() = "#latestProjects .elementor-widget-image-box:not(.elementor-widget-spacer)"

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.thumbnail_url = element.select(".attachment-thumbnail").attr("src")
        manga.setUrlWithoutDomain(element.select(".elementor-image-box-title a").attr("href"))
        manga.title = element.select(".elementor-image-box-title a").text()
        return manga
    }

    override fun popularMangaNextPageSelector(): String? = null

    // latest
    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()

    override fun latestUpdatesSelector(): String = throw UnsupportedOperationException()

    override fun latestUpdatesFromElement(element: Element): SManga = throw UnsupportedOperationException()

    override fun latestUpdatesNextPageSelector(): String = throw UnsupportedOperationException()

    // search
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        Observable.just(MangasPage(emptyList(), false))

    override fun searchMangaFromElement(element: Element): SManga = throw Exception("Not used")

    override fun searchMangaNextPageSelector(): String = throw Exception("Not used")

    override fun searchMangaSelector(): String = throw Exception("Not used")

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw Exception("Not used")

    // manga details
    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        val descElement = document.select(".elementor-widget-text-editor").parents().first()

        thumbnail_url = descElement.select("img").attr("src")
        title = descElement.select(".elementor-heading-title").text()
        description = descElement.select(".elementor-widget-text-editor div").text()
    }

    // chapters
    override fun chapterListSelector() =
        ".elementor-column-gap-no .elementor-widget-image-box,.elementor-column-gap-default .elementor-widget-image-box"

    private fun chapterWithDate(element: Element, slug: String): SChapter {
        val seriesPrefs = Injekt.get<Application>().getSharedPreferences("source_${id}_updateTime:$slug", 0)
        val seriesPrefsEditor = seriesPrefs.edit()

        val chapter = chapterFromElement(element)

        val currentTimeMillis = System.currentTimeMillis()
        if (!seriesPrefs.contains(chapter.name)) {
            seriesPrefsEditor.putLong(chapter.name, currentTimeMillis)
        }

        chapter.date_upload = seriesPrefs.getLong(chapter.name, currentTimeMillis)

        seriesPrefsEditor.apply()
        return chapter
    }

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select("a").first()
        val chapter = SChapter.create()

        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = element.select(".elementor-image-box-title").text() + ": " + element.select(".elementor-image-box-description").text()

        return chapter
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val slug = response.request.url.pathSegments[0]

        return document.select(chapterListSelector()).map { chapterWithDate(it, slug) }
    }

    // pages
    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()
        var i = 0
        document.select(".container .img_container center img").forEach { element ->
            val url = element.attr("src")
            i++
            if (url.isNotEmpty()) {
                pages.add(Page(i, "", url))
            }
        }
        return pages
    }

    override fun imageUrlParse(document: Document) = ""
}
