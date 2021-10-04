package eu.kanade.tachiyomi.extension.en.hentairead

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class Hentairead : Madara("HentaiRead", "https://hentairead.com", "en", dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US))
