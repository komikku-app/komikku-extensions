package eu.kanade.tachiyomi.extension.en.infernalvoidscans

import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import eu.kanade.tachiyomi.source.model.Page
import org.jsoup.nodes.Document

class InfernalVoidScans : WPMangaStream("Infernal Void Scans", "https://infernalvoidscans.com", "en") {
    // Site dynamically replaces a placeholder image in the "src" tag with the actual url in "data-src"
    override fun pageListParse(document: Document): List<Page> {
        return super.pageListParse(
            document.apply {
                select(pageSelector).forEach { pageElem ->
                    pageElem.attr("data-src")
                        .takeIf { ! it.isNullOrBlank() }
                        ?.let { pageElem.attr("src", it) }
                }
            }
        )
    }
}
