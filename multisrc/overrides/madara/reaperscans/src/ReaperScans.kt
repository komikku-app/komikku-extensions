package eu.kanade.tachiyomi.extension.en.reaperscans

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.SChapter
import org.jsoup.nodes.Element

class ReaperScans : Madara("Reaper Scans", "https://reaperscans.com", "en") {
    override val versionId = 2

    override fun popularMangaSelector() = "div.page-item-detail.manga"

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()

        with(element) {
            select(chapterUrlSelector).first()?.let { urlElement ->
                chapter.url = urlElement.attr("abs:href").let {
                    it.substringBefore("?style=paged") + if (!it.endsWith(chapterUrlSuffix)) chapterUrlSuffix else ""
                }
                chapter.name = urlElement.ownText()
            }
            // Dates can be part of a "new" graphic or plain text
            // Added "title" alternative
            chapter.date_upload = select("img").firstOrNull()?.attr("alt")?.let { parseRelativeDate(it) }
                ?: select("span a").firstOrNull()?.attr("title")?.let { parseRelativeDate(it) }
                ?: parseChapterDate(select("span.chapter-release-date i").firstOrNull()?.text())
        }

        return chapter
    }
}
