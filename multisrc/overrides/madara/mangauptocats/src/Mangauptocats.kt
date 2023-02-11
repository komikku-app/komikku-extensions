package eu.kanade.tachiyomi.extension.th.mangauptocats

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class Mangauptocats : Madara(
    "Mangauptocats",
    "https://manga-uptocats.com",
    "th",
    SimpleDateFormat("d MMMM yyyy", Locale("th")),
)
