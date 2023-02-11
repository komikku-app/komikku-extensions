package eu.kanade.tachiyomi.extension.en.manhuazone

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class ManhuaZone : Madara(
    "ManhuaZone",
    "https://manhuazone.com",
    "en",
    dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US),
)
