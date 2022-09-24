package eu.kanade.tachiyomi.extension.zh.sixmh

import eu.kanade.tachiyomi.AppInfo
import eu.kanade.tachiyomi.lib.unpacker.Unpacker
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import rx.Observable
import rx.Single
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

class SixMH : ParsedHttpSource() {
    override val name = "6漫画"
    override val lang = "zh"
    override val baseUrl = PC_URL
    override val supportsLatest = true

    private val json: Json by injectLazy()

    override val client = network.client.newBuilder()
        .rateLimit(2)
        .build()

    override fun popularMangaRequest(page: Int) = GET("$PC_URL/rank/1-$page.html", headers)
    override fun popularMangaNextPageSelector() = "li.thisclass:not(:last-of-type)"
    override fun popularMangaSelector() = "div.cy_list_mh > ul"
    override fun popularMangaFromElement(element: Element) = SManga.create().apply {
        with(element.child(1).child(0)) {
            url = attr("href")
            title = ownText()
        }
        thumbnail_url = element.selectFirst(Evaluator.Tag("img")).attr("src")
    }

    override fun latestUpdatesRequest(page: Int) = GET("$PC_URL/rank/5-$page.html", headers)
    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()
    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()
    override fun searchMangaSelector() = popularMangaSelector()
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isNotBlank()) {
            return GET("$PC_URL/search.php?keyword=$query", headers)
        } else {
            filters.filterIsInstance<PageFilter>().firstOrNull()?.run {
                return GET("$PC_URL$path$page.html", headers)
            }
            return popularMangaRequest(page)
        }
    }

    private fun pcRequest(manga: SManga) = GET("$PC_URL${manga.url}", headers)
    private fun mobileRequest(manga: SManga) = GET("$MOBILE_URL${manga.url}", headers)

    // for WebView
    override fun mangaDetailsRequest(manga: SManga) = mobileRequest(manga)
    override fun mangaDetailsParse(document: Document) = throw UnsupportedOperationException("Not used.")

    // fetchMangaDetails fetches and parses PC page first, then mobile page
    // fetchChapterList does in the opposite order, to make use of transparent cache
    // in this way, the latter requests will be responded with 304 Not Modified (in most cases)

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> = Single.create<SManga> {
        val document = client.newCall(pcRequest(manga)).execute().asJsoup()
        val result = SManga.create().apply {
            val box = document.selectFirst(Evaluator.Id("intro_l"))
            val details = box.getElementsByTag("span")
            author = details[0].text().removePrefix("作者：")
            status = when (details[1].child(0).ownText()) {
                "连载中" -> SManga.ONGOING
                "已完结" -> SManga.COMPLETED
                else -> SManga.UNKNOWN
            }
            genre = buildList {
                add(details[2].ownText().removePrefix("类别："))
                details[3].ownText().removePrefix("标签：").split(Regex("[ -~]+"))
                    .filterTo(this) { it.isNotEmpty() }
            }.joinToString()
            description = box.selectFirst(Evaluator.Tag("p")).ownText()
            thumbnail_url = box.selectFirst(Evaluator.Tag("img")).attr("src")
        }
        val mobileDocument = client.newCall(mobileRequest(manga)).execute().asJsoup()
        val details = mobileDocument.selectFirst(Evaluator.Class("author"))
            .ownText().trim().split(Regex(""" +"""))
        if (details.size >= 3) {
            result.description = details[2] + '\n' + result.description
        }
        it.onSuccess(result)
    }.toObservable()

    override fun chapterListSelector() = throw UnsupportedOperationException("Not used.")
    override fun chapterFromElement(element: Element) = throw UnsupportedOperationException("Not used.")

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> = Single.create<List<SChapter>> {
        val document = client.newCall(mobileRequest(manga)).execute().asJsoup()
        val list = mutableListOf<SChapter>()

        val tab = document.selectFirst(Evaluator.Class("cartoon-directory")).children()
        if (tab.size >= 2) {
            tab[1].children().mapTo(list) { element ->
                SChapter.create().apply {
                    url = element.attr("href")
                    name = element.text()
                }
            }
            if (tab.size >= 3) {
                val element = tab[2]
                val path = manga.url
                val body = FormBody.Builder().apply {
                    addEncoded("id", element.attr("data-id"))
                    addEncoded("id2", element.attr("data-vid"))
                }.build()
                client.newCall(POST("$MOBILE_URL/bookchapter/", headers, body)).execute()
                    .parseAs<List<ChapterDto>>().mapTo(list) { it.toSChapter(path) }
            }
        }

        if (isNewDateLogic && list.isNotEmpty()) {
            val pcDocument = client.newCall(pcRequest(manga)).execute().asJsoup()
            pcDocument.selectFirst(".cy_zhangjie_top font")?.run {
                list[0].date_upload = dateFormat.parse(ownText())?.time ?: 0
            }
        }
        it.onSuccess(list)
    }.toObservable()

    override fun pageListRequest(chapter: SChapter) = GET("$MOBILE_URL${chapter.url}", headers)

    override fun pageListParse(response: Response): List<Page> {
        val result = Unpacker.unpack(response.body!!.string(), "[", "]")
            .ifEmpty { return emptyList() }
            .replace("\\", "")
            .removeSurrounding("\"").split("\",\"")
        return result.mapIndexed { i, url -> Page(i, imageUrl = url) }
    }

    override fun pageListParse(document: Document) = throw UnsupportedOperationException("Not used.")
    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException("Not used.")

    private inline fun <reified T> Response.parseAs(): T = use {
        json.decodeFromStream(body!!.byteStream())
    }

    override fun getFilterList() = FilterList(listOf(PageFilter()))

    companion object {
        // redirect URL: http://www.6mh9.com/
        private const val DOMAIN = "sixmh7.com"
        private const val PC_URL = "http://www.$DOMAIN"
        private const val MOBILE_URL = "http://m.$DOMAIN"

        private val isNewDateLogic = AppInfo.getVersionCode() >= 81
        private val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    }
}
