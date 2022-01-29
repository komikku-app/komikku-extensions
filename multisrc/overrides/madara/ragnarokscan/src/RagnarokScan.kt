package eu.kanade.tachiyomi.extension.es.ragnarokscan

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class RagnarokScan : Madara("RagnarokScan", "https://ragnarokscan.com", "es", SimpleDateFormat("MMMMM dd, yyyy", Locale("es")))
