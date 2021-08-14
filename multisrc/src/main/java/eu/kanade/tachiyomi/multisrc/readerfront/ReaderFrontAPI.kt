package eu.kanade.tachiyomi.multisrc.readerfront

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Serializable
data class Work(
    private val name: String,
    val stub: String,
    val thumbnail_path: String,
    val adult: Boolean? = null,
    val type: String? = null,
    val licensed: Boolean? = null,
    val status_name: String? = null,
    val description: String? = null,
    val demographic_name: String? = null,
    val genres: List<Name>? = null,
    private val people_works: List<People>? = null
) {
    @Transient
    val authors = people_works?.filter { it.role == 1 }

    @Transient
    val artists = people_works?.filter { it.role == 2 }

    override fun toString() = name
}

@Serializable
data class Chapter(
    val id: Int,
    private val chapter: Int,
    private val subchapter: Int,
    private val volume: Int,
    private val name: String,
    private val releaseDate: String
) {
    @Transient
    val number = "$chapter.$subchapter".toFloat()

    @Transient
    val timestamp = dateFormat.parse(releaseDate)?.time ?: 0L

    override fun toString() = buildString {
        if (volume > 0) append("Volume $volume ")
        if (number > 0) append("Chapter ${decimalFormat.format(number)}: ")
        append(name)
    }

    companion object {
        private val decimalFormat = DecimalFormat("#.##")
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
    }
}

@Serializable
data class PageList(
    private val uniqid: String,
    private val work: Uniqid,
    private val pages: List<Page>
) : Iterable<Page> by pages {
    /** Get the path of a page in the list. */
    fun path(page: Page) = "/works/$work/$this/$page"

    override fun toString() = uniqid
}

@Serializable
data class Page(private val filename: String, val width: Int) {
    override fun toString() = filename
}

@Serializable
data class Uniqid(private val uniqid: String) {
    override fun toString() = uniqid
}

@Serializable
data class People(val role: Int, private val people: Name) {
    override fun toString() = people.toString()
}

@Serializable
data class Name(private val name: String) : CharSequence by name {
    override fun toString() = name
}
