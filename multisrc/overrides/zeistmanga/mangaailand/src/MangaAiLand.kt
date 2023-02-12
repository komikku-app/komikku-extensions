package eu.kanade.tachiyomi.extension.ar.mangaailand

import eu.kanade.tachiyomi.multisrc.zeistmanga.ZeistManga
import org.jsoup.nodes.Document

class MangaAiLand : ZeistManga("Manga Ai Land", "https://manga-ai-land.blogspot.com", "ar") {

    override val chapterFeedRegex = """([^']+)\?""".toRegex()
    override val scriptSelector = "#myUL > script"
    override val imgSelector = "a[href]"
    override val imgSelectorAttr = "href"

    override fun getChaptersUrl(doc: Document): String {
        val script = doc.selectFirst(scriptSelector)!!.attr("src")
        val feed = chapterFeedRegex
            .find(script)
            ?.groupValues?.get(1)
            ?: throw Exception("Failed to find chapter feed")

        return "$baseUrl" + feed + "?alt=json&start-index=2&max-results=999999"
    }
}
