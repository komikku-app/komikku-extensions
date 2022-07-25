package eu.kanade.tachiyomi.extension.ja.manga9co

import eu.kanade.tachiyomi.multisrc.mangaraw.MangaRaw
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Manga9co : MangaRaw("Manga9co", "https://manga9.co/") {

    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/top/?page=$page", headers)

    override fun popularMangaSelector() = ".col-sm-4.my-2"

    override fun popularMangaNextPageSelector() = "nextpostslink"

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/page/$page", headers)

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        GET("$baseUrl/?s=$query&page=$page", headers)

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        description = document.select("strong").last().text().trim()
    }

    override fun chapterListSelector() = ".list-scoll a"

    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        url = element.attr("href").replace(baseUrl, "")
        name = element.text().trim()
    }

    override val imageSelector = ".card-wrap > img"
}
