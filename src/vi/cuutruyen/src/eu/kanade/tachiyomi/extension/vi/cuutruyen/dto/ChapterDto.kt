package eu.kanade.tachiyomi.extension.vi.cuutruyen.dto

import eu.kanade.tachiyomi.extension.vi.cuutruyen.CuuTruyenImageInterceptor
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
}

@Serializable
data class ChapterDto(
    val id: Int,
    val order: Int,
    val number: String,
    @SerialName("updated_at") val updatedAt: String,
    val name: String? = null,
    val pages: List<PageDto>? = null,
) {
    fun toSChapter(mangaUrl: String) = SChapter.create().apply {
        val dto = this@ChapterDto
        url = "$mangaUrl/chapters/$id"
        name = "Chapter ${dto.number}"
        if (dto.name != null && dto.name.isNotBlank()) {
            name += ": ${dto.name}"
        }

        date_upload = runCatching {
            DATE_FORMATTER.parse(dto.updatedAt.replace("+07:00", "Z"))?.time
        }.getOrNull() ?: 0L

        chapter_number = dto.number.toFloatOrNull() ?: -1f
    }
}

@Serializable
data class PageDto(
    val id: Int,
    val order: Int,
    val width: Int,
    val height: Int,
    val status: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("image_url_size") val imageUrlSize: Int,
    @SerialName("drm_data") val drmData: String,
) {
    fun toPage(): Page {
        val dto = this@PageDto
        val url = imageUrl.toHttpUrl().newBuilder()
            .fragment("${CuuTruyenImageInterceptor.KEY}=$drmData")
            .build()
            .toString()
        return Page(dto.order, imageUrl = url)
    }
}
