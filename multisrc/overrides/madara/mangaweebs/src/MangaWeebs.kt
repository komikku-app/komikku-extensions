package eu.kanade.tachiyomi.extension.en.mangaweebs

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class MangaWeebs : Madara("Manga Weebs", "https://mangaweebs.in", "en", dateFormat = SimpleDateFormat("dd MMMM HH:mm", Locale.US))
