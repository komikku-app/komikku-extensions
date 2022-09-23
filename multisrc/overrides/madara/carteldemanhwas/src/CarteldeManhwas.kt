package eu.kanade.tachiyomi.extension.es.carteldemanhwas

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class CarteldeManhwas : Madara("Cartel de Manhwas", "https://carteldemws.com", "es", dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("es")))
