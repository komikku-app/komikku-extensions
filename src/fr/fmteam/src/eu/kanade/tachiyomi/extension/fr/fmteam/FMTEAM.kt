package eu.kanade.tachiyomi.extension.fr.fmteam

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FMTEAM : ParsedHttpSource() {
    override val name = "FMTEAM"
    override val baseUrl = "https://fmteam.fr"
    override val lang = "fr"
    override val supportsLatest = true



    companion object {
        private val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.FRENCH)
        private val allPagesRegex = "\"url\":\"(.*?)\"".toRegex()
        private val authorRegex = "Author: *(.*) Synopsis".toRegex()
        private val descriptionRegex = "Synopsis: *(.*)".toRegex()
    }

    // All manga
    override fun popularMangaRequest(page: Int) = GET("$baseUrl/directory/")
    override fun popularMangaSelector() = "#content .panel .list.series .group"
    override fun popularMangaNextPageSelector(): String? = null
    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst(".title a")!!.text().trim()
        setUrlWithoutDomain(element.selectFirst("a")!!.attr("href"))
        thumbnail_url = element.select(".preview").attr("src")
    }

    // Latest
    override fun latestUpdatesRequest(page: Int) = GET(baseUrl)
    override fun latestUpdatesSelector() = ".panel .list .group"
    override fun latestUpdatesNextPageSelector() = ".prevnext .next .gbutton.fright:last-child a"
    override fun latestUpdatesFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst(".title a")!!.text().trim()
        setUrlWithoutDomain(element.select(".title a").attr("href"))
    }
    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(latestUpdatesSelector()).map { element ->
            latestUpdatesFromElement(element)
        }.distinctBy { it.title }
        return MangasPage(mangas, false)
    }

    // Search
    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        var mangas = document.select(popularMangaSelector()).map { popularMangaFromElement(it) }
        val query = response.request.headers["query"]
        if (query != null) {
            mangas = mangas.filter { it.title.contains(query, true) }
        }
        return MangasPage(mangas, false)
    }
    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)
    override fun searchMangaNextPageSelector(): String? = null
    override fun searchMangaSelector(): String = popularMangaSelector()
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val headers = headersBuilder()
            .add("query", query)
            .build()
        return GET("$baseUrl/directory/", headers)
    }

    // Manga details
    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        thumbnail_url = document.select(".comic.info .thumbnail img").attr("src")
        val authorSynopsis = document.select(".large.comic .info").text().trim()
        if (authorSynopsis.contains("Author")) {
            author = authorRegex.find(authorSynopsis)!!.groups[1]!!.value.trim()
        }
        description = descriptionRegex.find(authorSynopsis)!!.groups[1]!!.value.trim()
    }

    // Chapter list
    override fun chapterListSelector() = ".list .group .element"
    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        setUrlWithoutDomain(element.select(".title a").attr("href"))
        name = element.select(".title a").attr("title").trim()
        val date = element.select(".meta_r").text().trim().split(", ")[1]
        date_upload = if (date === "Aujourd'hui") {
            dateFormat.format(Date()).toLong()
        } else {
            try {
                dateFormat.parse(date)!!.time
            } catch (e: ParseException) {
                0L
            }
        }
    }

    // Pages
    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()
        val script = document.selectFirst("script:containsData(pages =)")!!.data()
        val allPages = allPagesRegex.findAll(script)
        allPages.asIterable().mapIndexed { i, it ->
            pages.add(Page(i, "", it.groupValues[1].replace("\\/", "/")))
        }
        return pages
    }
    override fun imageUrlParse(document: Document): String = throw Exception("Not used")
}
