package eu.kanade.tachiyomi.extension.en.zinmanhwa

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class Zinmanhwa : Madara(
    "Zinmanga.io",
    "https://zinmanhwa.io",     // original URL: https://zinmanhwa.com
    "en",
    dateFormat = SimpleDateFormat("dd/MM/yy", Locale.US),
) {
    override val id = 1971029352586269399
}
