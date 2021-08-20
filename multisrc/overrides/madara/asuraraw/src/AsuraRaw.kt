package eu.kanade.tachiyomi.extension.en.asuraraw

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class AsuraRaw : Madara(
    "Asura Raw",
    "https://asuraraw.com",
    "en",
    dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
) {
    override val useNewChapterEndpoint: Boolean = true
}
