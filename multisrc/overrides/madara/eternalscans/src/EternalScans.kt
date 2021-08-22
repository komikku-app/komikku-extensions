package eu.kanade.tachiyomi.extension.en.eternalscans

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class EternalScans : Madara(
    "Eternal Scans",
    "https://eternalscans.com",
    "en",
    dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
)
