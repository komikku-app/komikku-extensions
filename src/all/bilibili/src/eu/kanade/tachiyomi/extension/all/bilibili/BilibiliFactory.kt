package eu.kanade.tachiyomi.extension.all.bilibili

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

class BilibiliFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        BilibiliComicsEn(),
        BilibiliComicsCn(),
        BilibiliComicsId(),
        BilibiliManga()
    )
}

class BilibiliComicsEn : BilibiliComics("en") {

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

class BilibiliComicsCn : BilibiliComics("zh-Hans") {

    override fun getAllSortOptions(): Array<String> = arrayOf("为你推荐", "人气推荐", "更新时间")

    override fun getAllStatus(): Array<String> = arrayOf("全部", "连载中", "已完结")

    override fun getAllPrices(): Array<String> = arrayOf("全部", "免费", "付费")

    override fun getAllGenres(): Array<BilibiliTag> = arrayOf(
        BilibiliTag("全部", -1),
        BilibiliTag("校园", 18),
        BilibiliTag("都市", 9),
        BilibiliTag("耽美", 3),
        BilibiliTag("少女", 20),
        BilibiliTag("恋爱", 13),
        BilibiliTag("奇幻", 11),
        BilibiliTag("热血", 19),
        BilibiliTag("冒险", 22),
        BilibiliTag("古风", 12),
        BilibiliTag("百合", 16),
        BilibiliTag("玄幻", 30),
        BilibiliTag("悬疑", 41),
        BilibiliTag("科幻", 8)
    )
}

class BilibiliComicsId : BilibiliComics("id") {

    override fun getAllSortOptions(): Array<String> = arrayOf("Kamu Mungkin Suka", "Populer", "Terbaru")

    override fun getAllStatus(): Array<String> = arrayOf("Semua", "Berlangsung", "Tamat")

    override fun getAllPrices(): Array<String> = arrayOf("Semua", "Bebas", "Dibayar")

    override fun getAllGenres(): Array<BilibiliTag> = arrayOf(
        BilibiliTag("Semua", -1),
        BilibiliTag("Aksi", 19),
        BilibiliTag("Fantasi Timur", 30),
        BilibiliTag("Fantasi", 11),
        BilibiliTag("Historis", 12),
        BilibiliTag("Horror", 23),
        BilibiliTag("Kampus", 18),
        BilibiliTag("Komedi", 14),
        BilibiliTag("Menegangkan", 41),
        BilibiliTag("Remaja", 20),
        BilibiliTag("Romantis", 13)
    )
}

class BilibiliManga : Bilibili(
    "哔哩哔哩漫画",
    "https://manga.bilibili.com",
    "zh-Hans"
) {

    override val id: Long = 3561131545129718586

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
