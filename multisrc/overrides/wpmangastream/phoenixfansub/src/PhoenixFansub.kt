package eu.kanade.tachiyomi.extension.es.phoenixfansub

import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import java.text.SimpleDateFormat
import java.util.Locale

class PhoenixFansub : WPMangaStream(
    "Phoenix Fansub",
    "https://phoenixfansub.com",
    "es",
    SimpleDateFormat("MMM d, yyyy", Locale("es"))
) {

    override val altName: String = "Nombre alternativo: "
}
