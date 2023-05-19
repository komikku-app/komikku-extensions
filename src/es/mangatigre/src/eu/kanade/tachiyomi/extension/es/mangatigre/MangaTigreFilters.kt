package eu.kanade.tachiyomi.extension.es.mangatigre

import eu.kanade.tachiyomi.source.model.Filter

class Type(name: String, val id: String) : Filter.CheckBox(name)
class TypeFilter(values: List<Type>) : Filter.Group<Type>("Tipos", values)

class Status(name: String, val id: String) : Filter.CheckBox(name)
class StatusFilter(values: List<Status>) : Filter.Group<Status>("Estado", values)

class Demographic(name: String, val id: String) : Filter.CheckBox(name)
class DemographicFilter(values: List<Demographic>) : Filter.Group<Demographic>("Demografía", values)

class Content(name: String, val id: String) : Filter.CheckBox(name)
class ContentFilter(values: List<Content>) : Filter.Group<Content>("Contenido", values)

class Format(name: String, val id: String) : Filter.CheckBox(name)
class FormatFilter(values: List<Format>) : Filter.Group<Format>("Formato", values)

class Genre(name: String, val id: String) : Filter.CheckBox(name)
class GenreFilter(values: List<Genre>) : Filter.Group<Genre>("Géneros", values)

class Theme(name: String, val id: String) : Filter.CheckBox(name)
class ThemeFilter(values: List<Theme>) : Filter.Group<Theme>("Temas", values)

open class UriPartFilter(displayName: String, val vals: Array<Pair<String, String>>) :
    Filter.Select<String>(displayName, vals.map { it.first }.toTypedArray()) {
    fun toUriPart() = vals[state].second
}

class OrderFilter() : UriPartFilter(
    "Ordenar por",
    arrayOf(
        Pair("Alfabético", "name"),
        Pair("Vistas", "views"),
        Pair("Fecha Estreno", "date"),
    ),
)

fun getFilterTypeList() = listOf(
    Type("Manga", "1"),
    Type("Manhwa", "2"),
    Type("Manhua", "3"),
)

fun getFilterStatusList() = listOf(
    Status("En Marcha", "1"),
    Status("Terminado", "2"),
    Status("Detenido", "3"),
    Status("Pausado", "4"),
)

fun getFilterDemographicList() = listOf(
    Demographic("Shonen", "1"),
    Demographic("Seinen", "2"),
    Demographic("Shojo", "3"),
    Demographic("Josei", "4"),
)

fun getFilterContentList() = listOf(
    Content("Ecchi", "1"),
    Content("Gore", "2"),
    Content("Smut", "3"),
    Content("Violencia Sexual", "4"),
)

fun getFilterFormatList() = listOf(
    Format("Adaptación", "14"),
    Format("Antalogía", "9"),
    Format("Color Completo", "18"),
    Format("Coloreado Oficial", "19"),
    Format("Coloreado Por Fan", "15"),
    Format("Creado Por Usuario", "20"),
    Format("Delincuencia", "16"),
    Format("Doujinshi", "10"),
    Format("Galardonado", "13"),
    Format("One Shot", "11"),
    Format("Tira Larga", "17"),
    Format("Webcomic", "12"),
    Format("YonKoma", "8"),
)

fun getFilterGenreList() = listOf(
    Genre("Acción", "49"),
    Genre("Aventura", "50"),
    Genre("Boys Love", "75"),
    Genre("Chicas Mágicas", "73"),
    Genre("Ciencia-Ficción", "64"),
    Genre("Comedia", "51"),
    Genre("Crimen", "52"),
    Genre("Deporte", "65"),
    Genre("Drama", "53"),
    Genre("Fantasía", "54"),
    Genre("Filosófico", "61"),
    Genre("Girls Love", "76"),
    Genre("Guerra", "74"),
    Genre("Histórico", "55"),
    Genre("Horror", "56"),
    Genre("Isekai", "57"),
    Genre("Mecha", "58"),
    Genre("Médica", "59"),
    Genre("Misterio", "60"),
    Genre("Psicológico", "62"),
    Genre("Recuentos De La Vida", "72"),
    Genre("Romance", "63"),
    Genre("Superhéroe", "66"),
    Genre("Thriller", "67"),
    Genre("Tragedia", "68"),
    Genre("Wuxia", "69"),
    Genre("Yaoi", "70"),
    Genre("Yuri", "71"),
)

fun getFilterThemeList() = listOf(
    Theme("Animales", "52"),
    Theme("Apocalíptico", "50"),
    Theme("Artes Marciales", "60"),
    Theme("Chicas Monstruo", "77"),
    Theme("Cocinando", "53"),
    Theme("Crossdressing", "79"),
    Theme("Delincuencia", "78"),
    Theme("Demonios", "54"),
    Theme("Extranjeros", "51"),
    Theme("Fantasma", "55"),
    Theme("Género Bender", "81"),
    Theme("Gyaru", "56"),
    Theme("Harén", "57"),
    Theme("Incesto", "58"),
    Theme("Lolicon", "59"),
    Theme("Mafia", "64"),
    Theme("Magia", "65"),
    Theme("Militar", "61"),
    Theme("Monstruos", "62"),
    Theme("Música", "63"),
    Theme("Ninja", "66"),
    Theme("Policía", "67"),
    Theme("Realidad Virtual", "74"),
    Theme("Reencarnación", "68"),
    Theme("Samurái", "73"),
    Theme("Shotacon", "71"),
    Theme("Sobrenatural", "69"),
    Theme("Superpoderes", "82"),
    Theme("Supervivencia", "72"),
    Theme("Vampiros", "75"),
    Theme("Vida Escolar", "70"),
    Theme("Videojuegos", "80"),
    Theme("Zombis", "76"),
)
