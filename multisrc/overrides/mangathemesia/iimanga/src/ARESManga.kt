package eu.kanade.tachiyomi.extension.ar.iimanga

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import java.text.SimpleDateFormat
import java.util.Locale

class ARESManga : MangaThemesia(
    "ARESManga",
    "https://aresmanga.net",
    "ar",
    mangaUrlDirectory = "/series",
    dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale("ar")),
) {
    // The scanlator changed their name.
    override val id: Long = 230017529540228175
}
