package eu.kanade.tachiyomi.extension.zh.manhuadui

import eu.kanade.tachiyomi.multisrc.sinmh.SinMH
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.nodes.Document

class YKMH : SinMH("优酷漫画", "http://www.ykmh.com") {
    override val id = 1637952806167036168
    override val mobileUrl = "http://wap.ykmh.com"

    override val comicItemSelector = "li.list-comic"
    override val comicItemTitleSelector = "h3 > a, p > a"

    // DMZJ style
    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        title = document.selectFirst("h1").text()
        val details = document.selectFirst("ul.comic_deCon_liO").children()
        author = details[0].selectFirst("a").text()
        status = when (details[1].selectFirst("a").text()) {
            "连载中" -> SManga.ONGOING
            "已完结" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
        genre = (details[2].select("a") + details[3].select("a")).joinToString(", ") { it.text() }
        description = document.selectFirst("p.comic_deCon_d").text()
        thumbnail_url = document.selectFirst("div.comic_i_img > img").attr("src")
    }

    override fun List<SChapter>.sortedDescending() = this
}
