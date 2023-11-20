package eu.kanade.tachiyomi.extension.id.mikoroku

import eu.kanade.tachiyomi.multisrc.zeistmanga.ZeistManga
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response

class MikoRoku : ZeistManga("MikoRoku", "https://www.mikoroku.web.id", "id") {

    override val popularMangaSelector = "div.PopularPosts article"
    override val popularMangaSelectorTitle = ".post-title a"
    override val popularMangaSelectorUrl = ".post-title a"

    override val pageListSelector = "article#reader div.separator a"

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val document = response.asJsoup()
        with(document.selectFirst("div.section#main div.widget header")!!) {
            thumbnail_url = selectFirst("img")!!.attr("abs:src")
            genre = select("aside dl:has(dt:contains(Genre)) dd a")
                .joinToString { it.text() }
            status = parseStatus(selectFirst("span[data-status]")!!.text())
        }
        description = document.select("div.section#main div.widget div.grid #synopsis").text()
    }
}
