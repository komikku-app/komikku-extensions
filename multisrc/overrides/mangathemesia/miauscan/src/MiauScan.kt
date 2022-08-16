package eu.kanade.tachiyomi.extension.es.miauscan

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import java.text.SimpleDateFormat
import java.util.Locale

class MiauScan : MangaThemesia(
    "Miau Scan",
    "https://miauscan.com",
    "es",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("es"))
)
