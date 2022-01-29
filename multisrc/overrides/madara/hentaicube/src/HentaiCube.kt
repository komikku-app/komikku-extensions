package eu.kanade.tachiyomi.extension.vi.hentaicube

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.Page
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class HentaiCube : Madara("Hentai Cube", "https://hentaicube.net", "vi", SimpleDateFormat("dd/MM/yyyy", Locale("vi"))) {
    override fun pageListParse(document: Document): List<Page> {
        val urls = mutableListOf<String>()
        return super.pageListParse(document).filter {
            !urls.contains(it.imageUrl)
            urls.add(it.imageUrl!!)
        }
    }
}
