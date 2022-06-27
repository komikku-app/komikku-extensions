package eu.kanade.tachiyomi.extension.zh.copymanga

import com.luhuiguo.chinese.ChineseUtils
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

@Serializable
class MangaDto(
    val name: String,
    val path_word: String,
    val author: List<KeywordDto>,
    val cover: String,
    val region: ValueDto? = null,
    val status: ValueDto? = null,
    val theme: List<KeywordDto>? = null,
    val brief: String? = null,
) {
    fun toSManga() = SManga.create().apply {
        url = URL_PREFIX + path_word
        title = if (convertToSc) ChineseUtils.toSimplified(name) else name
        author = this@MangaDto.author.joinToString { it.name }
        thumbnail_url = cover.removeSuffix(".328x422.jpg")
    }

    fun toSMangaDetails(groups: ChapterGroups) = toSManga().apply {
        description = brief + groups.toDescription()
        genre = buildList(theme!!.size + 1) {
            add(region!!.display)
            theme.mapTo(this) { it.name }
        }.joinToString { ChineseUtils.toSimplified(it) }
        status = when (this@MangaDto.status!!.value) {
            0 -> SManga.ONGOING
            1 -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
        initialized = true
    }

    companion object {
        internal var convertToSc = false

        const val URL_PREFIX = "/comic/"

        private const val CHAPTER_GROUP_DELIMITER = "，"
        private const val CHAPTER_GROUP_PREFIX = "\n\n【其他版本："
        private const val CHAPTER_GROUP_POSTFIX = "】"
        private const val NO_CHAPTER_GROUP = "无"

        private fun ChapterGroups.toDescription(): String {
            if (size <= 1) return CHAPTER_GROUP_PREFIX + NO_CHAPTER_GROUP + CHAPTER_GROUP_POSTFIX
            val groups = ArrayList<KeywordDto>(size - 1)
            for ((key, group) in this) {
                if (key != "default") groups.add(group)
            }
            return groups.joinToString(CHAPTER_GROUP_DELIMITER, CHAPTER_GROUP_PREFIX, CHAPTER_GROUP_POSTFIX) {
                it.name + '#' + it.path_word
            }
        }

        fun String.parseChapterGroups(): List<KeywordDto>? {
            val index = lastIndexOf(CHAPTER_GROUP_PREFIX)
            if (index < 0) return null
            val groups = substring(index + CHAPTER_GROUP_PREFIX.length, length - CHAPTER_GROUP_POSTFIX.length)
            if (groups == NO_CHAPTER_GROUP) return emptyList()
            return groups.split(CHAPTER_GROUP_DELIMITER).map {
                val delimiterIndex = it.indexOf('#')
                KeywordDto(it.substring(0, delimiterIndex), it.substring(delimiterIndex + 1, it.length))
            }
        }
    }
}

@Serializable
class ChapterDto(
    val uuid: String,
    val name: String,
    val comic_path_word: String,
    val datetime_created: String,
) {
    fun toSChapter(group: String) = SChapter.create().apply {
        url = "/comic/$comic_path_word/chapter/$uuid"
        name = if (group.isEmpty()) this@ChapterDto.name else group + '：' + this@ChapterDto.name
        date_upload = dateFormat.parse(datetime_created)?.time ?: 0
    }

    companion object {
        val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    }
}

@Serializable
class KeywordDto(val name: String, val path_word: String) {
    fun toParam() = Param(ChineseUtils.toSimplified(name), path_word)
}

@Serializable
class ValueDto(val value: Int, val display: String)

@Serializable
class MangaWrapperDto(val comic: MangaDto, val groups: ChapterGroups? = null) {
    fun toSManga() = comic.toSManga()
    fun toSMangaDetails() = comic.toSMangaDetails(groups!!)
}

typealias ChapterGroups = LinkedHashMap<String, KeywordDto>

@Serializable
class ChapterPageListDto(val contents: List<UrlDto>)

@Serializable
class UrlDto(val url: String)

@Serializable
class ChapterPageListWrapperDto(val chapter: ChapterPageListDto, val show_app: Boolean)

@Serializable
class ListDto<T>(
    val total: Int,
    val limit: Int,
    val offset: Int,
    val list: List<T>,
)

@Serializable
class ResultDto<T>(val results: T)

@Serializable
class ResultMessageDto(val code: Int, val message: String)
