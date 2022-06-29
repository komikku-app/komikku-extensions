package eu.kanade.tachiyomi.extension.zh.pufei

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList

internal fun getFilters() = FilterList(
    Filter.Header("排序只对文本搜索和分类筛选有效"),
    SortFilter(),
    Filter.Separator(),
    Filter.Header("以下筛选最多使用一个，使用文本搜索时将会忽略"),
    CategoryFilter(),
    AlphabetFilter(),
)

internal fun parseSearchSort(filters: FilterList): String =
    filters.filterIsInstance<SortFilter>().firstOrNull()?.let { SORT_QUERIES[it.state] } ?: ""

internal fun parseFilters(page: Int, filters: FilterList): String {
    val pageStr = if (page == 1) "" else "_$page"
    var category = 0
    var categorySort = 0
    var alphabet = 0
    for (filter in filters) when (filter) {
        is SortFilter -> categorySort = filter.state
        is CategoryFilter -> category = filter.state
        is AlphabetFilter -> alphabet = filter.state
        else -> {}
    }
    return if (category > 0) {
        "/${CATEGORY_KEYS[category]}/${SORT_KEYS[categorySort]}$pageStr.html"
    } else if (alphabet > 0) {
        "/mh/${ALPHABET[alphabet].lowercase()}/index$pageStr.html"
    } else {
        ""
    }
}

internal class SortFilter : Filter.Select<String>("排序", SORT_NAMES)

private val SORT_NAMES = arrayOf("添加时间", "更新时间", "点击次数")
private val SORT_KEYS = arrayOf("index", "update", "view")
private val SORT_QUERIES = arrayOf("&orderby=newstime", "&orderby=lastdotime", "&orderby=onclick")

internal class CategoryFilter : Filter.Select<String>("分类", CATEGORY_NAMES)

private val CATEGORY_NAMES = arrayOf("全部", "少年热血", "少女爱情", "武侠格斗", "科幻魔幻", "竞技体育", "搞笑喜剧", "耽美人生", "侦探推理", "恐怖灵异")
private val CATEGORY_KEYS = arrayOf("", "shaonianrexue", "shaonvaiqing", "wuxiagedou", "kehuan", "jingjitiyu", "gaoxiaoxiju", "danmeirensheng", "zhentantuili", "kongbulingyi")

internal class AlphabetFilter : Filter.Select<String>("字母", ALPHABET)

private val ALPHABET = arrayOf("全部", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")
