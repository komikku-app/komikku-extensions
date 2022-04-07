package eu.kanade.tachiyomi.multisrc.mangaraw

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Headers
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Common parsers of mangaraw sites, follow manga1001.top by default.
 */
abstract class MangaRaw(
    override val name: String,
    override val baseUrl: String,
) : ParsedHttpSource() {

    protected open val imageSelector = ".wp-block-image > img"

    override val lang = "ja"

    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder {
        return super.headersBuilder().add("Referer", baseUrl)
    }

    // manga1001 and mangapro support rank by catalog + year/week. all catalog + week by default.
    override fun popularMangaRequest(page: Int): Request =
        GET("$baseUrl/seachlist/page/$page/?cat=-1&stime=1", headers)

    override fun popularMangaSelector() = "article"

    override fun popularMangaFromElement(element: Element) = SManga.create().apply {
        // FIXME: when manga has different domain, such as manga on '新刊コミック' page, this manga will cause "Too many follow-up requests: 21"
        setUrlWithoutDomain(element.select("a:has(img)").attr("href"))
        title = element.select("img").attr("alt").substringBefore("(RAW").trim()
        thumbnail_url = element.select("img").attr("data-src")
    }

    override fun popularMangaNextPageSelector() = ".next.page-numbers"

    // manga101 and mangapro have a '新刊コミック' page, but all manga under syosetu.top.
    // visit these manga will cause "Too many follow-up requests: 21" and this make latest update complete unusable
    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/newmanga/page/$page", headers)

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    // there's no next page button on latest manga page
    override fun latestUpdatesNextPageSelector(): String? { return null }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        GET("$baseUrl/page/$page/?s=$query", headers)

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        // All manga details are located in the same <p> tag
        description = document.select(".entry-content > p").text()
    }

    override fun chapterListSelector() = ".chaplist a"

    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        url = element.attr("href")
        name = element.text().trim()
    }

    override fun pageListParse(document: Document): List<Page> {
        return document.select(imageSelector).mapIndexed { i, element ->
            val attribute = if (element.hasAttr("data-src")) "data-src" else "src"
            Page(i, "", element.attr(attribute))
        }
    }

    override fun imageUrlParse(document: Document): String =
        throw UnsupportedOperationException("Not Used")

    override fun getFilterList() = FilterList()
}
