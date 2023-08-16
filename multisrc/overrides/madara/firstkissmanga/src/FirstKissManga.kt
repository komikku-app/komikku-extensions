package eu.kanade.tachiyomi.extension.en.firstkissmanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

class FirstKissManga : Madara(
    "1st Kiss",
    "https://1stkissmanga.me",
    "en",
) {
    override val client = super.client.newBuilder()
        .rateLimit(1, 3, TimeUnit.SECONDS)
        .build()

    override fun searchPage(page: Int): String {
        return if (page > 1) {
            "page/$page/"
        } else {
            ""
        }
    }

    override fun imageFromElement(element: Element): String? {
        return when {
            element.hasAttr("data-cfsrc") -> element.attr("abs:data-cfsrc")
            else -> super.imageFromElement(element)
        }?.trim()
    }
}
