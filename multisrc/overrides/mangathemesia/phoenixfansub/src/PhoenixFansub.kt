package eu.kanade.tachiyomi.extension.es.phoenixfansub

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import java.text.SimpleDateFormat
import java.util.Locale

class PhoenixFansub : MangaThemesia(
    "Phoenix Fansub",
    "https://phoenixfansub.com",
    "es",
    dateFormat = SimpleDateFormat("MMM d, yyyy", Locale("es")),
) {

    override val altNamePrefix: String = "Nombre alternativo: "
}
