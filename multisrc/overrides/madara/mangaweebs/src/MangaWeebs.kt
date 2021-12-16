package eu.kanade.tachiyomi.extension.en.mangaweebs

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class MangaWeebs : Madara("Manga Weebs", "https://mangaweebs.in", "en", dateFormat = SimpleDateFormat("dd MMMM HH:mm", Locale.US)) {
    override val pageListParseSelector = "div.page-break > div.loading, li.blocks-gallery-item, .reading-content .text-left:not(:has(.blocks-gallery-item)) :has(>img)"
}
