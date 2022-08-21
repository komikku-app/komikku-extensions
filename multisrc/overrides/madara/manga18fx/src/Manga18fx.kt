package eu.kanade.tachiyomi.extension.en.manga18fx

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import java.text.SimpleDateFormat
import java.util.Locale

class Manga18fx : Madara(
    "Manga18fx",
    "https://manga18fx.com",
    "en",
    SimpleDateFormat("dd MMM yy", Locale.ENGLISH)
) {
    override val id = 3157287889751723714

    override val client = network.client

    override val fetchGenres = false
    override val sendViewCount = false

    override fun popularMangaRequest(page: Int) = GET(baseUrl, headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val block = document.selectFirst(Evaluator.Class("trending-block"))
        val mangas = block.select(Evaluator.Tag("a")).map(::mangaFromElement)
        return MangasPage(mangas, false)
    }

    private fun mangaFromElement(element: Element) = SManga.create().apply {
        url = element.attr("href")
        title = element.attr("title")
        thumbnail_url = element.selectFirst(Evaluator.Tag("img")).attr("data-src")
    }

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/page/$page", headers)

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(Evaluator.Class("bsx-item")).map {
            mangaFromElement(it.selectFirst(Evaluator.Tag("a")))
        }
        val nextButton = document.selectFirst(Evaluator.Class("next"))
        val hasNextPage = nextButton != null && nextButton.hasClass("disabled").not()
        return MangasPage(mangas, hasNextPage)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        GET("$baseUrl/search?q=$query&page=$page", headers)

    override fun searchMangaParse(response: Response) = latestUpdatesParse(response)

    override val mangaDetailsSelectorDescription = ".dsct"

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val container = document.selectFirst(Evaluator.Class("row-content-chapter"))
        return container.children().map(::chapterFromElement)
    }

    override fun chapterDateSelector() = "span.chapter-time"

    override fun getFilterList() = FilterList(emptyList())
}
