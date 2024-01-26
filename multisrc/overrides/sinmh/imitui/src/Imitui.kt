package eu.kanade.tachiyomi.extension.zh.imitui

import eu.kanade.tachiyomi.multisrc.sinmh.SinMH
import eu.kanade.tachiyomi.source.model.Page
import org.jsoup.nodes.Document

class Imitui : SinMH("爱米推漫画 (Dead)", "https://www.imitui.com") {
    // This site blocks IP outside China, now only has movies

    override fun chapterListSelector() = ".chapter-body li > a:not([href^=/comic/app/])"

    override fun pageListParse(document: Document): List<Page> =
        document.select("img[onclick]").mapIndexed { index, img ->
            val url = img.attr("data-src").ifEmpty { img.attr("src") }
            Page(index, imageUrl = url)
        }
}
