package eu.kanade.tachiyomi.extension.ar.magusmanga

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import java.text.SimpleDateFormat
import java.util.Locale

class MagusManga : MangaThemesia("Magus Manga", "https://magusmanga.com", "ar", dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale("ar")))
