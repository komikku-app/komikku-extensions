package eu.kanade.tachiyomi.extension.en.toongod

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Page
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Locale

class ToonGod : Madara("ToonGod", "https://www.toongod.com", "en", SimpleDateFormat("dd MMM yyyy", Locale.US)) {
    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/webtoons/page/$page/?m_orderby=views", headers)
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/webtoons/page/$page/?m_orderby=latest", headers)
    override val mangaSubString = "webtoons"
    override fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headers)
    }
}
