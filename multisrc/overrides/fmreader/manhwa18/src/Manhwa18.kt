package eu.kanade.tachiyomi.extension.en.manhwa18

import eu.kanade.tachiyomi.multisrc.fmreader.FMReader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class Manhwa18 : FMReader("Manhwa18", "https://manhwa18.com", "en") {
    override val requestPath = "tim-kiem"

    override val popularSort = "sort=top"

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select(popularMangaSelector()).map { popularMangaFromElement(it) }

        return MangasPage(mangas, document.select(".pagination_wrap .disabled").text() != "Bottom")
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/$requestPath?".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("page", page.toString())
        return GET(url.toString(), headers)
    }
    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/$requestPath?listType=pagination&page=$page&sort=update&sort_type=DESC", headers)

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            title = document.select(".series-name").text()
            thumbnail_url = document.select("meta[property='og:image']").attr("abs:content")

            document.select(".series-information")?.let { info ->
                author = info.select(".info-name:contains(Author:) + .info-value").text()
                genre = info.select(".info-name:contains(Genre:) + .info-value > a")
                    .joinToString { it.text().trim() }

                description = document.select(".summary-content").text().trim()
                info.select(".info-name:contains(Other name:) + .info-value")
                    .firstOrNull()?.text()?.let {
                        val altName = removeGenericWords(it)
                        description = when (title.lowercase(Locale.US)) {
                            altName.lowercase(Locale.US) -> description
                            else -> description + "\n\n$altName"
                        }
                    }
                status =
                    parseStatus(info.select(".info-name:contains(Status:) + .info-value").text())
            }
        }
    }

    private fun removeGenericWords(name: String): String {
        val excludeList = listOf("manhwa", "engsub")
        return name.split(' ').filterNot { word ->
            word.lowercase(Locale.US) in excludeList
        }.joinToString(" ")
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()

        return document.select(".list-chapters > a").map { element ->
            SChapter.create().apply {
                setUrlWithoutDomain(element.attr("abs:href"))
                name = element.attr("title")
                date_upload =
                    SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(
                        element.select(".chapter-time").text().substringAfter(" - "),
                    )?.time ?: 0L
                chapter_number = element.attr("time").substringAfterLast(' ').toFloatOrNull() ?: -1f
            }
        }
    }

    override val pageListImageSelector = "#chapter-content > img"

    override fun getFilterList() = FilterList()
}
