package eu.kanade.tachiyomi.extension.en.xunscans

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class XuNScans : Madara(
    "XuN Scans",
    "https://xunscans.xyz",
    "en",
    dateFormat = SimpleDateFormat("d MMM yyy", Locale.US)
)
