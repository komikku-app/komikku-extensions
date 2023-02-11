package eu.kanade.tachiyomi.extension.en.adultwebtoon

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import okhttp3.CacheControl
import okhttp3.Request

class AdultWebtoon : Madara("Adult Webtoon", "https://adultwebtoon.com", "en") {

    override val useLoadMoreSearch = false
    override fun popularMangaRequest(page: Int): Request {
        val pageSuffix = if (page != 1) "page/$page/" else ""
        return GET(
            "$baseUrl/manga/$pageSuffix?m_orderby=trending",
            formHeaders,
            CacheControl.FORCE_NETWORK,
        )
    }
    override fun latestUpdatesRequest(page: Int): Request {
        val pageSuffix = if (page != 1) "page/$page/" else ""
        return GET(
            "$baseUrl/manga/$pageSuffix?m_orderby=latest",
            formHeaders,
            CacheControl.FORCE_NETWORK,
        )
    }
}
