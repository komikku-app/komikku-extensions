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
import org.jsoup.nodes.Element

class ManhwasNet : HttpSource() {
    override val baseUrl: String = "https://manhwas.net"
    override val lang: String = "es"
    override val name: String = "Manhwas.net"
    override val supportsLatest: Boolean = true

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select(".fa-book.d-inline-flex").map { chapterAnchor ->
            val chapterUrl = getUrlWithoutDomain(chapterAnchor.attr("href"))
            val chapterName = chapterUrl.substringAfterLast("-")
            val chapter = SChapter.create()
            chapter.chapter_number = chapterName.toFloat()
            chapter.name = chapterName
            chapter.url = chapterUrl
            chapter
        }
    }

    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException("Not used.")
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val content18 = document.select(".list-unstyled.row")[0]
        val content15 = document.select(".list-unstyled.row")[1]
        val manhwas = parseManhwas(content18) + parseManhwas(content15)
        return MangasPage(manhwas, false)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/es")
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        val profileManga = document.selectFirst(".anime-single")!!
        val manhwa = SManga.create()
        manhwa.title = profileManga.selectFirst(".title")!!.text()
        manhwa.thumbnail_url = profileManga.selectFirst("img")!!.attr("src")
        manhwa.description = profileManga.selectFirst(".sinopsis")!!.text().substringAfter(manhwa.title + " ")
        val status = profileManga.select(".anime-type-peli.text-white").text()
        manhwa.status = SManga.ONGOING
        if (!status.contains("Publicándose")) manhwa.status = SManga.COMPLETED

        return manhwa
    }

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        return document.select("#chapter_imgs img").mapIndexed { i, img ->
            var url = img.attr("src")
            if (url.toString() == "/discord.jpg") {
                url = "$baseUrl/discord.jpg"
            }
            Page(i, imageUrl = url)
        }
    }

    override fun popularMangaParse(response: Response): MangasPage {
        return parseLibraryMangas(response)
    }

    override fun popularMangaRequest(page: Int): Request {
        val url = "$baseUrl/biblioteca".toHttpUrlOrNull()!!.newBuilder()
        if (page > 1) {
            url.addQueryParameter("page", page.toString())
        }
        return GET(url.build().toString())
    }

    override fun searchMangaParse(response: Response): MangasPage {
        return parseLibraryMangas(response)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/biblioteca".toHttpUrlOrNull()!!.newBuilder()
        url.addQueryParameter("buscar", query)
        if (page > 1) {
            url.addQueryParameter("page", page.toString())
        }
        return GET(url.build().toString())
    }

    private fun parseLibraryMangas(response: Response): MangasPage {
        val document = response.asJsoup()
        val content = document.selectFirst(".animes")!!
        val manhwas = parseManhwas(content)
        val hasNextPage = document.selectFirst(".pagination .page-link[rel=\"next\"]") != null
        return MangasPage(manhwas, hasNextPage)
    }

    private fun parseManhwas(element: Element): List<SManga> {
        return element.select(".anime").map { anime ->
            val manhwa = SManga.create()
            manhwa.title = anime.selectFirst(".title")!!.text().trim()
            manhwa.thumbnail_url = anime.selectFirst("img")!!.attr("src")
            manhwa.url = getUrlWithoutDomain(
                transformUrl(anime.select("a").attr("href")),
            )
            manhwa
        }
    }

    private fun transformUrl(url: String): String {
        if (!url.contains("/leer/")) return url
        val name = url.substringAfter("/leer/").substringBeforeLast("-")
        return "$baseUrl/manga/$name"
    }

    private fun getUrlWithoutDomain(url: String) = url.substringAfter(baseUrl)
}
