package eu.kanade.tachiyomi.extension.pt.blmanhwaclub

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class BlManhwaClub : Madara(
    "BL Manhwa Club",
    "https://blmanhwa.club",
    "pt-BR",
    SimpleDateFormat("dd MMM yyyy", Locale("pt", "BR")),
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()
}
