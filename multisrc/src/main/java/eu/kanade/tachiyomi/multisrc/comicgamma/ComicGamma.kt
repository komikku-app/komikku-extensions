package eu.kanade.tachiyomi.multisrc.comicgamma

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

abstract class ComicGamma(
    override val name: String,
    override val baseUrl: String,
    override val lang: String = "ja",
) : ParsedHttpSource() {

    override val client = network.client.newBuilder().addInterceptor(PtImgInterceptor).build()

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/manga/", headers)
    override fun popularMangaNextPageSelector(): String? = null

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        fetchPopularManga(page).map { p -> MangasPage(p.mangas.filter { it.title.contains(query) }, false) }

    override fun searchMangaNextPageSelector() = throw UnsupportedOperationException("Not used.")
    override fun searchMangaSelector() = throw UnsupportedOperationException("Not used.")
    override fun searchMangaFromElement(element: Element) = throw UnsupportedOperationException("Not used.")
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        throw UnsupportedOperationException("Not used.")

    override fun pageListParse(document: Document) =
        document.select("#content > div[data-ptimg]").mapIndexed { i, e ->
            Page(i, imageUrl = e.attr("abs:data-ptimg"))
        }

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException("Not used.")

    companion object {
        internal fun SimpleDateFormat.parseJST(date: String) = parse(date)?.apply {
            time += 12 * 3600 * 1000 // updates at 12 noon
        }

        internal fun getJSTFormat(datePattern: String) =
            SimpleDateFormat(datePattern, Locale.JAPANESE).apply {
                timeZone = TimeZone.getTimeZone("GMT+09:00")
            }
    }
}
