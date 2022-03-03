package eu.kanade.tachiyomi.extension.fr.arcanescan

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import java.text.SimpleDateFormat
import java.util.Locale

class Arcanescan : WPMangaReader(
    "Arcane scan",
    "https://arcanescan.fr",
    "fr",
    dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale.FRANCE)
)
