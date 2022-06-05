package eu.kanade.tachiyomi.extension.pt.mangatube

import eu.kanade.tachiyomi.multisrc.mangasar.MangaSar
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MangaTube : MangaSar(
    "MangaTube",
    "https://mangatube.site",
    "pt-BR"
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(::searchIntercept)
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()
}
