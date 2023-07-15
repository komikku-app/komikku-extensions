package eu.kanade.tachiyomi.extension.id.manhwalandmom

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient

class ManhwaLandMom : MangaThemesia("ManhwaLand.mom", "https://manhwaland.us", "id") {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(4)
        .build()
}
