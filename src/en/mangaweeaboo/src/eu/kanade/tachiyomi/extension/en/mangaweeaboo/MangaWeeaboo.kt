package eu.kanade.tachiyomi.extension.en.mangaweeaboo

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class MangaWeeaboo : ParsedHttpSource() {
    override val name = "Manga Weeaboo"

    override val baseUrl = "https://mangaweeaboo.com"

    override val lang = "en"

    override val supportsLatest = false

    override fun popularMangaRequest(page: Int) =
        GET("$baseUrl/all/", headers)

    override fun popularMangaSelector() = ".pt-cv-ifield"

    override fun popularMangaNextPageSelector(): String? = null

    override fun popularMangaFromElement(element: Element) =
        SManga.create().apply {
            element.selectFirst(".pt-cv-title > a").let {
                title = it.text()
                setUrlWithoutDomain(it.attr("href"))
            }
            thumbnail_url = element.selectFirst(".lazyload").attr("data-src")
        }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        GET("$baseUrl/page/$page/?s=$query", headers)

    override fun searchMangaSelector() = ".post-content:not(:contains(Updates))"

    override fun searchMangaNextPageSelector() = ".pagination .next"

    override fun searchMangaFromElement(element: Element) =
        SManga.create().apply {
            element.selectFirst(".entry-title > a").let {
                title = it.text()
                setUrlWithoutDomain(it.attr("href"))
            }
            thumbnail_url = element.selectFirst(".lazyload").attr("data-src")
        }

    override fun mangaDetailsParse(document: Document) =
        SManga.create().apply {
            title = document.selectFirst(".elementor-heading-title").text()
            description = document.selectFirst(".elementor-tab-content > p").text()
            document.selectFirst(".elementor-icon-list-item:nth-child(1)").text()
                .let { if (it != "Alternate: Updating") description += "\n\n$it" }
            thumbnail_url = document.selectFirst(".size-full.lazyload").attr("data-src")
            author = document.selectFirst(".elementor-icon-list-item:nth-child(2)")
                .text().substringAfter("Author: ").takeIf { it != "Updating" }
            artist = author
            status = when (document.selectFirst(".elementor-icon-list-item:nth-child(3)").text()) {
                "Status: Ongoing" -> SManga.ONGOING
                else -> SManga.UNKNOWN
            }
            genre = document.selectFirst(".elementor-icon-list-item:nth-child(4)")
                .text().substringAfter("Genres: ").takeIf { it != "Updating" }
        }

    override fun chapterListSelector() = ".insideframe li:contains(Chapter)"

    override fun chapterFromElement(element: Element) =
        SChapter.create().apply {
            element.selectFirst("a").let {
                setUrlWithoutDomain(it.attr("href"))
                chapter_number = it.text().substringAfterLast(' ').toFloat()
                name = "Chapter %.0f".format(chapter_number)
            }
            date_upload = dateFormat.parse(element.selectFirst(".date").text())?.time ?: 0L
        }

    override fun pageListParse(document: Document) =
        document.select(".elementor-widget-container > .attachment-full")
            .mapIndexed { idx, img -> Page(idx, "", img.attr("data-src")) }

    override fun latestUpdatesRequest(page: Int) =
        throw UnsupportedOperationException("Not used")

    override fun latestUpdatesSelector() = ""

    override fun latestUpdatesNextPageSelector(): String? = null

    override fun latestUpdatesFromElement(element: Element) =
        throw UnsupportedOperationException("Not used")

    override fun imageUrlParse(document: Document) =
        throw UnsupportedOperationException("Not used")

    companion object {
        private val dateFormat by lazy {
            SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
        }
    }
}
