package eu.kanade.tachiyomi.extension.tr.faestorm

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class FaeStorm : Madara(
    "FaeStorm",
    "https://faestormmanga.com",
    "tr",
    SimpleDateFormat("d MMM yyy", Locale("tr")),
)
