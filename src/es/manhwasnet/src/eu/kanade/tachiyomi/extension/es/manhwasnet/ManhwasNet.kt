package eu.kanade.tachiyomi.extension.es.manhwasnet

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class ManhwasNet : HttpSource() {
    override val baseUrl: String = "https://manhwas.net"
    override val lang: String = "es"
    override val name: String = "Manhwas.net"
    override val supportsLatest: Boolean = true

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select(".eplister a").map { chapterEl ->
            val chapterUrl = getUrlWithoutDomain(chapterEl.attr("href"))
            val chapterName = chapterUrl.substringAfterLast("-")
            val chapter = SChapter.create()
            chapter.url = chapterUrl
            chapter.name = chapterName
            chapter.chapter_number = chapterName.toFloat()
            chapter
        }
    }

    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException("Not used.")
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val bixbox = document.selectFirst(".bixbox")
        val manhwas = parseManhwas(bixbox)
        return MangasPage(manhwas, false)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/es")
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()

        val manhwa = SManga.create()

        manhwa.title = document.selectFirst(".entry-title").text()
        manhwa.thumbnail_url = document.selectFirst(".thumb img").attr("src")
        manhwa.description = document.selectFirst(".entry-content").text()

        val estado = document.select(".imptdt a")[1]
        manhwa.status = SManga.ONGOING
        if (estado.text() == "Finalizado") manhwa.status = SManga.COMPLETED

        return manhwa
    }

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        return document.select("#imgs img").mapIndexed { i, img ->
            val url = img.attr("src")
            Page(i, imageUrl = url)
        }
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val manhwas = parseManhwas(document)
        val hasNextPage = document.selectFirst(".wpag-li.page-item [rel=\"next\"]") != null
        return MangasPage(manhwas, hasNextPage)
    }

    override fun popularMangaRequest(page: Int): Request {
        val url = "$baseUrl/biblioteca".toHttpUrlOrNull()!!.newBuilder()
        if (page > 1) {
            url.addQueryParameter("page", page.toString())
        }
        return GET(url.build().toString())
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val manhwas = parseManhwas(document)
        val hasNextPage = document.selectFirst(".wpag-li.page-item [rel=\"next\"]") != null
        return MangasPage(manhwas, hasNextPage)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/biblioteca".toHttpUrlOrNull()!!.newBuilder()
        url.addQueryParameter("buscar", query)
        if (page > 1) {
            url.addQueryParameter("page", page.toString())
        }
        return GET(url.build().toString())
    }

    private fun parseManhwas(element: Element): List<SManga> {
        return element.select(".bsx").map { bxs ->
            val manhwa = SManga.create()
            manhwa.url = getUrlWithoutDomain(
                transformUrl(bxs.selectFirst("a").attr("href"))
            )
            manhwa.title = bxs.selectFirst(".tt").text().trim()
            manhwa.thumbnail_url = bxs.selectFirst(".ts-post-image").attr("src")
            manhwa
        }
    }

    private fun parseManhwas(document: Document): List<SManga> {
        return parseManhwas(document.body())
    }

    private fun transformUrl(url: String): String {
        if (!url.contains("/leer/")) return url
        val name = url.substringAfter("/leer/").substringBeforeLast("-")
        return "$baseUrl/manga/$name"
    }

    private fun getUrlWithoutDomain(url: String) = url.substringAfter(baseUrl)
}
