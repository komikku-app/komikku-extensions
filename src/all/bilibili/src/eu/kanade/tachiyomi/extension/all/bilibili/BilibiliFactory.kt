package eu.kanade.tachiyomi.extension.all.bilibili

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

class BilibiliFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        BilibiliComics(),
        BilibiliManhua()
    )
}

class BilibiliComics : Bilibili("BILIBILI COMICS", "https://www.bilibilicomics.com", "en") {

    override fun getAllGenres(): Array<BilibiliTag> = arrayOf(
        BilibiliTag("All", -1),
        BilibiliTag("Action", 19),
        BilibiliTag("Adventure", 22),
        BilibiliTag("BL", 3),
        BilibiliTag("Comedy", 14),
        BilibiliTag("Eastern", 30),
        BilibiliTag("Fantasy", 11),
        BilibiliTag("GL", 16),
        BilibiliTag("Harem", 15),
        BilibiliTag("Historical", 12),
        BilibiliTag("Horror", 23),
        BilibiliTag("Mistery", 17),
        BilibiliTag("Romance", 13),
        BilibiliTag("Slice of Life", 21),
        BilibiliTag("Suspense", 41),
        BilibiliTag("Teen", 20)
    )
}

class BilibiliManhua : Bilibili("哔哩哔哩漫画", "https://manga.bilibili.com", "zh") {

    override val statusLabel: String = "进度"

    override val sortLabel: String = "排序"

    override val genreLabel: String = "题材"

    override val areaLabel: String = "地区"

    override val priceLabel: String = "收费"

    override val episodePrefix: String = ""

    override val defaultPopularSort: Int = 0

    override val defaultLatestSort: Int = 1

    // Machine translated, needs to be revisited.
    override val hasPaidChaptersWarning: String = "本系列已付费章节从章节列表中过滤掉。 " +
        "暂时使用哔哩哔哩网站或官方应用程序阅读它们。"

    // Machine translated, needs to be revisited.
    override val resolutionPrefTitle: String = "章节图像分辨率"

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
