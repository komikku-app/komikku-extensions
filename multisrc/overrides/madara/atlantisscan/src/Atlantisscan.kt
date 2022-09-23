package eu.kanade.tachiyomi.extension.es.atlantisscan

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class Atlantisscan : Madara("Atlantis scan", "https://atlantisscan.com", "es", dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es")))
