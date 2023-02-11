package eu.kanade.tachiyomi.extension.pt.modescanlator

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class ModeScanlator : MangaThemesia(
    "Mode Scanlator",
    "https://modescanlator.com",
    "pt-BR",
    mangaUrlDirectory = "/projetos",
    dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale("pt", "BR")),
) {

    // Site changed from Madara to WpMangaReader.
    override val versionId: Int = 2

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()

    override val altNamePrefix = "Nome alternativo: "
}
