package eu.kanade.tachiyomi.extension.tr.noxsubs

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import java.text.SimpleDateFormat
import java.util.Locale

class NoxSubs : MangaThemesia("NoxSubs", "https://noxsubs.com", "tr", dateFormat = SimpleDateFormat("MMM d, yyyy", Locale("tr")))
