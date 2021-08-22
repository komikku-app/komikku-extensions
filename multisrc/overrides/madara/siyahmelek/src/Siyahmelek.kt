package eu.kanade.tachiyomi.extension.tr.siyahmelek

import eu.kanade.tachiyomi.annotations.Nsfw
import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

@Nsfw
class Siyahmelek : Madara("Siyahmelek", "https://siyahmelek.com", "tr", SimpleDateFormat("dd MMM yyyy", Locale("tr")))
