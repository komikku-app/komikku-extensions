package eu.kanade.tachiyomi.extension.all.mangapark

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import okhttp3.HttpUrl

class MangaParkFilters {

    internal fun getFilterList(): FilterList {
        return FilterList(
            Filter.Header("NOTE: Ignored if using text search!"),
            Filter.Separator(),
            SortFilter("Sort By", defaultSort, sortList),
            Filter.Separator(),
            MinChapterFilter(),
            MaxChapterFilter(),
            Filter.Separator(),
            PublicationFilter("Status", publicationList, 0),
            TypeFilter("Type", typeList),
            DemographicFilter("Demographic", demographicList),
            ContentFilter("Content", contentList),
            GenreFilter("Genre", genreList),
        )
    }

    internal fun addFiltersToUrl(url: HttpUrl.Builder, filters: FilterList): String {
        var sort = "rating.za"
        var minChap: Int? = null
        var maxChap: Int? = null
        var publication: String? = null
        val includedGenre = mutableListOf<String>()
        val excludedGenre = mutableListOf<String>()

        filters.forEach { filter ->
            when (filter) {
                is SortFilter -> {
                    val sortType = sortList[filter.state!!.index].value
                    val sortDirection = if (filter.state!!.ascending) "az" else "za"
                    sort = "$sortType.$sortDirection"
                }
                is MinChapterFilter -> {
                    try {
                        minChap = filter.state.toInt()
                    } catch (_: NumberFormatException) {
                        // Do Nothing
                    }
                }
                is MaxChapterFilter -> {
                    try {
                        maxChap = filter.state.toInt()
                    } catch (_: NumberFormatException) {
                        // Do Nothing
                    }
                }
                is PublicationFilter -> {
                    if (filter.state != 0) {
                        publication = publicationList[filter.state].value
                    }
                }
                is TypeFilter -> {
                    includedGenre += filter.state.filter { it.isIncluded() }.map { it.value }
                    excludedGenre += filter.state.filter { it.isExcluded() }.map { it.value }
                }
                is DemographicFilter -> {
                    includedGenre += filter.state.filter { it.isIncluded() }.map { it.value }
                    excludedGenre += filter.state.filter { it.isExcluded() }.map { it.value }
                }
                is ContentFilter -> {
                    includedGenre += filter.state.filter { it.isIncluded() }.map { it.value }
                    excludedGenre += filter.state.filter { it.isExcluded() }.map { it.value }
                }
                is GenreFilter -> {
                    includedGenre += filter.state.filter { it.isIncluded() }.map { it.value }
                    excludedGenre += filter.state.filter { it.isExcluded() }.map { it.value }
                }
                else -> {}
            }
        }

        return url.apply {
            if (sort != "rating.za") {
                addQueryParameter(
                    "sort",
                    sort,
                )
            }
            if (includedGenre.isNotEmpty() || excludedGenre.isNotEmpty()) {
                addQueryParameter(
                    "genres",
                    includedGenre.joinToString(",") + "|" + excludedGenre.joinToString(","),
                )
            }
            if (publication != null) {
                addQueryParameter(
                    "release",
                    publication,
                )
            }
            addQueryParameter(
                "chapters",
                minMaxToChapter(minChap, maxChap),
            )
        }.toString()
    }

    private fun minMaxToChapter(minChap: Int?, maxChap: Int?): String? {
        if (minChap == null && maxChap == null) return null
        return when {
            minChap != null && maxChap == null -> minChap
            minChap == null && maxChap != null -> "0-$maxChap"
            else -> "$minChap-$maxChap"
        }.toString()
    }

    // Sort Filter
    class SortItem(val name: String, val value: String)

    private val sortList: List<SortItem> = listOf(
        SortItem("Rating", "rating"),
        SortItem("Comments", "comments"),
        SortItem("Discuss", "discuss"),
        SortItem("Update", "update"),
        SortItem("Create", "create"),
        SortItem("Name", "name"),
        SortItem("Total Views", "d000"),
        SortItem("Most Views 360 days", "d360"),
        SortItem("Most Views 180 days", "d180"),
        SortItem("Most Views 90 days", "d090"),
        SortItem("Most Views 30 days", "d030"),
        SortItem("Most Views 7 days", "d007"),
        SortItem("Most Views 24 hours", "h024"),
        SortItem("Most Views 12 hours", "h012"),
        SortItem("Most Views 6 hours", "h006"),
        SortItem("Most Views 60 minutes", "h001"),
    )

    class SortDefault(val defaultSortIndex: Int, val ascending: Boolean)

    private val defaultSort: SortDefault = SortDefault(0, false)

    class SortFilter(name: String, default: SortDefault, sorts: List<SortItem>) :
        Filter.Sort(
            name,
            sorts.map { it.name }.toTypedArray(),
            Selection(default.defaultSortIndex, default.ascending),
        )

    // Min - Max Chapter Filter
    abstract class TextFilter(name: String) : Filter.Text(name)

    class MinChapterFilter : TextFilter("Min. Chapters")
    class MaxChapterFilter : TextFilter("Max. Chapters")

    // Publication Filter
    class PublicationItem(val name: String, val value: String)

    private val publicationList: List<PublicationItem> = listOf(
        PublicationItem("All", ""),
        PublicationItem("Pending", "pending"),
        PublicationItem("Ongoing", "ongoing"),
        PublicationItem("Completed", "completed"),
        PublicationItem("Hiatus", "hiatus"),
        PublicationItem("Cancelled", "cancelled"),
    )

    class PublicationFilter(
        name: String,
        statusList: List<PublicationItem>,
        defaultStatusIndex: Int,
    ) :
        Filter.Select<String>(
            name,
            statusList.map { it.name }.toTypedArray(),
            defaultStatusIndex,
        )

    // Type
    class TypeItem(name: String, val value: String) : Filter.TriState(name)

    private val typeList: List<TypeItem> = listOf(
        TypeItem("Cartoon", "cartoon"),
        TypeItem("Comic", "comic"),
        TypeItem("Doujinshi", "doujinshi"),
        TypeItem("Manga", "manga"),
        TypeItem("Manhua", "manhua"),
        TypeItem("Manhwa", "manhwa"),
        TypeItem("Webtoon", "webtoon"),
    )

    class TypeFilter(name: String, typeList: List<TypeItem>) :
        Filter.Group<TypeItem>(name, typeList)

    // Demographic
    class DemographicItem(name: String, val value: String) : Filter.TriState(name)

    private val demographicList: List<DemographicItem> = listOf(
        DemographicItem("Shounen", "shounen"),
        DemographicItem("Shoujo", "shoujo"),
        DemographicItem("Seinen", "seinen"),
        DemographicItem("Josei", "josei"),
    )

    class DemographicFilter(name: String, demographicList: List<DemographicItem>) :
        Filter.Group<DemographicItem>(name, demographicList)

    // Content
    class ContentItem(name: String, val value: String) : Filter.TriState(name)

    private val contentList: List<ContentItem> = listOf(
        ContentItem("Adult", "adult"),
        ContentItem("Ecchi", "ecchi"),
        ContentItem("Gore", "gore"),
        ContentItem("Hentai", "hentai"),
        ContentItem("Mature", "mature"),
        ContentItem("Smut", "smut"),
    )

    class ContentFilter(name: String, contentList: List<ContentItem>) :
        Filter.Group<ContentItem>(name, contentList)

    // Genre
    class GenreItem(name: String, val value: String) : Filter.TriState(name)

    private val genreList: List<GenreItem> = listOf(
        GenreItem("Action", "action"),
        GenreItem("Adaptation", "adaptation"),
        GenreItem("Adventure", "adventure"),
        GenreItem("Aliens", "aliens"),
        GenreItem("Animals", "animals"),
        GenreItem("Anthology", "anthology"),
        GenreItem("Award Winning", "award_winning"), // This Is Hidden In Web
        GenreItem("Cars", "cars"),
        GenreItem("Comedy", "comedy"),
        GenreItem("Cooking", "cooking"),
        GenreItem("Crime", "crime"),
        GenreItem("Crossdressing", "crossdressing"),
        GenreItem("Delinquents", "delinquents"),
        GenreItem("Dementia", "dementia"),
        GenreItem("Demons", "demons"),
        GenreItem("Drama", "drama"),
        GenreItem("Fantasy", "fantasy"),
        GenreItem("Full Color", "full_color"),
        GenreItem("Game", "game"),
        GenreItem("Gender Bender", "gender_bender"),
        GenreItem("Genderswap", "genderswap"),
        GenreItem("Gyaru", "gyaru"),
        GenreItem("Harem", "harem"),
        GenreItem("Historical", "historical"),
        GenreItem("Horror", "horror"),
        GenreItem("Incest", "incest"),
        GenreItem("Isekai", "isekai"),
        GenreItem("Kids", "kids"),
        GenreItem("Loli", "loli"),
        GenreItem("Lolicon", "lolicon"),
        GenreItem("Magic", "magic"),
        GenreItem("Magical Girls", "magical_girls"),
        GenreItem("Martial Arts", "martial_arts"),
        GenreItem("Mecha", "mecha"),
        GenreItem("Medical", "medical"),
        GenreItem("Military", "military"),
        GenreItem("Monster Girls", "monster_girls"),
        GenreItem("Monsters", "monsters"),
        GenreItem("Music", "music"),
        GenreItem("Mystery", "mystery"),
        GenreItem("Office Workers", "office_workers"),
        GenreItem("Oneshot", "oneshot"),
        GenreItem("Parody", "parody"),
        GenreItem("Philosophical", "philosophical"),
        GenreItem("Police", "police"),
        GenreItem("Post Apocalyptic", "post_apocalyptic"),
        GenreItem("Psychological", "psychological"),
        GenreItem("Reincarnation", "reincarnation"),
        GenreItem("Romance", "romance"),
        GenreItem("Samurai", "samurai"),
        GenreItem("School Life", "school_life"),
        GenreItem("Sci-fi", "sci_fi"),
        GenreItem("Shotacon", "shotacon"),
        GenreItem("Shounen Ai", "shounen_ai"),
        GenreItem("Shoujo Ai", "shoujo_ai"),
        GenreItem("Slice of Life", "slice_of_life"),
        GenreItem("Space", "space"),
        GenreItem("Sports", "sports"),
        GenreItem("Super Power", "super_power"),
        GenreItem("Superhero", "superhero"),
        GenreItem("Supernatural", "supernatural"),
        GenreItem("Survival", "survival"),
        GenreItem("Thriller", "thriller"),
        GenreItem("Traditional Games", "traditional_games"),
        GenreItem("Tragedy", "tragedy"),
        GenreItem("Vampires", "vampires"),
        GenreItem("Video Games", "video_games"),
        GenreItem("Virtual Reality", "virtual_reality"),
        GenreItem("Wuxia", "wuxia"),
        GenreItem("Yaoi", "yaoi"),
        GenreItem("Yuri", "yuri"),
        GenreItem("Zombies", "zombies"),
    )

    class GenreFilter(name: String, genreList: List<GenreItem>) :
        Filter.Group<GenreItem>(name, genreList)
}
