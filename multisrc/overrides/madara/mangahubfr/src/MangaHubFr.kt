package eu.kanade.tachiyomi.extension.fr.mangahubfr

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class MangaHubFr : Madara("MangaHub.fr", "https://mangahub.fr", "fr", dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.FRENCH))
