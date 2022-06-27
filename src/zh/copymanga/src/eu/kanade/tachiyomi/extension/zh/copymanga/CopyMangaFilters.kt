package eu.kanade.tachiyomi.extension.zh.copymanga

import eu.kanade.tachiyomi.source.model.Filter
import okhttp3.HttpUrl

class Param(val name: String, val value: String)

open class CopyMangaFilter(name: String, private val key: String, private val params: Array<Param>) :
    Filter.Select<String>(name, params.map { it.name }.toTypedArray()) {
    fun addQuery(builder: HttpUrl.Builder) {
        val param = params[state].value
        if (param.isNotEmpty())
            builder.addQueryParameter(key, param)
    }
}

class SearchFilter : CopyMangaFilter("文本搜索范围", "q_type", SEARCH_FILTER_VALUES)

private val SEARCH_FILTER_VALUES = arrayOf(
    Param("全部", ""),
    Param("名称", "name"),
    Param("作者", "author"),
    Param("汉化组", "local"),
)

class GenreFilter(genres: Array<Param>) : CopyMangaFilter("题材", "theme", genres)

class RegionFilter : CopyMangaFilter("地区", "region", REGION_VALUES)

private val REGION_VALUES = arrayOf(
    Param("全部", ""),
    Param("日本", "0"),
    Param("韩国", "1"),
    Param("欧美", "2"),
)

class StatusFilter : CopyMangaFilter("状态", "status", STATUS_VALUES)

private val STATUS_VALUES = arrayOf(
    Param("全部", ""),
    Param("连载中", "0"),
    Param("已完结", "1"),
    Param("短篇", "2"),
)

class SortFilter : CopyMangaFilter("排序", "ordering", SORT_VALUES)

private val SORT_VALUES = arrayOf(
    Param("热门", "-popular"),
    Param("热门(逆序)", "popular"),
    Param("更新时间", "-datetime_updated"),
    Param("更新时间(逆序)", "datetime_updated"),
)
