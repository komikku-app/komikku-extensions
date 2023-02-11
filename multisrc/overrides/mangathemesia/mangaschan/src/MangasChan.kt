package eu.kanade.tachiyomi.extension.pt.mangaschan

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class MangasChan : MangaThemesia(
    "Mangás Chan",
    "https://mangaschan.com",
    "pt-BR",
    dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale("pt", "BR")),
) {

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()

    override val altNamePrefix = "Nomes alternativos: "

    override val seriesArtistSelector = ".infotable tr:contains(Artista) td:last-child"
    override val seriesAuthorSelector = ".infotable tr:contains(Autor) td:last-child"
    override val seriesTypeSelector = ".infotable tr:contains(Tipo) td:last-child"
}
