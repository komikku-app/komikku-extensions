package eu.kanade.tachiyomi.extension.fr.voircomic

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class VoirComic : Madara("VoirComic", "https://voircomic.com", "fr", dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.FRANCE))
