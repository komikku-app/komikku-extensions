package eu.kanade.tachiyomi.extension.it.shinobiscans

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class ShinobiScans : Madara("ShinobiScans", "https://shinobiscans.com", "it", SimpleDateFormat("MMMM d, yyyy", Locale("it")))
