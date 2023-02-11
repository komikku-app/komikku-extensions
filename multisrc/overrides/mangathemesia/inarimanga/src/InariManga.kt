package eu.kanade.tachiyomi.extension.es.inarimanga

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import java.text.SimpleDateFormat
import java.util.Locale

class InariManga : MangaThemesia(
    "InariManga",
    "https://inarimanga.com",
    "es",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("es")),
)
