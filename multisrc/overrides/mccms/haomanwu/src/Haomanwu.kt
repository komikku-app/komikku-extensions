package eu.kanade.tachiyomi.extension.zh.haomanwu

import eu.kanade.tachiyomi.multisrc.mccms.MCCMS
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.nodes.Element

class Haomanwu : MCCMS("好漫屋", "https://app2.haomanwu.com") {

    // Search

    override fun searchMangaNextPageSelector() = "li:nth-child(30) > a"  // 有30项则可能有下一页
    override fun searchMangaSelector() = "li > a"
    override fun searchMangaFromElement(element: Element) = SManga.create().apply {
        title = element.text()
        setUrlWithoutDomain(element.attr("abs:href"))
    }
}
