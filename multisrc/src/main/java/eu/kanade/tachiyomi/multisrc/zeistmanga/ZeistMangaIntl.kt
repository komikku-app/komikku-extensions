package eu.kanade.tachiyomi.multisrc.zeistmanga

class ZeistMangaIntl(lang: String) {

    val availableLang: String = if (lang in AVAILABLE_LANGS) lang else ENGLISH

    // Status Filter

    val statusFilterTitle: String = when (availableLang) {
        SPANISH -> "Estado"
        else -> "Status"
    }

    val statusAll: String = when (availableLang) {
        SPANISH -> "Todos"
        else -> "All"
    }

    val statusOngoing: String = when (availableLang) {
        SPANISH -> "En curso"
        else -> "Ongoing"
    }

    val statusCompleted: String = when (availableLang) {
        SPANISH -> "Completado"
        else -> "Completed"
    }

    val statusDropped: String = when (availableLang) {
        SPANISH -> "Abandonada"
        else -> "Dropped"
    }

    val statusUpcoming: String = when (availableLang) {
        SPANISH -> "Próximos"
        else -> "Upcoming"
    }

    // Type Filter

    val typeFilterTitle: String = when (availableLang) {
        SPANISH -> "Tipo"
        else -> "Type"
    }

    val typeAll: String = when (availableLang) {
        SPANISH -> "Todos"
        else -> "All"
    }

    val typeManga: String = when (availableLang) {
        SPANISH -> "Manga"
        else -> "Manga"
    }

    val typeManhua: String = when (availableLang) {
        SPANISH -> "Manhua"
        else -> "Manhua"
    }

    val typeManhwa: String = when (availableLang) {
        SPANISH -> "Manhwa"
        else -> "Manhwa"
    }

    val typeNovel: String = when (availableLang) {
        SPANISH -> "Novela"
        else -> "Novel"
    }

    // Language Filter

    val languageFilterTitle: String = when (availableLang) {
        SPANISH -> "Idioma"
        else -> "Language"
    }

    val languageAll: String = when (availableLang) {
        SPANISH -> "Todos"
        else -> "All"
    }

    // Genre Filter

    val genreFilterTitle: String = when (availableLang) {
        SPANISH -> "Género"
        else -> "Genre"
    }

    // Extra
    val filterWarning: String = when (availableLang) {
        SPANISH -> "Los filtros serán ignorados si la búsqueda no está vacía."
        else -> "Filters will be ignored if the search is not empty."
    }

    companion object {
        const val ENGLISH = "en"
        const val SPANISH = "es"

        val AVAILABLE_LANGS = arrayOf(ENGLISH, SPANISH)
    }
}
