package eu.kanade.tachiyomi.extension.zh.bilibilimanga

import eu.kanade.tachiyomi.lib.ratelimit.SpecificHostRateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.bilibili.Bilibili
import eu.kanade.tachiyomi.multisrc.bilibili.BilibiliTag
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class BilibiliManga : Bilibili(
    "哔哩哔哩漫画",
    "https://manga.bilibili.com",
    "zh-Hans"
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

    override fun getAllStatus(): Array<String> = arrayOf("全部", "连载", "完结")

    override fun getAllSortOptions(): Array<String> = arrayOf("人气推荐", "更新时间", "追漫人数", "上架时间")

    override fun getAllPrices(): Array<String> = arrayOf("全部", "免费", "付费", "等就免费")

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
