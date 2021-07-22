package eu.kanade.tachiyomi.extension.en.mangagg

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import okhttp3.Request

class MangaGG : Madara("MangaGG", "https://mangagg.com", "en") {

    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/comic-list/$page?m_orderby=views", headers)
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/comic-list/$page?m_orderby=latest", headers)
}
