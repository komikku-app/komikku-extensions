package eu.kanade.tachiyomi.extension.en.manta

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit.MILLISECONDS as MS

private val isoDate by lazy {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT)
}

private inline val String?.timestamp: Long
    get() = this?.substringBefore('.')?.let(isoDate::parse)?.time ?: 0L

@Serializable
data class Series<T : Any>(
    val data: T,
    val id: Int,
    val image: Cover,
    val episodes: List<Episode>? = null,
) {
    override fun toString() = data.toString()
}

@Serializable
data class Title(private val title: Name) {
    override fun toString() = title.toString()
}

@Serializable
data class Details(
    val tags: List<Tag>,
    val isCompleted: Boolean? = null,
    private val description: Description,
    private val creators: List<Creator>,
) {
    val artists by lazy {
        creators.filter { it.role == "Illustration" }
    }

    val authors by lazy {
        creators.filter { it.role != "Illustration" }.ifEmpty { creators }
    }

    override fun toString() = description.toString()
}

@Serializable
data class Episode(
    val id: Int,
    val ord: Int,
    val data: Data?,
    private val createdAt: String,
    val cutImages: List<Image>? = null,
) {
    val timestamp: Long
        get() = createdAt.timestamp

    val isLocked: Boolean
        get() = timeTillFree > 0

    val waitingTime: String
        get() = when (val days = MS.toDays(timeTillFree)) {
            0L -> "later today"
            1L -> "tomorrow"
            else -> "in $days days"
        }

    private val timeTillFree by lazy {
        data?.freeAt.timestamp - System.currentTimeMillis()
    }

    override fun toString() = buildString {
        append(data?.title ?: "Episode $ord")
        if (isLocked) append(" \uD83D\uDD12")
    }
}

@Serializable
data class Data(
    val title: String? = null,
    val freeAt: String? = null,
)

@Serializable
data class Creator(
    private val name: String,
    val role: String,
) {
    override fun toString() = name
}

@Serializable
data class Description(
    private val long: String,
    private val short: String,
) {
    override fun toString() = "$short\n\n$long"
}

@Serializable
data class Cover(private val `1280x1840_480`: Image) {
    override fun toString() = `1280x1840_480`.toString()
}

@Serializable
data class Image(private val downloadUrl: String) {
    override fun toString() = downloadUrl
}

@Serializable
data class Tag(private val name: Name) {
    override fun toString() = name.toString()
}

@Serializable
data class Name(private val en: String) {
    override fun toString() = en
}

@Serializable
data class Status(
    private val description: String,
    private val message: String,
) {
    override fun toString() = "$description: $message"
}
