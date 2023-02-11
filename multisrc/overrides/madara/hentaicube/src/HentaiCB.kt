package eu.kanade.tachiyomi.extension.vi.hentaicube

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Page
import okhttp3.CacheControl
import okhttp3.Request
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class HentaiCB : Madara("Hentai CB", "https://hentaicb.xyz", "vi", SimpleDateFormat("dd/MM/yyyy", Locale("vi"))) {
    override val id: Long = 823638192569572166
    override val useLoadMoreSearch = false
    override fun pageListParse(document: Document): List<Page> {
        return super.pageListParse(document).distinctBy { it.imageUrl }
    }
    override fun popularMangaRequest(page: Int): Request {
        return GET(
            "$baseUrl/manga/page/$page/?m_orderby=views",
            formHeaders,
            CacheControl.FORCE_NETWORK,
        )
    }
    override fun latestUpdatesRequest(page: Int): Request {
        return GET(
            "$baseUrl/manga/page/$page/?m_orderby=latest",
            formHeaders,
            CacheControl.FORCE_NETWORK,
        )
    }
}
