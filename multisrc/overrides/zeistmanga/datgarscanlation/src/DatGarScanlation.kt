package eu.kanade.tachiyomi.extension.es.datgarscanlation

import eu.kanade.tachiyomi.multisrc.zeistmanga.ZeistManga
import org.jsoup.nodes.Document

class DatGarScanlation : ZeistManga("DatGarScanlation", "https://datgarscanlation.blogspot.com", "es") {

    private val altChapterFeedRegex = """label\s*=\s*'([^']+)'""".toRegex()
    private val altScriptSelector = "#latest > script"

    override fun getChaptersUrl(doc: Document): String {
        var chapterRegex = chapterFeedRegex
        var script = doc.selectFirst(scriptSelector)

        if (script == null) {
            script = doc.selectFirst(altScriptSelector)!!
            chapterRegex = altChapterFeedRegex
        }

        val feed = chapterRegex
            .find(script.html())
            ?.groupValues?.get(1)
            ?: throw Exception("Failed to find chapter feed")

        val url = apiUrl(feed)
            .addQueryParameter("start-index", "2") // Only get chapters
            .addQueryParameter("max-results", "999999") // Get all chapters
            .build()

        return url.toString()
    }
}
