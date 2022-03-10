package eu.kanade.tachiyomi.extension.it.walpurgisscan

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class WalpurgisScan : Madara("Walpurgi Scan", "https://walpurgiscan.altervista.org", "it", SimpleDateFormat("dd MMMMM yyyy", Locale("it"))) {
    override val id = 6566957355096372149
}
