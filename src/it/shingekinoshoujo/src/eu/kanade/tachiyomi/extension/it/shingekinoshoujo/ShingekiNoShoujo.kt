package eu.kanade.tachiyomi.extension.it.shingekinoshoujo

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class ShingekiNoShoujo : ParsedHttpSource() {

    override val name = "Shingeki no Shoujo"
    override val baseUrl = "https://shingekinoshoujo.it"
    override val lang = "it"
    override val supportsLatest = true
    override val client: OkHttpClient = network.cloudflareClient

    //region REQUESTS
    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/", headers)
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/category/novita/", headers)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isNotEmpty()) {
            return GET("$baseUrl/page/$page/?s=$query", headers)
        } else {
            (if (filters.isEmpty()) getFilterList() else filters).forEach { filter ->
                when (filter) {
                    is GenreSelez -> {
                        return GET(
                            "$baseUrl/tag/${getGenreList().filter {
                                filter.values[filter.state] == it.name
                            }.map { it.id }[0]}/page/$page",
                        )
                    }
                    else -> {}
                }
            }
            return GET(baseUrl, headers)
        }
    }
    //endregion

    //region CONTENTS INFO
    private fun mangasParse(response: Response, selector: String, num: Int): MangasPage {
        val document = response.asJsoup()
        if (document.select("#login > .custom-message").size > 0) throw Exception("Devi accedere al sito web con il tuo account!\nPremi WebView (il pulsante con il globo)\ne accedi normalmente")

        val mangas = document.select(selector).map { element ->
            when (num) {
                1 -> popularMangaFromElement(element)
                2 -> latestUpdatesFromElement(element)
                else -> searchMangaFromElement(element)
            }
        }
        return MangasPage(
            mangas,
            !document.select(searchMangaNextPageSelector()).isEmpty(),
        )
    }
    override fun popularMangaParse(response: Response): MangasPage = mangasParse(response, popularMangaSelector(), 1)
    override fun latestUpdatesParse(response: Response): MangasPage = mangasParse(response, latestUpdatesSelector(), 2)
    override fun searchMangaParse(response: Response): MangasPage = mangasParse(response, searchMangaSelector(), 3)

    override fun popularMangaSelector() = ".lp-box"
    override fun latestUpdatesSelector() = "article"
    override fun searchMangaSelector() = "article.type-post"

    override fun popularMangaFromElement(element: Element) = SManga.create().apply {
        thumbnail_url = element.selectFirst("img")!!.attr("src")
        element.select("a").first()!!.let {
            setUrlWithoutDomain(it.attr("href"))
            title = it.text()
        }
    }
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

    override fun mangaDetailsParse(document: Document): SManga {
        val statusElementText = document.select("blockquote").last()!!.text().lowercase()
        return SManga.create().apply {
            thumbnail_url = document.select(".header-image").attr("src")
            status = when {
                statusElementText.contains("in corso") -> SManga.ONGOING
                statusElementText.contains("fine") -> SManga.COMPLETED
                statusElementText.contains("diritti") -> SManga.LICENSED
                statusElementText.contains("sospeso") -> SManga.CANCELLED
                statusElementText.contains("hiatus") -> SManga.ON_HIATUS
                else -> SManga.UNKNOWN
            }
            author = document.select("span:has(strong:contains(Autore))").text().substringAfter("Autore:").trim()
            genre = document.select("span:has(strong:contains(Genere))").text().substringAfter("Genere:").replace(".", "").trim()
            description = document.select("p:has(strong:contains(Trama))").text().substringAfter("Trama:").trim()
        }
    }
    //endregion

    //region NEXT SELECTOR  -  Not used

    override fun popularMangaNextPageSelector(): String? = null
    override fun latestUpdatesNextPageSelector() = searchMangaNextPageSelector()
    override fun searchMangaNextPageSelector() = ".next.page-numbers"
    //endregion

    //region CHAPTER and PAGES

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val chapters = mutableListOf<SChapter>()
        document.select(chapterListSelector().replace("<chapName>", document.location().substringAfter("$baseUrl/").substringBefore("/"))).forEachIndexed { i, it ->
            chapters.add(
                SChapter.create().apply {
                    setUrlWithoutDomain(it.attr("href"))
                    name = it.text()
                    chapter_number = it.text().replace(Regex("OneShot|Prologo"), "0").filter { it.isDigit() }.let {
                        it.ifEmpty { "$i" }
                    }.toFloat()
                },
            )
        }

        chapters.reverse()
        return chapters
    }
    override fun chapterListSelector() = ".entry-content > ul > li"
    override fun chapterFromElement(element: Element) = throw Exception("Not used")

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()

        document.select(".alignnone").forEachIndexed { i, it ->
            pages.add(Page(i, "", it.attr("src")))
        }
        if (document.toString().contains("che state leggendo")) {
            pages.add(Page(1, "", "https://i.imgur.com/l0eZuoO.png"))
        }

        return pages
    }

    override fun imageUrlParse(document: Document) = ""
    override fun imageRequest(page: Page): Request {
        val imgHeader = Headers.Builder().apply {
            add("User-Agent", "Mozilla/5.0 (Linux; U; Android 4.1.1; en-gb; Build/KLP) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Safari/534.30")
            add("Referer", baseUrl)
        }.build()
        return GET(page.imageUrl!!, imgHeader)
    }
    //endregion

    //region FILTERS
    private class Genre(name: String, val id: String = name) : Filter.CheckBox(name)
    private class GenreSelez(genres: List<Genre>) : Filter.Select<String>(
        "Genere",
        genres.map {
            it.name
        }.toTypedArray(),
        0,
    )

    override fun getFilterList() = FilterList(
        Filter.Header("La ricerca testuale non accetta i filtri e viceversa"),
        GenreSelez(getGenreList()),
    )

    private fun getGenreList() = listOf(
        Genre("Josei", "josei"),
        Genre("Sportivo", "sportivo"),
        Genre("Harlequin", "harlequin"),
        Genre("Ufficio", "ufficio"),
        Genre("Shoujo", "shoujo"),
        Genre("Fujitani Yoko", "fujitani-yoko"),
        Genre("Tsukishima Haru", "tsukishima-haru"),
        Genre("Scolastico", "scolastico"),
        Genre("Viaggio nel Tempo", "viaggio-nel-tempo"),
        Genre("Vita Scolastica", "vita-scolastica"),
        Genre("Seinen", "seinen"),
        Genre("Drammatico", "drammatico"),
        Genre("Commedia", "commedia"),
        Genre("Drama", "drama"),
        Genre("Oneshot", "oneshot"),
        Genre("Mistero", "mistero"),
        Genre("Saori", "saori"),
        Genre("Fuji Momo", "fuji-momo"),
        Genre("Azione", "azione"),
        Genre("Sovrannaturale", "sovrannaturale"),
        Genre("Vita Quotidiana", "vita-quotidiana"),
        Genre("Storico", "storico"),
        Genre("Shibano Yuka", "shibano-yuka"),
        Genre("Fantasy", "fantasy"),
        Genre("Smut", "smut"),
        Genre("Psicologico", "psicologico"),
    )
    //endregion
}
