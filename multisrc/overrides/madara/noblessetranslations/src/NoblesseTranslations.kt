package eu.kanade.tachiyomi.extension.es.noblessetranslations

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class NoblesseTranslations : Madara(
    "Noblesse Translations",
    "https://www.noblessetranslations.com",
    "es",
    dateFormat = SimpleDateFormat("dd/MM/yy", Locale.ROOT),
) {
    override val useNewChapterEndpoint = true
}
