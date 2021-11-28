package eu.kanade.tachiyomi.extension.en.resetscans

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.SChapter
import org.jsoup.nodes.Element
import java.util.Locale

class ResetScans : Madara("Reset Scans", "https://reset-scans.com", "en", java.text.SimpleDateFormat("dd/MM/yyyy", Locale.US)) {
    override val useNewChapterEndpoint = true

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = super.chapterFromElement(element)

        with(element) {
            chapter.date_upload = select("span.chapter-release-date i").firstOrNull()?.text().let { parseChapterDate(it) }
        }

        return chapter
    }
}
