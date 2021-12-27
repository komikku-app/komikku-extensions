package eu.kanade.tachiyomi.extension.all.mangaplus

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class MangaPlusResponse(
    @ProtoNumber(1) val success: SuccessResult? = null,
    @ProtoNumber(2) val error: ErrorResult? = null
)

@Serializable
data class ErrorResult(
    @ProtoNumber(2) val englishPopup: Popup,
    @ProtoNumber(3) val spanishPopup: Popup
)

@Serializable
data class Popup(
    @ProtoNumber(1) val subject: String,
    @ProtoNumber(2) val body: String
)

@Serializable
data class SuccessResult(
    @ProtoNumber(1) val isFeaturedUpdated: Boolean? = false,
    @ProtoNumber(6) val titleRankingView: TitleRankingView? = null,
    @ProtoNumber(8) val titleDetailView: TitleDetailView? = null,
    @ProtoNumber(10) val mangaViewer: MangaViewer? = null,
    @ProtoNumber(25) val allTitlesViewV2: AllTitlesViewV2? = null,
    @ProtoNumber(31) val webHomeViewV3: WebHomeViewV3? = null
)

@Serializable
data class TitleRankingView(@ProtoNumber(1) val titles: List<Title> = emptyList())

@Serializable
data class AllTitlesViewV2(
    @ProtoNumber(1) val allTitlesGroup: List<AllTitlesGroup> = emptyList()
)

@Serializable
data class AllTitlesGroup(
    @ProtoNumber(1) val theTitle: String,
    @ProtoNumber(2) val titles: List<Title> = emptyList()
)

@Serializable
data class WebHomeViewV3(@ProtoNumber(2) val groups: List<UpdatedTitleV2Group> = emptyList())

@Serializable
data class TitleDetailView(
    @ProtoNumber(1) val title: Title,
    @ProtoNumber(2) val titleImageUrl: String,
    @ProtoNumber(3) val overview: String,
    @ProtoNumber(4) val backgroundImageUrl: String,
    @ProtoNumber(5) val nextTimeStamp: Int = 0,
    @ProtoNumber(7) val viewingPeriodDescription: String = "",
    @ProtoNumber(8) val nonAppearanceInfo: String = "",
    @ProtoNumber(9) val firstChapterList: List<Chapter> = emptyList(),
    @ProtoNumber(10) val lastChapterList: List<Chapter> = emptyList(),
    @ProtoNumber(14) val isSimulReleased: Boolean = false,
    @ProtoNumber(17) val chaptersDescending: Boolean = true
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
        get() = viewingPeriodDescription.contains(MangaPlus.REEDITION_REGEX)

    val isCompleted: Boolean
        get() = nonAppearanceInfo.contains(MangaPlus.COMPLETED_REGEX) || isOneShot

    val genres: List<String>
        get() = listOf(
            if (isSimulReleased && !isReEdition) "Simulrelease" else "",
            if (isOneShot) "One-shot" else "",
            if (isReEdition) "Re-edition" else "",
            if (isWebtoon) "Webtoon" else ""
        )
}

@Serializable
data class MangaViewer(@ProtoNumber(1) val pages: List<MangaPlusPage> = emptyList())

@Serializable
data class Title(
    @ProtoNumber(1) val titleId: Int,
    @ProtoNumber(2) val name: String,
    @ProtoNumber(3) val author: String,
    @ProtoNumber(4) val portraitImageUrl: String,
    @ProtoNumber(5) val landscapeImageUrl: String,
    @ProtoNumber(6) val viewCount: Int = 0,
    @ProtoNumber(7) val language: Language? = Language.ENGLISH
)

@Serializable
enum class Language(val id: Int) {
    @ProtoNumber(0)
    ENGLISH(0),

    @ProtoNumber(1)
    SPANISH(1),

    @ProtoNumber(2)
    FRENCH(2),

    @ProtoNumber(3)
    INDONESIAN(4),

    @ProtoNumber(4)
    PORTUGUESE_BR(4),

    @ProtoNumber(5)
    RUSSIAN(5),

    @ProtoNumber(6)
    THAI(6)
}

@Serializable
data class UpdatedTitleV2Group(
    @ProtoNumber(1) val groupName: String,
    @ProtoNumber(2) val titleGroups: List<OriginalTitleGroup> = emptyList()
)

@Serializable
data class OriginalTitleGroup(
    @ProtoNumber(1) val theTitle: String,
    @ProtoNumber(3) val titles: List<UpdatedTitle> = emptyList()
)

@Serializable
data class UpdatedTitle(@ProtoNumber(1) val title: Title)

@Serializable
data class Chapter(
    @ProtoNumber(1) val titleId: Int,
    @ProtoNumber(2) val chapterId: Int,
    @ProtoNumber(3) val name: String,
    @ProtoNumber(4) val subTitle: String? = null,
    @ProtoNumber(6) val startTimeStamp: Int,
    @ProtoNumber(7) val endTimeStamp: Int,
    @ProtoNumber(9) val isVerticalOnly: Boolean = false
)

@Serializable
data class MangaPlusPage(@ProtoNumber(1) val page: MangaPage? = null)

@Serializable
data class MangaPage(
    @ProtoNumber(1) val imageUrl: String,
    @ProtoNumber(2) val width: Int,
    @ProtoNumber(3) val height: Int,
    @ProtoNumber(5) val encryptionKey: String? = null
)
