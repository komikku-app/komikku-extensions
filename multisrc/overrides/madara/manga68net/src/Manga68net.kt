package eu.kanade.tachiyomi.extension.en.manga68net

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class Manga68net : Madara(
    "Manga68.net",
    "https://manga68.net",
    "en",
    dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
)
