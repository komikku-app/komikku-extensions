package eu.kanade.tachiyomi.extension.en.manganatoinfo

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class Manganatoinfo : Madara(
    "Manganato.info",
    "https://manganato.info",
    "en",
    dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
)
