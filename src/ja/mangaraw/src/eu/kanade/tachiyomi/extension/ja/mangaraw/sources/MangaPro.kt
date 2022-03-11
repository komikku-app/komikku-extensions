package eu.kanade.tachiyomi.extension.ja.mangaraw.sources

import eu.kanade.tachiyomi.extension.ja.mangaraw.MangaRaw
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.nodes.Document

class MangaPro : MangaRaw("MangaPro", "https://mangapro.top") {
    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        // Extract the author, take out the colon and quotes
        author = document.select("#main > article > div > div > div > div > p").html()
            .substringAfter("</strong>").substringBefore("<br>").drop(1)
        genre = document.select("#main > article > div > div > div > div > p > a")
            .joinToString(separator = ", ", transform = { it.text() })
        description = document.select("#main > article > div > div > div > div > p").html()
            .substringAfterLast("<br>")
        thumbnail_url = document.select(".wp-block-image img").attr("abs:src")
    }
}
