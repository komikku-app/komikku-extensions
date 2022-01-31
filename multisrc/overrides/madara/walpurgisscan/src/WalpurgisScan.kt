package eu.kanade.tachiyomi.extension.it.walpurgisscan

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class WalpurgisScan : Madara("Walpurgis Scan", "https://walpurgiscan.it", "it", SimpleDateFormat("dd MMMMM yyyy", Locale("it")))
