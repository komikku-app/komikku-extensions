package eu.kanade.tachiyomi.extension.tr.mangasepeti

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class MangaSepeti : Madara(
    "Manga Sepeti",
    "https://www.mangasepeti.xyz",
    "tr",
    SimpleDateFormat("MMMMM d, yyyy", Locale("tr")),
)
