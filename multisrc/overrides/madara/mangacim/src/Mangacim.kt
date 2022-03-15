package eu.kanade.tachiyomi.extension.tr.mangacim

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class Mangacim : Madara(
    "Mangacim",
    "https://www.mangacim.com",
    "tr",
    SimpleDateFormat("MMM d, yyy", Locale("tr"))
)
