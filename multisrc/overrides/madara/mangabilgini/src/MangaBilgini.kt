package eu.kanade.tachiyomi.extension.tr.mangabilgini

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class MangaBilgini : Madara(
    "Manga Bilgini",
    "https://mangabilgini.com",
    "tr",
    dateFormat = SimpleDateFormat("MMM d, yyy", Locale("tr")),
) {
    override val useNewChapterEndpoint = false
}
