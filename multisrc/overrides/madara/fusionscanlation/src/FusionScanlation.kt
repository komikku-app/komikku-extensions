package eu.kanade.tachiyomi.extension.es.fusionscanlation

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class FusionScanlation : Madara("Fusion Scanlation", "https://fusionscanlation.com", "es", SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es"))) {

    override val versionId = 2

    override val seriesTypeSelector = ".post-content_item:contains(Tipo) .summary-content"
    override val altNameSelector = ".post-content_item:contains(Nombre Alternativo) .summary-content"
    override val altName = "Nombre alternativo: "

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .rateLimit(1, 2)
        .build()
}
