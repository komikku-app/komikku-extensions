package eu.kanade.tachiyomi.extension.tr.moondaisyscans

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import java.text.SimpleDateFormat
import java.util.Locale

class MoonDaisyScans : MangaThemesia(
    "Moon Daisy Scans",
    "https://moondaisyscans.com",
    "tr",
    dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale("tr")),
) {
    override val seriesAuthorSelector = ".tsinfo .imptdt:contains(Yazar) i"
    override val seriesArtistSelector = ".tsinfo .imptdt:contains(Çizer) i"
    override val seriesStatusSelector = ".tsinfo .imptdt:contains(Durum) i"
}
