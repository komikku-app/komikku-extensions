package eu.kanade.tachiyomi.extension.fr.phenixscans

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import java.text.SimpleDateFormat
import java.util.Locale

class PhenixScans : MangaThemesia("PhenixScans", "https://phenixscans.fr", "fr", dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.FRENCH))
