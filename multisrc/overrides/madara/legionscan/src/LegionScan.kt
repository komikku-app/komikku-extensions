package eu.kanade.tachiyomi.extension.es.legionscan

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class LegionScan : Madara("Legion Scan", "https://legionscans.com", "es", dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("es")))
