package eu.kanade.tachiyomi.extension.all.erocool

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

class EroCool(
    override val lang: String,
    private val langName: String,
    number: Int
) : ParsedHttpSource() {
    override val name = "EroCool"

    override val baseUrl = "https://$lang.erocool$number.com"

    override val supportsLatest = true

    override fun latestUpdatesSelector() = GALLERY_SELECTOR

    override fun latestUpdatesNextPageSelector() = NEXT_PAGE_SELECTOR

    override fun latestUpdatesRequest(page: Int) =
        GET("$baseUrl/language/$langName/page/$page", headers)

    override fun latestUpdatesFromElement(element: Element) = element.toManga()

    override fun popularMangaSelector() = GALLERY_SELECTOR

    override fun popularMangaNextPageSelector() = NEXT_PAGE_SELECTOR

    override fun popularMangaRequest(page: Int) =
        GET("$baseUrl/language/$langName/popular/page/$page", headers)

    override fun popularMangaFromElement(element: Element) = element.toManga()

    override fun searchMangaSelector() = GALLERY_SELECTOR

    override fun searchMangaNextPageSelector() = NEXT_PAGE_SELECTOR

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        if (query.isBlank()) popularMangaRequest(page)
        else GET("$baseUrl/search/q_$query $langName/page/$page", headers)

    override fun searchMangaFromElement(element: Element) = element.toManga()

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        description = document.selectFirst(".breadtitle")?.text()
        genre = document.select(TAGS_SELECTOR)?.joinToString { it.text() }
        artist = document.select(ARTISTS_SELECTOR)?.joinToString { it.text() }
        author = document.select(GROUPS_SELECTOR)
            ?.joinToString { it.text() }?.ifEmpty { artist } ?: artist
    }

    override fun chapterListSelector() = "#comicdetail"

    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        name = "Chapter"
        chapter_number = -1f
        date_upload = element.uploadDate()
        setUrlWithoutDomain(element.baseUri())
    }

    override fun pageListParse(document: Document) =
        document.select(".vimg.lazyload").mapIndexed { idx, img ->
            Page(idx, "", img.absUrl("data-src"))
        }

    override fun imageUrlParse(document: Document) =
        throw UnsupportedOperationException("Not used")

    private fun Element.toManga() = SManga.create().also {
        it.url = attr("href")
        it.title = selectFirst(".caption").attr("title")
        it.thumbnail_url = selectFirst(".list-content")
            .attr("style").substringAfter('(').substringBefore(')')
    }

    private fun Element.uploadDate() =
        dateFormat.parse(selectFirst(DATE_SELECTOR).text())?.time ?: 0L

    companion object {
        private const val GALLERY_SELECTOR = ".gallery"

        private const val NEXT_PAGE_SELECTOR = ".list-p-li > a[rel=next]"

        private const val DATE_SELECTOR = ".ld_box > div:first-child > .ld_body"

        private const val GROUPS_SELECTOR = ".ld_boxs .ld_bodys[href^=/group/]"

        private const val ARTISTS_SELECTOR = ".ld_boxs .ld_bodys[href^=/artist/]"

        private const val TAGS_SELECTOR =
            ".ld_boxs .ld_bodys[href^=/parody/]," +
                ".ld_boxs .ld_bodys[href^=/tag/]," +
                ".ld_boxs .ld_bodys[href^=/category/]"

        private val dateFormat by lazy {
            SimpleDateFormat("yyyy/MM/dd", Locale.ROOT)
        }
    }
}
