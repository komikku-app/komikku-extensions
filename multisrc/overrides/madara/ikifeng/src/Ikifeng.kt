package eu.kanade.tachiyomi.extension.es.ikifeng

import eu.kanade.tachiyomi.annotations.Nsfw
import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

@Nsfw
class Ikifeng : Madara("Ikifeng", "https://ikifeng.com", "es", SimpleDateFormat("dd/MM/yyyy", Locale("es")))
