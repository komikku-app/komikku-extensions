package eu.kanade.tachiyomi.extension.es.yugenmangas

import android.app.Application
import android.content.SharedPreferences
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.multisrc.heancms.Genre
import eu.kanade.tachiyomi.multisrc.heancms.HeanCms
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.ConfigurableSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class YugenMangas :
    HeanCms(
        "YugenMangas",
        "https://yugenmangas.net",
        "es",
        "https://api.yugenmangas.net",
    ),
    ConfigurableSource {

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    // Site changed from Madara to HeanCms.
    override val versionId = 2

    override val fetchAllTitlesStrategy = when (getfetchAllStrategyPref()) {
        "all" -> FetchAllStrategy.SEARCH_ALL
        "each" -> FetchAllStrategy.SEARCH_EACH
        else -> FetchAllStrategy.NONE
    }

    override val client = super.client.newBuilder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .rateLimit(2, 3)
        .build()

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

    private fun getfetchAllStrategyPref(): String? {
        return preferences.getString(PREF_FETCH_ALL_STRATEGY_KEY, PREF_FETCH_ALL_STRATEGY_DEFAULT)
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val fetchAllStrategyPreference = ListPreference(screen.context).apply {
            key = PREF_FETCH_ALL_STRATEGY_KEY
            title = PREF_FETCH_ALL_STRATEGY_TITLE
            summary = PREF_FETCH_ALL_STRATEGY_SUMMARY
            entries = PREF_FETCH_ALL_STRATEGY_ENTRIES
            entryValues = PREF_FETCH_ALL_STRATEGY_VALUES
            setDefaultValue(PREF_FETCH_ALL_STRATEGY_DEFAULT)

            setOnPreferenceChangeListener { _, newValue ->
                Toast.makeText(screen.context, RESTART_MESSAGE, Toast.LENGTH_LONG).show()
                true
            }
        }

        screen.addPreference(fetchAllStrategyPreference)
    }

    companion object {
        const val PREF_FETCH_ALL_STRATEGY_KEY = "prefFetchAllStrategy"
        const val PREF_FETCH_ALL_STRATEGY_TITLE = "Método de búsqueda"
        const val PREF_FETCH_ALL_STRATEGY_SUMMARY = "Global: Busca las URLs de todas las series al iniciar la aplicación, lento pero más estable.\n" +
            "Individual: Busca la URL de la serie al actualizar, rápido pero puede fallar.\n" +
            "Ninguno: Usa la URL con la que fue agregado, tendrá que migrar si la URL cambia.\n" +
            "Valor actual: %s"

        val PREF_FETCH_ALL_STRATEGY_ENTRIES = arrayOf("Ninguno", "Individual", "Global")
        val PREF_FETCH_ALL_STRATEGY_VALUES = arrayOf("off", "each", "all")

        const val PREF_FETCH_ALL_STRATEGY_DEFAULT = "off"

        const val RESTART_MESSAGE = "Reinicie la aplicación para que los cambios surtan efecto."
    }
}
