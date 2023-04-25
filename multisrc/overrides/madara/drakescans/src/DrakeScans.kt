package eu.kanade.tachiyomi.extension.en.drakescans

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class DrakeScans : Madara(
    "Drake Scans",
    "https://drakescans.com",
    "en",
    SimpleDateFormat("MM/dd/yyyy", Locale.US),
) {

    override val mangaDetailsSelectorTag = ""

    override val mangaSubString = "series"
}
