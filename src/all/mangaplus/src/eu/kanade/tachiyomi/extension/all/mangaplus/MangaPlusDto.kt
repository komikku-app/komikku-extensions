package eu.kanade.tachiyomi.extension.all.mangaplus

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MangaPlusResponse(
    val success: SuccessResult? = null,
    val error: ErrorResult? = null,
)

@Serializable
data class ErrorResult(val popups: List<Popup> = emptyList()) {

    fun langPopup(lang: Language): Popup? =
        popups.firstOrNull { it.language == lang }
}

@Serializable
data class Popup(
    val subject: String,
    val body: String,
    val language: Language? = Language.ENGLISH,
)

@Serializable
data class SuccessResult(
    val isFeaturedUpdated: Boolean? = false,
    val titleRankingView: TitleRankingView? = null,
    val titleDetailView: TitleDetailView? = null,
    val mangaViewer: MangaViewer? = null,
    val allTitlesViewV2: AllTitlesViewV2? = null,
    val webHomeViewV3: WebHomeViewV3? = null,
)

@Serializable
data class TitleRankingView(val titles: List<Title> = emptyList())

@Serializable
data class AllTitlesViewV2(
    @SerialName("AllTitlesGroup") val allTitlesGroup: List<AllTitlesGroup> = emptyList(),
)

@Serializable
data class AllTitlesGroup(
    val theTitle: String,
    val titles: List<Title> = emptyList(),
)

@Serializable
data class WebHomeViewV3(val groups: List<UpdatedTitleV2Group> = emptyList())

@Serializable
data class TitleDetailView(
    val title: Title,
    val titleImageUrl: String,
    val overview: String? = null,
    val backgroundImageUrl: String,
    val nextTimeStamp: Int = 0,
    val viewingPeriodDescription: String = "",
    val nonAppearanceInfo: String = "",
    val firstChapterList: List<Chapter> = emptyList(),
    val lastChapterList: List<Chapter> = emptyList(),
    val isSimulReleased: Boolean = false,
    val chaptersDescending: Boolean = true,
) {
    private val isWebtoon: Boolean
        get() = firstChapterList.all(Chapter::isVerticalOnly) &&
            lastChapterList.all(Chapter::isVerticalOnly)

    private val isOneShot: Boolean
        get() = chapterCount == 1 && firstChapterList.firstOrNull()
            ?.name?.equals("one-shot", true) == true

    private val chapterCount: Int
        get() = firstChapterList.size + lastChapterList.size

    private val isReEdition: Boolean
        get() = viewingPeriodDescription.contains(REEDITION_REGEX)

    private val isCompleted: Boolean
        get() = nonAppearanceInfo.contains(COMPLETED_REGEX) || isOneShot

    private val isOnHiatus: Boolean
        get() = nonAppearanceInfo.contains(HIATUS_REGEX)

    private val genres: List<String>
        get() = listOfNotNull(
            "Simulrelease".takeIf { isSimulReleased && !isReEdition && !isOneShot },
            "One-shot".takeIf { isOneShot },
            "Re-edition".takeIf { isReEdition },
            "Webtoon".takeIf { isWebtoon },
        )

    fun toSManga(): SManga = title.toSManga().apply {
        description = (overview.orEmpty() + "\n\n" + viewingPeriodDescription).trim()
        status = when {
            isCompleted -> SManga.COMPLETED
            isOnHiatus -> SManga.ON_HIATUS
            else -> SManga.ONGOING
        }
        genre = genres.joinToString()
    }

    companion object {
        private val COMPLETED_REGEX = "completado|complete|completo".toRegex()
        private val HIATUS_REGEX = "on a hiatus".toRegex(RegexOption.IGNORE_CASE)
        private val REEDITION_REGEX = "revival|remasterizada".toRegex()
    }
}

@Serializable
data class MangaViewer(
    val pages: List<MangaPlusPage> = emptyList(),
    val titleId: Int? = null,
    val titleName: String? = null,
)

@Serializable
data class Title(
    val titleId: Int,
    val name: String,
    val author: String? = null,
    val portraitImageUrl: String,
    val landscapeImageUrl: String,
    val viewCount: Int = 0,
    val language: Language? = Language.ENGLISH,
) {

    fun toSManga(): SManga = SManga.create().apply {
        title = name
        author = this@Title.author?.replace(" / ", ", ")
        artist = author
        thumbnail_url = portraitImageUrl
        url = "#/titles/$titleId"
    }
}

enum class Language {
    ENGLISH,
    SPANISH,
    FRENCH,
    INDONESIAN,
    PORTUGUESE_BR,
    RUSSIAN,
    THAI,
}

@Serializable
data class UpdatedTitleV2Group(
    val groupName: String,
    val titleGroups: List<OriginalTitleGroup> = emptyList(),
)

@Serializable
data class OriginalTitleGroup(
    val theTitle: String,
    val titles: List<UpdatedTitle> = emptyList(),
)

@Serializable
data class UpdatedTitle(val title: Title)

@Serializable
data class Chapter(
    val titleId: Int,
    val chapterId: Int,
    val name: String,
    val subTitle: String? = null,
    val startTimeStamp: Int,
    val endTimeStamp: Int,
    val isVerticalOnly: Boolean = false,
) {

    val isExpired: Boolean
        get() = subTitle == null

    fun toSChapter(): SChapter = SChapter.create().apply {
        name = "${this@Chapter.name} - $subTitle"
        date_upload = 1000L * startTimeStamp
        url = "#/viewer/$chapterId"
        chapter_number = this@Chapter.name.substringAfter("#").toFloatOrNull() ?: -1f
    }
}

@Serializable
data class MangaPlusPage(val mangaPage: MangaPage? = null)

@Serializable
data class MangaPage(
    val imageUrl: String,
    val width: Int,
    val height: Int,
    val encryptionKey: String? = null,
)
