package eu.kanade.tachiyomi.extension.es.samuraiscan

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class SamuraiScan : Madara("SamuraiScan", "https://samuraiscan.org", "es", SimpleDateFormat("MMMM d, yyyy", Locale("es")))
