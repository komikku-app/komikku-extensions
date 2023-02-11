package eu.kanade.tachiyomi.extension.en.hwtmanga

import eu.kanade.tachiyomi.source.model.Filter

class Tag(name: String, private val id: String) : Filter.CheckBox(name) {
    override fun toString() = id
}

private val tags: List<Tag>
    get() = listOf(
        Tag("Action", "action"),
        Tag("Adventure", "adventure"),
        Tag("Comedy", "comedy"),
        Tag("Cooking", "cooking"),
        Tag("Drama", "drama"),
        Tag("Fantasy", "fantasy"),
        Tag("Horror", "horror"),
        Tag("Mystery", "mystery"),
        Tag("Martial Arts", "martialarts"),
        Tag("Romance", "romance"),
        Tag("School Life", "school"),
        Tag("Shoujo", "shoujo"),
        Tag("Shounen", "shounen"),
        Tag("Supernatural", "supernatural"),
        Tag("Sci-fi", "sci-fi"),
        Tag("Slice of Life", "slice of life"),
        Tag("Adult", "adult"),
        Tag("Ancient era", "ancient era"),
        Tag("Arranged Marriage", "arranged_marriage"),
        Tag("Age gap", "age_gap"),
        Tag("Betrayal", "betrayal"),
        Tag("Clan", "clan"),
        Tag("Childhood Friends", "childhood_friends"),
        Tag("Couple", "couple"),
        Tag("Crime", "crime"),
        Tag("Cultivation", "cultivation"),
        Tag("Comic", "comic"),
        Tag("Delinquent", "delinquent"),
        Tag("Doujinshi", "doujinshi"),
        Tag("Ecchi", "ecchi"),
        Tag("Family", "family"),
        Tag("Fetishes", "fetish"),
        Tag("Gender Bender", "gender_bender"),
        Tag("Gyaru", "gyaru"),
        Tag("Harem", "harem"),
        Tag("Historical", "historical"),
        Tag("Isekai", "isekai"),
        Tag("Josei", "josei"),
        Tag("Lolicon", "lolicon"),
        Tag("Leader or Politician", "leader_politician"),
        Tag("Mature", "mature"),
        Tag("Magic", "magic"),
        Tag("Mangaka", "mangaka"),
        Tag("Masochist", "masochist"),
        Tag("Monsters", "monsters"),
        Tag("Mecha", "mecha"),
        Tag("Music", "music"),
        Tag("Medical", "medical"),
        Tag("Misunderstands", "misunderstands"),
        Tag("OneShot", "oneshot"),
        Tag("Public figure", "public figure"),
        Tag("Psychological", "psychological"),
        Tag("Powerful Lead Character", "powerful"),
        Tag("Rushed ending", "rushed end"),
        Tag("Revenge", "revenge"),
        Tag("Reverse Harem", "reverse_harem"),
        Tag("Sadist", "sadist"),
        Tag("Seinen", "seinen"),
        Tag("Shotacon", "shotacon"),
        Tag("Secret Crush", "secret_crush"),
        Tag("Secret Relationship", "secret_relationship"),
        Tag("Smart MC", "smart_mc"),
        Tag("Sports", "sports"),
        Tag("Smut", "smut"),
        Tag("Tragedy", "tragedy"),
        Tag("Tomboy", "tomboy"),
        Tag("Triangles", "triangles"),
        Tag("Unusual Pupils", "unusual_pupils"),
        Tag("Vampires", "vampires"),
        Tag("Webtoon", "webtoon"),
        Tag("Work", "work"),
        Tag("Zombies", "zombies"),
        Tag("4-Koma", "4koma"),
        Tag("Manga", "manga"),
        Tag("Manhwa", "manhwa"),
        Tag("Manhua", "manhua"),
    )

class TagFilter(
    values: List<Tag> = tags,
) : Filter.Group<Tag>("Tag Match", values) {
    override fun toString() =
        state.filter { it.state }.joinToString(";").ifEmpty { "all;" }
}

private val states: Array<String>
    get() = arrayOf("ALL", "Completed", "Ongoing")

class StateFilter(
    values: Array<String> = states,
) : Filter.Select<String>("State", values) {
    private val ids = arrayOf("all", "complete", "ongoing")

    override fun toString() = ids[state]
}

private val orders: Array<String>
    get() = arrayOf(
        "A~Z",
        "Z~A",
        "Newest",
        "Oldest",
        "Most Liked",
        "Most Viewed",
        "Most Favourite",
    )

class OrderFilter(
    values: Array<String> = orders,
) : Filter.Select<String>("Order By", values) {
    private val ids = arrayOf(
        "az",
        "za",
        "newest",
        "oldest",
        "liked",
        "viewed",
        "fav",
    )

    override fun toString() = ids[state]
}
