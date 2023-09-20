package eu.kanade.tachiyomi.extension.all.manhwa18net

import eu.kanade.tachiyomi.multisrc.fmreader.FMReader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.Request
import org.jsoup.nodes.Element

class Manhwa18NetFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        Manhwa18Net(),
        Manhwa18NetRaw(),
    )
}

class Manhwa18Net : FMReader("Manhwa18.net", "https://manhwa18.net", "en") {
    override val requestPath = "genre/manhwa"
    override val popularSort = "sort=top"
    override val pageListImageSelector = "div#chapter-content > img"

    override fun latestUpdatesRequest(page: Int): Request =
        GET(
            "$baseUrl/$requestPath?listType=pagination&page=$page&sort=update&sort_type=DESC",
            headers,
        )

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val noRawsUrl = super.searchMangaRequest(page, query, filters).url.newBuilder().toString()
        return GET(noRawsUrl, headers)
    }

    override fun getGenreList() = getAdultGenreList()

    override fun chapterFromElement(element: Element, mangaTitle: String): SChapter {
        return SChapter.create().apply {
            setUrlWithoutDomain(element.attr("abs:href"))
            name = element.attr("title")
            date_upload = parseAbsoluteDate(
                element.select(chapterTimeSelector).text().substringAfter(" - "),
            )
        }
    }
}

class Manhwa18NetRaw : FMReader("Manhwa18.net", "https://manhwa18.net", "ko") {
    override val requestPath = "genre/raw"
    override val popularSort = "sort=top"
    override val pageListImageSelector = "div#chapter-content > img"

    override fun latestUpdatesRequest(page: Int): Request =
        GET(
            "$baseUrl/$requestPath?listType=pagination&page=$page&sort=update&sort_type=DESC",
            headers,
        )

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val onlyRawsUrl = super.searchMangaRequest(page, query, filters).url.newBuilder().toString()
        return GET(onlyRawsUrl, headers)
    }

    override fun getFilterList() = FilterList(
        super.getFilterList().filterNot { it == GenreList(getGenreList()) },
    )

    override fun chapterFromElement(element: Element, mangaTitle: String): SChapter {
        return SChapter.create().apply {
            setUrlWithoutDomain(element.attr("abs:href"))
            name = element.attr("title")
            date_upload = parseAbsoluteDate(
                element.select(chapterTimeSelector).text().substringAfter(" - "),
            )
        }
    }
}
