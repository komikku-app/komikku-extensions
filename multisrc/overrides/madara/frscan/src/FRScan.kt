package eu.kanade.tachiyomi.extension.fr.frscan

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class FRScan : Madara("FR-Scan", "https://fr-scan.com", "fr", dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.FRANCE))
