package eu.kanade.tachiyomi.extension.es.tiempodewebeo

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class TiempoDeWebeo : Madara(
    "Tiempo de webeo",
    "https://tiempodewebeo.com",
    "es",
    SimpleDateFormat("dd/MM/yyyy", Locale("es"))
)
