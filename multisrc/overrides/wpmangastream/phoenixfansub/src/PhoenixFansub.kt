package eu.kanade.tachiyomi.extension.es.phoenixfansub

import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class PhoenixFansub : WPMangaStream(
    "Phoenix Fansub",
    "https://phoenixfansub.com",
    "es",
    SimpleDateFormat("MMM d, yyyy", Locale("es"))
) {
    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        thumbnail_url = element.select("div.limit img").imgAttr()
        element.select("div.bsx > a").first().let {
            url = "/manga/${it.attr("href")}"
            title = it.attr("title")
        }
    }
    override val altName: String = "Nombre alternativo: "
}
