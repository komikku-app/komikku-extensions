package eu.kanade.tachiyomi.extension.zh.imitui

import eu.kanade.tachiyomi.multisrc.sinmh.SinMH
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.nodes.Document

class Imitui : SinMH("爱米推漫画", "https://www.imitui.com") {
    private val mobileUrl = "https://m.imitui.com"

    override fun chapterListRequest(manga: SManga) = GET(mobileUrl + manga.url, headers)

    override fun pageListRequest(chapter: SChapter) = GET(mobileUrl + chapter.url, headers)

    override fun pageListParse(document: Document): List<Page> {
        val pageCount = document.select("div.image-content > p").text().removePrefix("1/").toInt()
        val prefix = document.location().removeSuffix(".html")
        return (0 until pageCount).map { Page(it, "$prefix-${it + 1}.html") }
    }

    override fun imageUrlParse(document: Document): String =
        document.select("div.image-content > img#image").attr("src")
}
