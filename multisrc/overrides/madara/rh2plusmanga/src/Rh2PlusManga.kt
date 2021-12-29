package eu.kanade.tachiyomi.extension.th.rh2plusmanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.Page
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class Rh2PlusManga : Madara("Rh2PlusManga", "https://www.rh2plusmanga.com", "th", SimpleDateFormat("d MMMM yyyy", Locale("th"))) {
    override val useNewChapterEndpoint = true

    override val pageListParseSelector = "div.reading-content p code img"

    override fun pageListParse(document: Document): List<Page> {
        countViews(document)

        return document.select(pageListParseSelector).mapIndexed { index, element ->
            Page(
                index,
                document.location(),
                element.let {
                    it.absUrl(if (it.hasAttr("data-src")) "data-src" else "src")
                }
            )
        }
    }
}
