package eu.kanade.tachiyomi.extension.en.realmscans

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import eu.kanade.tachiyomi.source.model.Page
import org.jsoup.nodes.Document

class RealmScans : WPMangaReader("Realm Scans", "https://realmscans.com", "en", "/series") {
    override fun pageListParse(document: Document): List<Page> {
        countViews(document)

        return document.select(pageSelector).mapIndexed { i, img ->
            val url = if (img.attr("data-wpfc-original-src").isEmpty())
                img.attr("abs:src")
            else
                img.attr("data-wpfc-original-src")

            Page(i, "", url)
        }
    }
}
