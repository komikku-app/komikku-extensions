package eu.kanade.tachiyomi.extension.zh.boylove

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import org.jsoup.select.Evaluator
import java.text.SimpleDateFormat
import java.util.Locale

internal const val IMAGE_HOST = "https://blcnimghost1.cc" // 也有 2

@Serializable
class MangaDto(
    val id: Int,
    val title: String,
    val image: String,
    val auther: String,
    val desc: String,
    val mhstatus: Int,
    val keyword: String,
) {
    fun toSManga() = SManga.create().apply {
        url = id.toString()
        title = this@MangaDto.title
        author = auther
        description = desc
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

fun String.toImageUrl() = if (startsWith("http")) this else "$IMAGE_HOST$this"

@Serializable
class ChapterDto(
    val id: Int,
    val title: String,
    val manhua_id: Int,
    val update_time: String,
    val content: String?,
    val imagelist: String,
) {
    fun toSChapter() = SChapter.create().apply {
        url = "$manhua_id/$id:${getImages()}"
        name = title.trim()
        date_upload = dateFormat.parse(update_time)?.time ?: 0
    }

    private fun getImages(): String {
        return imagelist.ifEmpty {
            if (content == null) return ""
            Jsoup.parse(content).select(Evaluator.Tag("img"))
                .joinToString(",") { it.attr("src") }
        }
    }
}

private val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH) }

@Serializable
class ListPageDto<T>(val lastPage: Boolean, val list: List<T>)

@Serializable
class ResultDto<T>(val result: T)
