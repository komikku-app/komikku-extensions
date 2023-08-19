package eu.kanade.tachiyomi.extension.tr.monomanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class MonoManga : Madara(
    "Mono Manga",
    "https://monomanga.com",
    "tr",
    dateFormat = SimpleDateFormat("d MMM yyyy", Locale("tr")),
) {
    override val useNewChapterEndpoint = false

    override fun searchPage(page: Int): String = if (page == 1) "" else "page/$page/"
}
