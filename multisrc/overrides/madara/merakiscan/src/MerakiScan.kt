package eu.kanade.tachiyomi.extension.es.merakiscan

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class MerakiScan : Madara("Meraki Scan", "https://meraki801.com", "es", SimpleDateFormat("dd 'de' MMMMM 'de' yyyy", Locale("es")))
