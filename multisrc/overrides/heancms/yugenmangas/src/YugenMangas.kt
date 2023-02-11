package eu.kanade.tachiyomi.extension.es.yugenmangas

import eu.kanade.tachiyomi.multisrc.heancms.Genre
import eu.kanade.tachiyomi.multisrc.heancms.HeanCms
import java.text.SimpleDateFormat
import java.util.TimeZone

class YugenMangas : HeanCms(
    "YugenMangas",
    "https://yugenmangas.com",
    "es",
    "https://api.yugenmangas.com",
) {

    // Site changed from Madara to HeanCms.
    override val versionId = 2

    override val coverPath: String = ""

    override val dateFormat: SimpleDateFormat = super.dateFormat.apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun getGenreList(): List<Genre> = listOf(
        Genre("+18", 1),
        Genre("Acción", 36),
        Genre("Adulto", 38),
        Genre("Apocalíptico", 3),
        Genre("Artes marciales (1)", 16),
        Genre("Artes marciales (2)", 37),
        Genre("Aventura", 2),
        Genre("Boys Love", 4),
        Genre("Ciencia ficción", 39),
        Genre("Comedia", 5),
        Genre("Demonios", 6),
        Genre("Deporte", 26),
        Genre("Drama", 7),
        Genre("Ecchi", 8),
        Genre("Familia", 9),
        Genre("Fantasía", 10),
        Genre("Girls Love", 11),
        Genre("Gore", 12),
        Genre("Harem", 13),
        Genre("Harem inverso", 14),
        Genre("Histórico", 48),
        Genre("Horror", 41),
        Genre("Isekai", 40),
        Genre("Josei", 15),
        Genre("Maduro", 42),
        Genre("Magia", 17),
        Genre("MangoScan", 35),
        Genre("Mecha", 18),
        Genre("Militar", 19),
        Genre("Misterio", 20),
        Genre("Psicológico", 21),
        Genre("Realidad virtual", 46),
        Genre("Recuentos de la vida", 25),
        Genre("Reencarnación", 22),
        Genre("Regresion", 23),
        Genre("Romance", 24),
        Genre("Seinen", 27),
        Genre("Shonen", 28),
        Genre("Shoujo", 29),
        Genre("Sistema", 45),
        Genre("Smut", 30),
        Genre("Supernatural", 31),
        Genre("Supervivencia", 32),
        Genre("Tragedia", 33),
        Genre("Transmigración", 34),
        Genre("Vida Escolar", 47),
        Genre("Yaoi", 43),
        Genre("Yuri", 44),
    )
}
