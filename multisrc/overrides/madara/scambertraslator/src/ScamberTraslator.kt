package eu.kanade.tachiyomi.extension.es.scambertraslator

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class ScamberTraslator : Madara(
    "ScamberTraslator",
    "https://scambertraslator.com",
    "es",
    dateFormat = SimpleDateFormat("yyy-MM-dd", Locale.ROOT),
) {
    override val useNewChapterEndpoint = false
}
