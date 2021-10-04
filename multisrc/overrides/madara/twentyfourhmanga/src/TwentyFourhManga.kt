package eu.kanade.tachiyomi.extension.en.twentyfourhmanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class TwentyFourhManga : Madara("24hManga", "https://24hmanga.com", "en", dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US))
