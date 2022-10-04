package eu.kanade.tachiyomi.extension.pt.reaperscans

import eu.kanade.tachiyomi.multisrc.heancms.Genre
import eu.kanade.tachiyomi.multisrc.heancms.HeanCms
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class ReaperScans : HeanCms(
    "Reaper Scans",
    "https://reaperscans.com.br",
    "pt-BR"
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimitHost(apiUrl.toHttpUrl(), 1, 2)
        .build()

    // Site changed from Madara to HeanCms.
    override val versionId = 2

    override fun getGenreList(): List<Genre> = listOf(
        Genre("Artes Marciais", 2),
        Genre("Aventura", 10),
        Genre("Ação", 9),
        Genre("Comédia", 14),
        Genre("Drama", 15),
        Genre("Escolar", 7),
        Genre("Fantasia", 11),
        Genre("Ficção científica", 16),
        Genre("Guerra", 17),
        Genre("Isekai", 18),
        Genre("Jogo", 12),
        Genre("Mangá", 24),
        Genre("Manhua", 23),
        Genre("Manhwa", 22),
        Genre("Mecha", 19),
        Genre("Mistério", 20),
        Genre("Nacional", 8),
        Genre("Realidade Virtual", 21),
        Genre("Retorno", 3),
        Genre("Romance", 5),
        Genre("Segunda vida", 4),
        Genre("Seinen", 1),
        Genre("Shounen", 13),
        Genre("Terror", 6)
    )
}
