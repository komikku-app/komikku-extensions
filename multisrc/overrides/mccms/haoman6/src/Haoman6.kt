package eu.kanade.tachiyomi.extension.zh.haoman6

import eu.kanade.tachiyomi.multisrc.mccms.MCCMSWeb
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga

class Haoman6 : MCCMSWeb("好漫6", "https://www.haoman6.com") {
    override fun SManga.cleanup() = apply {
        description = description?.substringBefore(title)
        title = title.removeSuffix("(最新在线)").removeSuffix("-")
    }

    override fun pageListRequest(chapter: SChapter) =
        GET(baseUrl + chapter.url, headers)

    override val lazyLoadImageAttr = "mob-ec"
}
