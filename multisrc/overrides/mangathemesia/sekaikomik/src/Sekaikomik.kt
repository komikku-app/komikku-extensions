package eu.kanade.tachiyomi.extension.id.sekaikomik

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import java.text.SimpleDateFormat
import java.util.Locale

class Sekaikomik : MangaThemesia("Sekaikomik", "https://www.sekaikomik.pro", "id", dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("id")))
