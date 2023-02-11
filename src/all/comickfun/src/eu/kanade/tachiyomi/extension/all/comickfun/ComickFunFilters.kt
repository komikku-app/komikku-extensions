package eu.kanade.tachiyomi.extension.all.comickfun

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList

internal fun getFilters(): FilterList {
    return FilterList(
        Filter.Header(name = "NOTE: Everything below is ignored if using text search"),
        CompletedFilter("Completed translation"),
        GenreFilter("Genre", getGenresList),
        DemographicFilter("Demographic", getDemographicList),
        TypeFilter("Type", getTypeList),
        SortFilter("Sort", getSortsList),
        CreatedAtFilter("Created At", getCreatedAtList),
        MinimumFilter("Minimum Chapters"),
        Filter.Header("From Year, ex: 2010"),
        FromYearFilter("From"),
        Filter.Header("To Year, ex: 2021"),
        ToYearFilter("To"),
        Filter.Header("Separate tags with commas"),
        TagFilter("Tags"),

    )
}

/** Filters **/
internal class GenreFilter(name: String, genreList: List<TriState>) : Group(name, genreList)

internal class TagFilter(name: String) : Text(name)

internal class DemographicFilter(name: String, demographicList: List<CheckBox>) :
    Group(name, demographicList)

internal class TypeFilter(name: String, typeList: List<CheckBox>) :
    Group(name, typeList)

internal class CompletedFilter(name: String) : CheckBox(name)

internal class CreatedAtFilter(name: String, createdAtList: Array<Pair<String, String>>) :
    Select(name, createdAtList)

internal class MinimumFilter(name: String) : Text(name)

internal class FromYearFilter(name: String) : Text(name)

internal class ToYearFilter(name: String) : Text(name)

internal class SortFilter(name: String, sortList: Array<Pair<String, String>>) :
    Select(name, sortList)

/** Generics **/
internal open class Group(name: String, values: List<Any>) :
    Filter.Group<Any>(name, values)

internal open class TriState(name: String, val value: String) : Filter.TriState(name)

internal open class Text(name: String) : Filter.Text(name)

internal open class CheckBox(name: String, val value: String = "") : Filter.CheckBox(name)

internal open class Select(name: String, private val vals: Array<Pair<String, String>>) :
    Filter.Select<String>(name, vals.map { it.first }.toTypedArray()) {
    fun getValue() = vals[state].second
}

/** Filters Data **/
private val getGenresList: List<TriState> = listOf(
    TriState("4-Koma", "4-koma"),
    TriState("Action", "action"),
    TriState("Adaptation", "adaptation"),
    TriState("Adult", "adult"),
    TriState("Adventure", "adventure"),
    TriState("Aliens", "aliens"),
    TriState("Animals", "animals"),
    TriState("Anthology", "anthology"),
    TriState("Award Winning", "award-winning"),
    TriState("Comedy", "comedy"),
    TriState("Cooking", "cooking"),
    TriState("Crime", "crime"),
    TriState("Crossdressing", "crossdressing"),
    TriState("Delinquents", "delinquents"),
    TriState("Demons", "demons"),
    TriState("Doujinshi", "doujinshi"),
    TriState("Drama", "drama"),
    TriState("Ecchi", "ecchi"),
    TriState("Fan Colored", "fan-colored"),
    TriState("Fantasy", "fantasy"),
    TriState("Full Color", "full-color"),
    TriState("Gender Bender", "gender-bender"),
    TriState("Genderswap", "genderswap"),
    TriState("Ghosts", "ghosts"),
    TriState("Gore", "gore"),
    TriState("Gyaru", "gyaru"),
    TriState("Harem", "harem"),
    TriState("Historical", "historical"),
    TriState("Horror", "horror"),
    TriState("Incest", "incest"),
    TriState("Isekai", "isekai"),
    TriState("Loli", "loli"),
    TriState("Long Strip", "long-strip"),
    TriState("Mafia", "mafia"),
    TriState("Magic", "magic"),
    TriState("Magical Girls", "magical-girls"),
    TriState("Martial Arts", "martial-arts"),
    TriState("Mature", "mature"),
    TriState("Mecha", "mecha"),
    TriState("Medical", "medical"),
    TriState("Military", "military"),
    TriState("Monster Girls", "monster-girls"),
    TriState("Monsters", "monsters"),
    TriState("Music", "music"),
    TriState("Mystery", "mystery"),
    TriState("Ninja", "ninja"),
    TriState("Office Workers", "office-workers"),
    TriState("Official Colored", "official-colored"),
    TriState("Oneshot", "oneshot"),
    TriState("Philosophical", "philosophical"),
    TriState("Police", "police"),
    TriState("Post-Apocalyptic", "post-apocalyptic"),
    TriState("Psychological", "psychological"),
    TriState("Reincarnation", "reincarnation"),
    TriState("Reverse Harem", "reverse-harem"),
    TriState("Romance", "romance"),
    TriState("Samurai", "samurai"),
    TriState("School Life", "school-life"),
    TriState("Sci-Fi", "sci-fi"),
    TriState("Sexual Violence", "sexual-violence"),
    TriState("Shota", "shota"),
    TriState("Shoujo Ai", "shoujo-ai"),
    TriState("Shounen Ai", "shounen-ai"),
    TriState("Slice of Life", "slice-of-life"),
    TriState("Smut", "smut"),
    TriState("Sports", "sports"),
    TriState("Superhero", "superhero"),
    TriState("Supernatural", "supernatural"),
    TriState("Survival", "survival"),
    TriState("Thriller", "thriller"),
    TriState("Time Travel", "time-travel"),
    TriState("Traditional Games", "traditional-games"),
    TriState("Tragedy", "tragedy"),
    TriState("User Created", "user-created"),
    TriState("Vampires", "vampires"),
    TriState("Video Games", "video-games"),
    TriState("Villainess", "villainess"),
    TriState("Virtual Reality", "virtual-reality"),
    TriState("Web Comic", "web-comic"),
    TriState("Wuxia", "wuxia"),
    TriState("Yaoi", "yaoi"),
    TriState("Yuri", "yuri"),
    TriState("Zombies", "zombies"),
)

private val getDemographicList: List<CheckBox> = listOf(
    CheckBox("Shounen", "1"),
    CheckBox("Shoujo", "2"),
    CheckBox("Seinen", "3"),
    CheckBox("Josei", "4"),
)

private val getTypeList: List<CheckBox> = listOf(
    CheckBox("Manga", "jp"),
    CheckBox("Manhwa", "kr"),
    CheckBox("Manhua", "cn"),
)

private val getCreatedAtList: Array<Pair<String, String>> = arrayOf(
    Pair("", ""),
    Pair("30 days", "30"),
    Pair("3 months", "90"),
    Pair("6 months", "180"),
    Pair("1 year", "365"),
)

private val getSortsList: Array<Pair<String, String>> = arrayOf(
    Pair("Most follows", "user_follow_count"),
    Pair("Most views", "view"),
    Pair("High rating", "rating"),
    Pair("Last updated", "uploaded"),
)
