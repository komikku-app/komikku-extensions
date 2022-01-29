package eu.kanade.tachiyomi.extension.id.manhwaid

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class ManhwaID : Madara("ManhwaID", "https://manhwaid.org", "id", SimpleDateFormat("MMMMM dd, yyyy", Locale("en", "US")))
