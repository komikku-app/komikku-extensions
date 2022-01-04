package eu.kanade.tachiyomi.extension.ar.ozulscans

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import java.text.SimpleDateFormat
import java.util.Locale

class OzulScans : WPMangaReader(
    "Ozul Scans",
    "https://ozulscans.com",
    "ar",
    dateFormat = SimpleDateFormat("MMM d, yyy", Locale("ar"))
)
