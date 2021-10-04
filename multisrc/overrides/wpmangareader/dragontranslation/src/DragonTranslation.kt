package eu.kanade.tachiyomi.extension.es.dragontranslation

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import java.text.SimpleDateFormat
import java.util.Locale

class DragonTranslation : WPMangaReader(
    "DragonTranslation",
    "https://dragontranslation.com",
    "es",
    dateFormat = SimpleDateFormat("MMM d, yyyy", Locale("es"))
)
