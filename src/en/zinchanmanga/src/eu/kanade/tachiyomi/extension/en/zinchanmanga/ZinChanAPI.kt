package eu.kanade.tachiyomi.extension.en.zinchanmanga

import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.serialization.SerialName as N

@Serializable
data class Data<T>(
    private val data: List<T>,
    private val err_message: String,
) : List<T> by data {
    init { require(err_message == "Success") { err_message } }
}

@Serializable
data class SeriesList(
    private val data: Pagination,
    private val err_message: String,
) : List<Series> by data {
    init { require(err_message == "Success") { err_message } }

    val pages by lazy { data.pages }
}

@Serializable
data class Pagination(
    @N("total_page") val pages: Int,
    private val data: List<Series>,
) : List<Series> by data

@Serializable
data class Series(
    private val id_story: Int,
    @N("name_story") val title: String,
    private val slug_story: String,
    @N("status_story") val status: String? = null,
    private val name_genre: String,
    private val name_author: String,
    @N("thumbnail_story") val cover: String,
    private val content_story: String? = null,
) {
    val url by lazy {
        "$slug_story?id=$id_story"
    }

    val description by lazy {
        content_story?.let(Jsoup::parse)?.text()
            ?.takeIf { "Updating" !in it }
    }

    val genres by lazy {
        name_genre.trim('|').replace("|", ", ")
    }

    val authors by lazy {
        name_author.replace(",|", "|").trim('|').takeIf {
            it != "Updating" && it != "Đang Cập Nhật"
        }?.replace("Author: ", "")?.replace("   ", ", ")
    }
}

@Serializable
data class Chapter(
    private val id_chapter: Int,
    private val name_chapter: String,
    private val latest_update_chapter: String,
    private val name_extend: String,
) {
    val params by lazy {
        "?id_chapter=$id_chapter&type_story=manga"
    }

    val title by lazy {
        buildString {
            append(name_chapter)
            if (name_extend != "") {
                append(" - ")
                append(name_extend)
            }
        }
    }

    val number by lazy {
        name_chapter.substringAfter(' ').toFloatOrNull() ?: -1f
    }

    val timestamp by lazy {
        dateFormat.parse(latest_update_chapter)?.time ?: 0L
    }

    companion object {
        private val dateFormat by lazy {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        }
    }
}

@Serializable
data class PageList(
    private val data_chapter: List<String>,
) : List<String> by data_chapter
