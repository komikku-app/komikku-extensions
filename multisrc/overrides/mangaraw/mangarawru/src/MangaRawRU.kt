package eu.kanade.tachiyomi.extension.ja.mangarawru

import eu.kanade.tachiyomi.multisrc.mangaraw.ImageListParser
import eu.kanade.tachiyomi.multisrc.mangaraw.MangaRawTheme
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator

class MangaRawRU : MangaRawTheme("MangaRawRU", "https://mangaraw.ru") {
    override fun popularMangaRequest(page: Int): Request = latestUpdatesRequest(page)
    override val supportsLatest = false

    override fun String.sanitizeTitle(): String {
        val index = lastIndexOf("Raw", ignoreCase = true)
        if (index == -1) return this
        return substring(0, index)
            .trimEnd('(', ' ', ',')
    }

    override fun popularMangaSelector() = "#list_videos_videos_items:nth-child(2) .thumb.item>a"

    override fun popularMangaFromElement(element: Element) = SManga.create().apply {
        setUrlWithoutDomain(element.attr("href"))
        title = element.select("h3").text().sanitizeTitle()
        thumbnail_url = element.select("img").attr("abs:src")
    }

    override fun popularMangaNextPageSelector() = ".pagination__link"

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        GET("$baseUrl/?s=$query&page=$page")

    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

    override fun searchMangaSelector(): String = ".thumb.item>a"

    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        manga.title = document.select("h1").text().sanitizeTitle()
        manga.description = document.select(".video-info__box.b-content strong").text()
        manga.thumbnail_url = document.select(".player-holder img").attr("abs:src")

        val genres = document.select(".category")
            .map { element -> element.text() }
            .toMutableSet()

        manga.genre = genres.toList().joinToString(", ")

        return manga
    }

    override fun Document.getSanitizedDetails(): Element = this
    override fun chapterListSelector() = ".chapter-l a"
    override fun String.sanitizeChapter() = substringAfterLast(" – ").substringBeforeLast("漫画")

    override fun pageSelector(): Evaluator {
        return Evaluator.Tag("img")
    }

    override fun pageListParse(document: Document): List<Page> {
        val position = 32
        val parser = ImageListParser(document.html(), position)

        return parser.getImageList().orEmpty().mapIndexed { i, imageUrl ->
            Page(i, imageUrl = imageUrl)
        }
    }
}
