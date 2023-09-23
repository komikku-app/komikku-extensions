package eu.kanade.tachiyomi.extension.es.aiyumanga

import eu.kanade.tachiyomi.multisrc.zeistmanga.ZeistManga
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response

class AiYuManga : ZeistManga("AiYuManga", "https://www.aiyumanhua.com", "es") {

    // Site moved from Madara to ZeistManga
    override val versionId = 2

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        val profileManga = document.selectFirst("header > div.max-w-screen-xl")!!
        return SManga.create().apply {
            thumbnail_url = profileManga.selectFirst("img")!!.attr("abs:src")
            description = document.select("div#synopsis > p").text()
        }
    }

    override val pageListSelector = "article.chapter div.separator"
}
