package eu.kanade.tachiyomi.extension.zh.imitui

import eu.kanade.tachiyomi.multisrc.sinmh.SinMH
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.util.asJsoup
import org.jsoup.nodes.Document
import rx.Observable
import rx.Single

class Imitui : SinMH("爱米推漫画", "https://www.imitui.com") {

    override fun chapterListSelector() = ".chapter-body li > a:not([href^=/comic/app/])"

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> =
        Single.create<List<Page>> {
            val pcResponse = client.newCall(GET(baseUrl + chapter.url, headers)).execute()
            val pcResult = pageListParse(pcResponse.asJsoup())
            if (pcResult.isNotEmpty()) return@create it.onSuccess(pcResult)
            val mobileResponse = client.newCall(GET(mobileUrl + chapter.url, headers)).execute()
            it.onSuccess(mobilePageListParse(mobileResponse.asJsoup()))
        }.toObservable()

    private fun mobilePageListParse(document: Document): List<Page> {
        val pageCount = document.select("div.image-content > p").text().removePrefix("1/").toInt()
        val prefix = document.location().removeSuffix(".html")
        return (0 until pageCount).map { Page(it, url = "$prefix-${it + 1}.html") }
    }

    // mobile
    override fun imageUrlParse(document: Document): String =
        document.select("div.image-content > img#image").attr("src")
}
