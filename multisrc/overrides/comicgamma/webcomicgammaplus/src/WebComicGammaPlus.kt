package eu.kanade.tachiyomi.extension.ja.webcomicgammaplus

import eu.kanade.tachiyomi.multisrc.comicgamma.ComicGamma
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.DateFormat

class WebComicGammaPlus : ComicGamma("Web Comic Gamma Plus", "https://gammaplus.takeshobo.co.jp") {
    override val supportsLatest = true

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

    companion object {
        private const val datePattern = "yyyy年M月dd日(E)"
        private val JST_FORMAT_DESC by lazy { getJSTFormat(datePattern) }
        private val JST_FORMAT_LIST by lazy { getJSTFormat(datePattern) }
        private val LOCAL_FORMAT_DESC by lazy { DateFormat.getDateTimeInstance() }
        private val LOCAL_FORMAT_LIST by lazy { DateFormat.getDateTimeInstance() }
    }
}
