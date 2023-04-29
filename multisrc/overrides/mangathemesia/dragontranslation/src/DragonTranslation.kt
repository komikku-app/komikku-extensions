package eu.kanade.tachiyomi.extension.es.dragontranslation

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import java.text.SimpleDateFormat
import java.util.Locale

class DragonTranslation : MangaThemesia(
    "DragonTranslation",
    "https://dragontranslation.com",
    "es",
    mangaUrlDirectory = "/manga",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("en")),
)
