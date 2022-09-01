package eu.kanade.tachiyomi.extension.fr.flamescansfr

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import java.text.SimpleDateFormat
import java.util.Locale

class FlameScansFR : MangaThemesia("FlameScans.fr", "https://flamescans.fr", "fr", dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("fr")))
