package eu.kanade.tachiyomi.extension.es.taurusfansub

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class TaurusFansub : Madara(
    "Taurus Fansub",
    "https://taurusfansub.com",
    "es",
    dateFormat = SimpleDateFormat("dd/MM/yyy", Locale.ROOT),
) {
    override val useNewChapterEndpoint = true
}
