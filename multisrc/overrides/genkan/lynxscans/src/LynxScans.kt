package eu.kanade.tachiyomi.extension.en.lynxscans

import eu.kanade.tachiyomi.multisrc.genkan.Genkan
import eu.kanade.tachiyomi.source.model.Page
import org.jsoup.nodes.Document

class LynxScans : Genkan("LynxScans", "https://lynxscans.com", "en", "/web/comics") {
    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()

        val allImages = document.select("div#pages-container + script").first()!!.data()
            .substringAfter("[").substringBefore("];")
            .replace(Regex("""["\\]"""), "")
            .split(",/")

        for (i in allImages.indices) {
            pages.add(Page(i, "", if (i == 0) allImages[i] else "/" + allImages[i]))
        }

        return pages
    }
}
