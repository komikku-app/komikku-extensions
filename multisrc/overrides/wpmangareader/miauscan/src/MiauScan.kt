package eu.kanade.tachiyomi.extension.es.miauscan

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import java.text.SimpleDateFormat
import java.util.Locale

class MiauScan : WPMangaReader(
    "Miau Scan",
    "https://miauscan.com",
    "es",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("es"))
)
