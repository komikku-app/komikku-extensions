package eu.kanade.tachiyomi.extension.fr.arcanescan

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import java.text.SimpleDateFormat
import java.util.Locale

class Arcanescan : MangaThemesia(
    "Arcane scan",
    "https://arcanescan.fr",
    "fr",
    dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale.FRANCE),
)
