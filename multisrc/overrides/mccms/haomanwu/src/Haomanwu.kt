package eu.kanade.tachiyomi.extension.zh.haomanwu

import eu.kanade.tachiyomi.multisrc.mccms.MCCMS
import eu.kanade.tachiyomi.source.model.Page
import okhttp3.Response

class Haomanwu : MCCMS("好漫屋", "https://app2.haoman6.com", hasCategoryPage = false) {
    override fun pageListParse(response: Response): List<Page> {
        val pages = super.pageListParse(response)
        if (pages.any { it.imageUrl!!.endsWith("tianjia.jpg") }) {
            throw Exception("该章节有图片尚未添加")
        }
        return pages
    }
}
