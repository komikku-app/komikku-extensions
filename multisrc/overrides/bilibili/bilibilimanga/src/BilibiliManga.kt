package eu.kanade.tachiyomi.extension.zh.bilibilimanga

import eu.kanade.tachiyomi.lib.ratelimit.SpecificHostRateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.bilibili.Bilibili
import eu.kanade.tachiyomi.multisrc.bilibili.BilibiliIntl
import eu.kanade.tachiyomi.multisrc.bilibili.BilibiliTag
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class BilibiliManga : Bilibili(
    "哔哩哔哩漫画",
    "https://manga.bilibili.com",
    BilibiliIntl.SIMPLIFIED_CHINESE
) {

    override val id: Long = 3561131545129718586

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(::expiredTokenIntercept)
        .addInterceptor(SpecificHostRateLimitInterceptor(baseUrl.toHttpUrl(), 1))
        .addInterceptor(SpecificHostRateLimitInterceptor(CDN_URL.toHttpUrl(), 2))
        .addInterceptor(SpecificHostRateLimitInterceptor(COVER_CDN_URL.toHttpUrl(), 2))
        .build()

    override val defaultPopularSort: Int = 0

    override val defaultLatestSort: Int = 1

    override fun getAllSortOptions(): Array<String> =
        arrayOf(intl.sortPopular, intl.sortUpdated, intl.sortFollowers, intl.sortAdded)

    override fun getAllPrices(): Array<String> =
        arrayOf(intl.priceAll, intl.priceFree, intl.pricePaid, intl.priceWaitForFree)

    override fun getAllGenres(): Array<BilibiliTag> = arrayOf(
        BilibiliTag("全部", -1),
        BilibiliTag("竞技", 1034),
        BilibiliTag("冒险", 1013),
        BilibiliTag("热血", 999),
        BilibiliTag("搞笑", 994),
        BilibiliTag("恋爱", 995),
        BilibiliTag("少女", 1026),
        BilibiliTag("日常", 1020),
        BilibiliTag("校园", 1001),
        BilibiliTag("治愈", 1007),
        BilibiliTag("古风", 997),
        BilibiliTag("玄幻", 1016),
        BilibiliTag("奇幻", 998),
        BilibiliTag("惊奇", 996),
        BilibiliTag("悬疑", 1023),
        BilibiliTag("都市", 1002),
        BilibiliTag("剧情", 1030),
        BilibiliTag("总裁", 1004),
        BilibiliTag("科幻", 1015),
        BilibiliTag("正能量", 1028),
    )

    override fun getAllAreas(): Array<BilibiliTag> = arrayOf(
        BilibiliTag("全部", -1),
        BilibiliTag("大陆", 1),
        BilibiliTag("日本", 2),
        BilibiliTag("韩国", 6),
        BilibiliTag("其他", 5),
    )
}
