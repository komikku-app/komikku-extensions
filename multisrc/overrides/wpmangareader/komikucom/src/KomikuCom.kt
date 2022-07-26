package eu.kanade.tachiyomi.extension.id.komikucom

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import java.text.SimpleDateFormat
import java.util.Locale

class KomikuCom : WPMangaReader("Komiku.com", "https://komiku.com", "id", dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("id")))
