package eu.kanade.tachiyomi.extension.id.shinigami

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Element

class Shinigami : Madara("Shinigami", "https://shinigami.id", "id") {
    // moved from Reaper Scans (id) to Shinigami (id)
    override val id = 3411809758861089969

    // Tags are useless as they are just SEO keywords.
    override val mangaDetailsSelectorTag = ""

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        val urlElement = element.selectFirst(chapterUrlSelector)!!

        name = urlElement.selectFirst("p.chapter-manhwa-title")?.text()
            ?: urlElement.ownText()
        date_upload = urlElement.selectFirst("span.chapter-release-date > i")?.text()
            .let { parseChapterDate(it) }

        val fixedUrl = urlElement.attr("abs:href").toHttpUrl().newBuilder()
            .removeAllQueryParameters("style")
            .addQueryParameter("style", "list")
            .toString()

        setUrlWithoutDomain(fixedUrl)
    }
}
