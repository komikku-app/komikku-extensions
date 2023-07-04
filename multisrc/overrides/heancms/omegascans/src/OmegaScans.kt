package eu.kanade.tachiyomi.extension.en.omegascans

import eu.kanade.tachiyomi.multisrc.heancms.HeanCms
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class OmegaScans : HeanCms("Omega Scans", "https://omegascans.org", "en") {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimitHost(apiUrl.toHttpUrl(), 1)
        .build()

    // Site changed from MangaThemesia to HeanCms.
    override val versionId = 2

    override val coverPath = ""
}
