package eu.kanade.tachiyomi.multisrc.comicgamma

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.text.DateFormat.getDateTimeInstance
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

open class ComicGamma(
    override val name: String,
    override val baseUrl: String,
    override val lang: String = "ja",
) : ParsedHttpSource() {
    override val supportsLatest = true

    override val client = network.client.newBuilder().addInterceptor(PtImgInterceptor()).build()

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/manga/", headers)
    override fun popularMangaNextPageSelector(): String? = null
    override fun popularMangaSelector() = ".work_list li"
    override fun popularMangaFromElement(element: Element) = SManga.create().apply {
        setUrlWithoutDomain(element.select("a").attr("abs:href"))
        val image = element.select("img")
        title = image.attr("alt").removePrefix("『").removeSuffix("』作品ページへ")
        thumbnail_url = image.attr("abs:src")
    }

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/", headers)
    override fun latestUpdatesNextPageSelector(): String? = null
    override fun latestUpdatesSelector() = ".whatsnew li:contains(読む)"

    override fun latestUpdatesFromElement(element: Element) = SManga.create().apply {
        val url = element.select(".show_detail").attr("abs:href")
        setUrlWithoutDomain(url)
        title = element.select("h3").textNodes()[0].text()
        thumbnail_url = url + "main.jpg"
    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        fetchPopularManga(page).map { p -> MangasPage(p.mangas.filter { it.title.contains(query) }, false) }

    override fun searchMangaNextPageSelector() = throw UnsupportedOperationException("Not used.")
    override fun searchMangaSelector() = throw UnsupportedOperationException("Not used.")
    override fun searchMangaFromElement(element: Element) = throw UnsupportedOperationException("Not used.")
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        throw UnsupportedOperationException("Not used.")

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        title = document.select("h1").text()
        author = document.select(".author").text()
        description = document.select(".work_sammary").text()
        val nextDate = document.select(".episode_caption:contains(【次回更新】)")
        if (nextDate.isNotEmpty()) {
            val dateStr = nextDate.textNodes()
                .filter { it.text().contains("【次回更新】") }[0]
                .text().trim().removePrefix("【次回更新】")
            val localDate = JST_FORMAT_DESC.parseJST(dateStr)?.let { LOCAL_FORMAT_DESC.format(it) }
            if (localDate != null) description = "【Next/Repeat: $localDate】\n$description"
        }
    }

    override fun chapterListSelector() =
        ".box_episode > .box_episode_L:contains(読む), .box_episode > .box_episode_M:contains(読む)" // filter out purchase links

    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        val url = element.select("a[id^=read]").attr("abs:href")
        setUrlWithoutDomain(url)
        val chapterNumber = url.removeSuffix("/").substringAfterLast('/').replace('_', '.')
        val title = element.select(".episode_title").textNodes().filterNot {
            it.text().contains("集中連載") || it.text().contains("配信中!!")
        }.joinToString("／") { it.text() }
        name = "$chapterNumber $title"
        element.select(".episode_caption").textNodes().forEach {
            if (it.text().contains("【公開日】")) {
                val date = it.text().trim().removePrefix("【公開日】")
                date_upload = JST_FORMAT_LIST.parseJST(date)!!.time
            } else if (it.text().contains("【次回更新】")) {
                val date = it.text().trim().removePrefix("【次回更新】")
                val localDate = JST_FORMAT_LIST.parseJST(date)?.let { LOCAL_FORMAT_LIST.format(it) }
                if (localDate != null) scanlator = "~$localDate"
            }
        }
        if (date_upload <= 0L) date_upload = -1L // hide unknown ones
    }

    override fun pageListParse(document: Document) =
        document.select("#content > div[data-ptimg]").mapIndexed { i, e ->
            Page(i, imageUrl = e.attr("abs:data-ptimg"))
        }

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException("Not used.")

    // for thread-safety (of subclasses)
    private val JST_FORMAT_DESC = getJSTFormat()
    private val JST_FORMAT_LIST = getJSTFormat()
    private val LOCAL_FORMAT_DESC = getDateTimeInstance()
    private val LOCAL_FORMAT_LIST = getDateTimeInstance()

    companion object {
        private fun SimpleDateFormat.parseJST(date: String) = parse(date)?.apply {
            time += 12 * 3600 * 1000 // updates at 12 noon
        }

        private fun getJSTFormat() =
            SimpleDateFormat("yyyy年M月dd日(E)", Locale.JAPANESE).apply {
                timeZone = TimeZone.getTimeZone("GMT+09:00")
            }
    }
}
