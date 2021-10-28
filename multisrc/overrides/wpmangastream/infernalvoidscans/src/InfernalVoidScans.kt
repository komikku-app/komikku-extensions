package eu.kanade.tachiyomi.extension.en.infernalvoidscans

import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import org.jsoup.nodes.Document
import eu.kanade.tachiyomi.source.model.Page

class InfernalVoidScans : WPMangaStream("Infernal Void Scans", "https://infernalvoidscans.com", "en") {
    // Site dynamically replaces a placeholder image in the "src" tag with the actual url in "data-src"
    override fun pageListParse(document: Document): List<Page> {
        return super.pageListParse(
            document.apply {
                select(pageSelector).forEach {
                    it.attr("src", it.attr("data-src"))
                }
            }
        )
    }
}
