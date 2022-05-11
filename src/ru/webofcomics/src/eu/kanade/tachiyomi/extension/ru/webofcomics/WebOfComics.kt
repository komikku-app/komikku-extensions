package eu.kanade.tachiyomi.extension.ru.webofcomics

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class WebOfComics : ParsedHttpSource() {

    override val name = "Web of Comics"

    override val baseUrl = "https://webofcomics.ru"

    override val lang = "ru"

    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36 Edg/100.0.1185.50")
        .add("Referer", baseUrl)

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor(RateLimitInterceptor(3))
        .build()

    override fun popularMangaRequest(page: Int): Request {
        return POST(
            "$baseUrl/page/$page",
            body = FormBody.Builder()
                .add("dlenewssortby", "rating")
                .add("dledirection", "desc")
                .add("set_new_sort", "dle_sort_main")
                .add("set_direction_sort", "dle_direction_main")
                .build(),
            headers = headers
        )
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return POST(
            "$baseUrl/page/$page",
            body = FormBody.Builder()
                .add("dlenewssortby", "date")
                .add("dledirection", "desc")
                .add("set_new_sort", "dle_sort_main")
                .add("set_direction_sort", "dle_direction_main")
                .build(),
            headers = headers
        )
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return POST(
            "$baseUrl/index.php?do=search",
            body = FormBody.Builder()
                .add("do", "search")
                .add("subaction", "search")
                .add("story", query)
                .add("search_start", page.toString())
                .build(),
            headers = headers
        )
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select(searchMangaSelector()).map { element ->
            searchMangaFromElement(element)
        }
        return MangasPage(mangas, mangas.isNotEmpty())
    }

    override fun popularMangaSelector() = ".movie-item"

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun searchMangaSelector() = popularMangaSelector()

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.thumbnail_url = baseUrl + element.select(".lazyload").first().attr("data-src").replace("/thumbs", "")
        element.select(".movie-title").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.html().substringBefore("<div>")
        }
        return manga
    }

    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

    override fun popularMangaNextPageSelector() = ".pnext a"

    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    override fun searchMangaNextPageSelector(): String? = null

    private fun parseStatus(status: String): Int {
        return when (status) {
            "Завершён" -> SManga.COMPLETED
            "Продолжается" -> SManga.ONGOING
            else -> SManga.UNKNOWN
        }
    }

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val infoElement = document.select(".page-cols").first()
        val infoElement2 = document.select(".m-info2 .sliceinfo1")
        title = infoElement.select("h1").first().text()
        thumbnail_url = baseUrl + infoElement.select(".lazyload").first().attr("data-src")
        description = document.select(".slice-this").first().text().substringAfter("Описание:").trim()
        author = infoElement2.select(":contains(Автор) a").joinToString { it.text() }
        if (author.isNullOrEmpty())
            author = infoElement.select(".mi-item:contains(Издательство)").first().text()
        artist = infoElement2.select(":contains(Художник) a").joinToString { it.text() }
        genre = (infoElement.select(".mi-item:contains(Тип) a") + infoElement.select(".mi-item:contains(Возраст) a") + infoElement.select(".mi-item:contains(Формат) a") + infoElement.select(".mi-item:contains(Жанр) a")).joinToString { it.text() }
        status = if (document.toString().contains("Удалено по просьбе правообладателя"))
            SManga.LICENSED
        else
            parseStatus(infoElement.select(".mi-item:contains(Перевод) a").first().text())
    }

    override fun chapterListRequest(manga: SManga): Request {
        val TypeSeries =
            with(manga.url) {
                when {
                    contains("/manga/") -> "xsort='tommanga,glavamanga' template='custom-linkstocomics-xfmanga-guest'"
                    contains("/comics/") -> "xsort='number' template='custom-linkstocomics-xfcomics-guest'"
                    else -> "error"
                }
            }
        return POST(
            baseUrl + "/engine/ajax/customajax.php",
            body = FormBody.Builder()
                .add("castom", "custom senxf='fastnavigation|${manga.url.substringAfterLast("/").substringBefore("-")}' $TypeSeries limit='3000' sort='asc' cache='yes'")
                .build(),
            headers = headers
        )
    }

    override fun chapterListSelector() = ".ltcitems:has(a:not(.alttranslatelink))"

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()
        element.select("a").first().let {
            chapter.name = it.text().substringAfterLast(":")
            chapter.chapter_number = it.text().substringAfter("Глава").substringAfter("#").substringBefore("-").toFloatOrNull() ?: -1f
            chapter.setUrlWithoutDomain(it.attr("href"))
        }
        chapter.date_upload = simpleDateFormat.parse(element.select("div").first().text().trim())?.time ?: 0L
        return chapter
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        return super.chapterListParse(response).reversed()
    }

    override fun pageListParse(document: Document): List<Page> {
        var baseImgUrl = document.select("link[rel='image_src']").last().attr("href")

        val publicUrl = "/public_html"
        val uploadUrl =
            with(baseImgUrl) {
                when {
                    contains("/uploads/") -> "/uploads/"
                    contains("/manga/") -> "/manga/"
                    contains("/mangaparser/") -> "/mangaparser/"
                    else -> "errorUploads"
                }
            }
        baseImgUrl = baseImgUrl.substringBefore(uploadUrl)
        if (baseImgUrl.contains(publicUrl))
            baseImgUrl =
                baseImgUrl.substringBefore(publicUrl) + "/www/" +
                baseUrl.substringAfter("://") + publicUrl +
                baseImgUrl.substringAfter(publicUrl)

        if (document.select(".readtab .lazyload").isNotEmpty()) {
            return document.select(".readtab .lazyload").mapIndexed { index, element ->
                Page(
                    index,
                    "",
                    baseImgUrl + uploadUrl + element.attr("data-src").substringAfter(uploadUrl)
                )
            }
        } else {
            val counterPageStr = document.select("#comics script").toString()

            val startPageStr = counterPageStr
                .substringAfter("for(var i =")
                .substringBefore("; i <")
                .trim()
            var endPageStr = counterPageStr
                .substringAfter("; i <")
                .substringBefore("; i++)")
                .trim()

            if (endPageStr.contains("="))
                endPageStr = (endPageStr.replace("=", "").trim().toInt() + 1).toString()

            if (baseImgUrl.contains("/share."))
                baseImgUrl = counterPageStr
                    .substringAfter("data-src=\"")
                    .substringBefore("' + i")
                    .trim().replace("https://feik.domain.ru/", "https://read.webofcomics.ru/webofcomics.ru/www/webofcomics.ru/public_html/") +
                    counterPageStr
                        .substringAfter("i + '")
                        .substringBefore("\">")
                        .trim()

            var subPage = ""

            return (startPageStr.toInt() until endPageStr.toInt()).mapIndexed { index, page ->
                if (startPageStr == "0") {
                    subPage = when {
                        page < 10 -> "00"
                        page < 100 -> "0"
                        else -> ""
                    }
                }
                Page(
                    index,
                    "",
                    baseImgUrl.substringBeforeLast("/") + "/$subPage$page." + baseImgUrl.substringAfterLast(".")
                )
            }
        }
    }

    override fun imageUrlParse(document: Document) = ""

    companion object {
        private val simpleDateFormat by lazy { SimpleDateFormat("dd.MM.yyyy", Locale.US) }
    }
}
