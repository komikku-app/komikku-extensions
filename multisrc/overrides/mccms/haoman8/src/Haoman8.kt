package eu.kanade.tachiyomi.extension.zh.haoman8

import eu.kanade.tachiyomi.multisrc.mccms.MCCMS
import eu.kanade.tachiyomi.network.GET

class Haoman8 : MCCMS("好漫8", "https://caiji.haoman8.com") {

    // Search: 此站点nginx配置有问题，只能用以下格式搜索第一页

    override fun textSearchRequest(page: Int, query: String) =
        GET("$baseUrl/index.php/search?key=$query", headers)

    override fun searchMangaNextPageSelector(): String? = null
}
