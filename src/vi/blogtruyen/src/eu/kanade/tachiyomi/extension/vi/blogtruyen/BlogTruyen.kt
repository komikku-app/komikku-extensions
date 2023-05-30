package eu.kanade.tachiyomi.extension.vi.blogtruyen

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

class BlogTruyen : ParsedHttpSource() {

    override val name = "BlogTruyen"

    override val baseUrl = "https://blogtruyen.vn"

    override val lang = "vi"

    override val supportsLatest = false

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimit(1)
        .build()

    private val json: Json by injectLazy()

    private val dateFormat: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH)

    private lateinit var directory: List<Element>

    override fun headersBuilder(): Headers.Builder =
        super.headersBuilder().add("Referer", "$baseUrl/")

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        return if (page == 1) {
            client.newCall(popularMangaRequest(page))
                .asObservableSuccess()
                .map { response ->
                    popularMangaParse(response)
                }
        } else {
            Observable.just(parseDirectory(page))
        }
    }

    override fun popularMangaRequest(page: Int): Request =
        GET("https://forum.blogtruyen.vn/anh-em-doc-tam/anh-em-doc-tam-75733")

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        directory = document.select(popularMangaSelector())
        return parseDirectory(1)
    }

    private fun parseDirectory(page: Int): MangasPage {
        val mangas = mutableListOf<SManga>()
        val endRange = ((page * 24) - 1).let { if (it <= directory.lastIndex) it else directory.lastIndex }

        for (i in (((page - 1) * 24)..endRange)) {
            mangas.add(popularMangaFromElement(directory[i]))
        }

        return MangasPage(mangas, endRange < directory.lastIndex)
    }

    override fun popularMangaSelector() = "div.topic-content div a"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        setUrlWithoutDomain(element.attr("href"))
        title = element.text()
        thumbnail_url = element.selectFirst("img")?.attr("src")
    }

    override fun popularMangaNextPageSelector(): String? = null

    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException("Unused")

    override fun latestUpdatesSelector(): String = throw UnsupportedOperationException("Unused")

    override fun latestUpdatesFromElement(element: Element): SManga = throw UnsupportedOperationException("Unused")

    override fun latestUpdatesNextPageSelector(): String? = null

    override fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): Observable<MangasPage> {
        return when {
            query.startsWith(PREFIX_ID_SEARCH) -> {
                var id = query.removePrefix(PREFIX_ID_SEARCH).trim()

                // it's a chapter, resolve to manga ID
                if (id.startsWith("c")) {
                    val document = client.newCall(GET("$baseUrl/$id", headers)).execute().asJsoup()
                    throwIfUnapprovedManga(document)

                    id = document.selectFirst(".breadcrumbs a:last-child")!!.attr("href").removePrefix("/")
                }

                fetchMangaDetails(
                    SManga.create().apply {
                        url = "/$id"
                    },
                )
                    .map { MangasPage(listOf(it.apply { url = "/$id" }), false) }
            }
            page == 1 -> client.newCall(searchMangaRequest(page, query, filters))
                .asObservableSuccess()
                .map { response ->
                    searchMangaParse(response, query, filters)
                }
            else -> Observable.just(parseDirectory(page))
        }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        popularMangaRequest(1)

    private fun searchMangaParse(response: Response, query: String, filters: FilterList): MangasPage {
        val trimmedQuery = query.trim()

        directory = response.asJsoup().select(popularMangaSelector()).filter { it ->
            it.text().contains(trimmedQuery, ignoreCase = true)
        }

        return parseDirectory(1)
    }

    override fun searchMangaSelector(): String = throw UnsupportedOperationException("Unused")

    override fun searchMangaFromElement(element: Element): SManga =
        throw UnsupportedOperationException("Unused")

    override fun searchMangaNextPageSelector(): String? = null

    private fun getMangaTitle(document: Document) = document.selectFirst(".entry-title a")!!
        .attr("title")
        .replaceFirst("truyện tranh", "", false)
        .trim()

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        throwIfUnapprovedManga(document)

        val anchor = document.selectFirst(".entry-title a")!!
        setUrlWithoutDomain(anchor.attr("href"))
        title = getMangaTitle(document)

        thumbnail_url = document.select(".thumbnail img").attr("abs:src")
        author = document.select("a[href*=tac-gia]").joinToString { it.text() }
        genre = document.select("span.category a").joinToString { it.text() }
        status = parseStatus(
            document.select("span.color-red:not(.bold)").text(),
        )

        description = StringBuilder().apply {
            // the actual synopsis
            val synopsisBlock = document.selectFirst(".manga-detail .detail .content")!!

            // replace the facebook blockquote in synopsis with the link (if there is one)
            val fbElement = synopsisBlock.selectFirst(".fb-page, .fb-group")
            if (fbElement != null) {
                val fbLink = fbElement.attr("data-href")

                val node = document.createElement("p")
                node.appendText(fbLink)

                fbElement.replaceWith(node)
            }
            appendLine(synopsisBlock.textWithNewlines().trim())
            appendLine()

            // other metadata
            document.select(".description p").forEach {
                val text = it.text()
                if (text.contains("Thể loại") ||
                    text.contains("Tác giả") ||
                    text.isBlank()
                ) {
                    return@forEach
                }

                if (text.contains("Trạng thái")) {
                    appendLine(text.substringBefore("Trạng thái").trim())
                    return@forEach
                }

                if (text.contains("Nguồn") ||
                    text.contains("Tham gia update") ||
                    text.contains("Nhóm dịch")
                ) {
                    val key = text.substringBefore(":")
                    val value = it.select("a").joinToString { el -> el.text() }
                    appendLine("$key: $value")
                    return@forEach
                }

                it.select("a, span").append("\\n")
                appendLine(it.text().replace("\\n", "\n").replace("\n ", "\n").trim())
            }
        }.toString().trim()
    }

    private fun Element.textWithNewlines() = run {
        select("p").prepend("\\n")
        select("br").prepend("\\n")
        text().replace("\\n", "\n").replace("\n ", "\n")
    }

    private fun parseStatus(status: String) = when {
        status.contains("Đang tiến hành") -> SManga.ONGOING
        status.contains("Đã hoàn thành") -> SManga.COMPLETED
        status.contains("Tạm ngưng") -> SManga.ON_HIATUS
        else -> SManga.UNKNOWN
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()

        throwIfUnapprovedManga(document)

        val title = getMangaTitle(document)
        return document.select(chapterListSelector()).map { chapterFromElement(it, title) }
    }

    override fun chapterListSelector() = "div.list-wrap > p"

    override fun chapterFromElement(element: Element): SChapter = throw UnsupportedOperationException("Not used")

    private fun chapterFromElement(element: Element, title: String): SChapter = SChapter.create().apply {
        val anchor = element.select("span > a").first()!!

        setUrlWithoutDomain(anchor.attr("href"))
        name = anchor.attr("title").replace(title, "", true).trim()
        date_upload = runCatching {
            dateFormat.parse(
                element.selectFirst("span.publishedDate")!!.text(),
            )?.time
        }.getOrNull() ?: 0L
    }

    private fun countViewRequest(mangaId: String, chapterId: String): Request = POST(
        "$baseUrl/Chapter/UpdateView",
        headers,
        FormBody.Builder()
            .add("mangaId", mangaId)
            .add("chapterId", chapterId)
            .build(),
    )

    private fun countView(document: Document) {
        val mangaId = document.getElementById("MangaId")!!.attr("value")
        val chapterId = document.getElementById("ChapterId")!!.attr("value")
        runCatching {
            client.newCall(countViewRequest(mangaId, chapterId)).execute().close()
        }
    }

    override fun pageListParse(document: Document): List<Page> {
        throwIfUnapprovedManga(document)

        val pages = mutableListOf<Page>()

        document.select("#content > img").forEachIndexed { i, e ->
            pages.add(Page(i, imageUrl = e.attr("abs:src")))
        }

        // Some chapters use js script to render images
        document.select("#content > script:containsData(listImageCaption)").lastOrNull()
            ?.let { script ->
                val imagesStr = script.data().substringBefore(";").substringAfterLast("=").trim()
                val imageArr = json.parseToJsonElement(imagesStr).jsonArray
                imageArr.forEach {
                    val imageUrl = it.jsonObject["url"]!!.jsonPrimitive.content
                    pages.add(Page(pages.size, imageUrl = imageUrl))
                }
            }

        countView(document)
        return pages
    }

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException("Not used")

    private fun isPageUnblocked(document: Document): Boolean {
        val anchor = document.selectFirst("center b a")?.attr("href") ?: return true
        val img = document.selectFirst("center img")?.attr("src") ?: return true

        val content = document.body().text()

        if (content != UNDERGOING_CHECK || anchor != APPROVED_MANGA_POST || img != DONATION_IMAGE) {
            return true
        }

        return false
    }

    private fun throwIfUnapprovedManga(document: Document) {
        if (!isPageUnblocked(document)) {
            throw Exception("Truyện chưa được kiểm duyệt!")
        }
    }

    companion object {
        const val PREFIX_ID_SEARCH = "id:"

        const val UNDERGOING_CHECK = "Website đang rà soát lại nội dung. Anh em đọc tạm một số truyện nhẹ nhàng lành mạnh đã được kiểm duyệt TẠI ĐÂY Website đang trong giai đoạn khó khăn, nhưng hãy donate chỉ khi bạn sẵn lòng và vui vẻ thôi nhé \uD83D\uDC96"
        const val APPROVED_MANGA_POST = "https://forum.blogtruyen.vn/anh-em-doc-tam/anh-em-doc-tam-75733"
        const val DONATION_IMAGE = "https://blogtruyen.vn/xin-donate.png"
    }
}
