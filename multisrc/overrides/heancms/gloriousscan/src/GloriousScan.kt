package eu.kanade.tachiyomi.extension.pt.gloriousscan

import eu.kanade.tachiyomi.multisrc.heancms.HeanCms
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.TimeZone

class GloriousScan : HeanCms(
    "Glorious Scan",
    "https://gloriousscan.com",
    "pt-BR",
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimitHost(apiUrl.toHttpUrl(), 1, 2)
        .build()

    // Site changed from Madara to HeanCms.
    override val versionId = 2

    override val coverPath = ""

    override val dateFormat: SimpleDateFormat = super.dateFormat.apply {
        timeZone = TimeZone.getTimeZone("GMT+02:00")
    }

    override val fetchAllTitles = false
}
