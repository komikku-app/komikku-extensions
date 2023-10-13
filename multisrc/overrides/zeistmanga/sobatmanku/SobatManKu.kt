package eu.kanade.tachiyomi.extension.id.sobatmanku

import eu.kanade.tachiyomi.multisrc.zeistmanga.ZeistManga
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import okhttp3.Response

class SobatManKu : ZeistManga("SobatManKu", "https://www.sobatmanku19.site", "id") {

    override val supportsLatest = true

    override val hasFilters = true

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/search/label/Update", headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val doc = response.asJsoup()
        val selector = doc.select(".grid.gtc-f141a > div")
        val mangas = selector.map { element ->

            SManga.create().apply {
                element.select("a:nth-child(2)").let {
                    title = it.text()
                    setUrlWithoutDomain(it.attr("href"))
                }
                thumbnail_url = element.select("img").first()!!.attr("abs:src")
            }
        }

        return MangasPage(mangas, false)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        val profileManga = document.selectFirst(".grid.gtc-235fr")!!
        return SManga.create().apply {
            thumbnail_url = profileManga.selectFirst("img")!!.attr("abs:src")
            description = profileManga.select("#synopsis").text()
            genre = profileManga.select("div.mt-15 > a[rel=tag]").joinToString { it.text() }

            val infoElement = profileManga.select(".y6x11p")
            infoElement.forEach {
                val descText = it.select("span.dt").text()
                when (it.ownText().trim()) {
                    "Status" -> {
                        status = parseStatus(descText)
                    }

                    "Author" -> {
                        author = descText
                    }

                    "Artist" -> {
                        artist = descText
                    }
                }
            }
        }
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        return super.chapterListParse(response).onEach {
            // fix some chapter name
            it.name = it.name.run {
                substring(indexOf("Chapter"))
            }
        }
    }

    private fun parseStatus(element: String): Int = when (element.lowercase()) {
        "ongoing" -> SManga.ONGOING
        "completed" -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }
}
