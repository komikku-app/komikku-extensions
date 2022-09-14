package eu.kanade.tachiyomi.extension.zh.boylove

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.long
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
class MangaDto(
    val id: Int,
    private val title: String,
    private val update_time: JsonPrimitive,
    private val image: String,
    private val auther: String,
    private val desc: String,
    private val mhstatus: Int,
    private val keyword: String,
) {
    fun toSManga() = SManga.create().apply {
        url = id.toString()
        title = this@MangaDto.title
        author = auther
        val updateTime = if (update_time.isString) {
            update_time.content
        } else {
            dateFormat.format(Date(update_time.long * 1000))
        }
        description = "更新时间：$updateTime\n\n$desc"
        genre = keyword.replace(",", ", ")
        status = when (mhstatus) {
            0 -> SManga.ONGOING
            1 -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
        thumbnail_url = image.toImageUrl()
        initialized = true
    }
}

fun String.toImageUrl() =
    if (startsWith("http")) {
        this
    } else {
        val i = (hashCode() and 1) + 1 // 1 or 2
        "https://blcnimghost$i.cc$this"
    }

@Serializable
class ChapterDto(
    private val id: Int,
    private val title: String,
) {
    fun toSChapter() = SChapter.create().apply {
        url = "/home/book/capter/id/$id"
        name = title.trim()
    }
}

private val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH) }

@Serializable
class ListPageDto<T>(val lastPage: Boolean, val list: List<T>)

@Serializable
class ResultDto<T>(val result: T)
