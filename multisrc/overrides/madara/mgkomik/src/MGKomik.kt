package eu.kanade.tachiyomi.extension.id.mgkomik

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import okhttp3.CacheControl
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Locale

class MGKomik : Madara("MG Komik", "https://mgkomik.id", "id", SimpleDateFormat("dd MMM yy", Locale.US)) {

    override val chapterUrlSuffix = ""

    override fun searchMangaNextPageSelector() = "a.page.larger"
    override fun popularMangaSelector() = searchMangaSelector()

    override fun popularMangaRequest(page: Int): Request {
        return GET(
            url = "$baseUrl/${searchPage(page)}?s&post_type=wp-manga&m_orderby=views",
            headers = headers,
            cache = CacheControl.FORCE_NETWORK,
        )
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET(
            url = "$baseUrl/${searchPage(page)}?s&post_type=wp-manga&m_orderby=new-manga",
            headers = headers,
            cache = CacheControl.FORCE_NETWORK,
        )
    }
}
