package eu.kanade.tachiyomi.extension.id.masterkomik

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient

class MasterKomik : MangaThemesia("MasterKomik", "https://masterkomik.com", "id") {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(4)
        .build()

    override val projectPageString = "/project-list"

    override val hasProjectPage = true
}
