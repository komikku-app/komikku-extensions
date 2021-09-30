package eu.kanade.tachiyomi.extension.ru.glavikrestyanam

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class glavikrestyanam : ParsedHttpSource() {

    override val name = "GlaviKrestyanam"

    override val baseUrl = "https://glavikrestyanam.ru"

    override val lang = "ru"

    override val supportsLatest = true

    override fun popularMangaRequest(page: Int): Request =
        GET("$baseUrl/manga?page=$page", headers)

    override fun latestUpdatesRequest(page: Int): Request =
        GET(baseUrl, headers)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/search?query=$query&sort=rating_short&page=$page")
    }

    override fun popularMangaSelector() = "div.p-2"

    override fun latestUpdatesSelector() = "div.row"

    override fun searchMangaSelector() = "div.col-12 > div"

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.thumbnail_url = element.select("img").first().attr("src")
        element.select("a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.thumbnail_url = element.select("img").first().attr("src")
        element.select("a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
        }
        element.select("div.col-6").first().let {
            manga.title = it.text()
        }
        return manga
    }

    override fun searchMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.thumbnail_url = element.select("img").first().attr("src")
        element.select("a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }

    override fun popularMangaNextPageSelector() = "a.page-link[rel=next]"

    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    override fun mangaDetailsParse(document: Document): SManga {
        val infoElement = document.select("div.col-lg").first()
        val manga = SManga.create()
        manga.author = infoElement.select(".div.col-lg > h2 > a").text().split(":")[1]
        manga.description = infoElement.ownText()
        return manga
    }

    override fun chapterListSelector() = "div.chapters-list > div"

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select("a").first()
        val chapter = SChapter.create()
        element.select("div.col-5").first().let {
            chapter.name = it.text()
        }
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        return chapter
    }

    override fun pageListParse(document: Document): List<Page> {
        return document.select("div.ch-show > img").mapIndexed { index, element ->
            Page(index, "", getImage(element))
        }
    }

    private fun getImage(first: Element): String? {
        val image = first.attr("data-src")
        if (image.isNotEmpty()) {
            return image
        }
        return first.attr("src")
    }

    override fun imageUrlParse(document: Document) = ""
}
