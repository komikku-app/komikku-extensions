package eu.kanade.tachiyomi.extension.ar.iimanga

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import java.text.SimpleDateFormat
import java.util.Locale

class iiMANGA : WPMangaReader("iiMANGA", "https://iimanga.com", "ar", dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale("ar")))
