package eu.kanade.tachiyomi.extension.fr.japscan

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import android.util.Log
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class Japscan : ConfigurableSource, ParsedHttpSource() {

    override val id: Long = 11

    override val name = "Japscan"

    override val baseUrl = "https://www.japscan.lol"

    override val lang = "fr"

    override val supportsLatest = true

    private val json: Json by injectLazy()

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimit(1, 2)
        .build()

    companion object {
        val dateFormat by lazy {
            SimpleDateFormat("dd MMM yyyy", Locale.US)
        }
        private const val SHOW_SPOILER_CHAPTERS_Title = "Les chapitres en Anglais ou non traduit sont upload en tant que \" Spoilers \" sur Japscan"
        private const val SHOW_SPOILER_CHAPTERS = "JAPSCAN_SPOILER_CHAPTERS"
        private val prefsEntries = arrayOf("Montrer uniquement les chapitres traduit en Français", "Montrer les chapitres spoiler")
        private val prefsEntryValues = arrayOf("hide", "show")
    }

    private fun chapterListPref() = preferences.getString(SHOW_SPOILER_CHAPTERS, "hide")

    override fun headersBuilder() = super.headersBuilder()
        .add("referer", "$baseUrl/")

    // Popular
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/mangas/", headers)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        pageNumberDoc = document

        val mangas = document.select(popularMangaSelector()).map { element ->
            popularMangaFromElement(element)
        }
        val hasNextPage = false
        return MangasPage(mangas, hasNextPage)
    }

    override fun popularMangaNextPageSelector(): String? = null

    override fun popularMangaSelector() = "#top_mangas_week li"

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("a").first()!!.let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
            manga.thumbnail_url = "$baseUrl/imgs/${it.attr("href").replace(Regex("/$"),".jpg").replace("manga","mangas")}".lowercase(Locale.ROOT)
        }
        return manga
    }

    // Latest
    override fun latestUpdatesRequest(page: Int): Request {
        return GET(baseUrl, headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(latestUpdatesSelector())
            .distinctBy { element -> element.select("a").attr("href") }
            .map { element ->
                latestUpdatesFromElement(element)
            }
        val hasNextPage = false
        return MangasPage(mangas, hasNextPage)
    }

    override fun latestUpdatesNextPageSelector(): String? = null

    override fun latestUpdatesSelector() = "#chapters h3.text-truncate, #chapters_list h3.text-truncate"

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isEmpty()) {
            val uri = Uri.parse(baseUrl).buildUpon()
                .appendPath("mangas")
            filters.forEach { filter ->
                when (filter) {
                    is TextField -> uri.appendPath(((page - 1) + filter.state.toInt()).toString())
                    is PageList -> uri.appendPath(((page - 1) + filter.values[filter.state]).toString())
                    else -> {}
                }
            }
            return GET(uri.toString(), headers)
        } else {
            val formBody = FormBody.Builder()
                .add("search", query)
                .build()
            val searchHeaders = headers.newBuilder()
                .add("X-Requested-With", "XMLHttpRequest")
                .build()

            try {
                val searchRequest = POST("$baseUrl/live-search/", searchHeaders, formBody)
                val searchResponse = client.newCall(searchRequest).execute()

                if (!searchResponse.isSuccessful) {
                    throw Exception("Code ${searchResponse.code} inattendu")
                }

                val jsonResult = json.parseToJsonElement(searchResponse.body.string()).jsonArray

                if (jsonResult.isEmpty()) {
                    Log.d("japscan", "Search not returning anything, using duckduckgo")
                    throw Exception("Pas de données")
                }

                return searchRequest
            } catch (e: Exception) {
                // Fallback to duckduckgo if the search does not return any result
                val uri = Uri.parse("https://duckduckgo.com/lite/").buildUpon()
                    .appendQueryParameter("q", "$query site:$baseUrl/manga/")
                    .appendQueryParameter("kd", "-1")
                return GET(uri.toString(), headers)
            }
        }
    }

    override fun searchMangaNextPageSelector(): String = "li.page-item:last-child:not(li.active),.next_form .navbutton"

    override fun searchMangaSelector(): String = "div.card div.p-2, a.result-link"

    override fun searchMangaParse(response: Response): MangasPage {
        if ("live-search" in response.request.url.toString()) {
            val jsonResult = json.parseToJsonElement(response.body.string()).jsonArray

            val mangaList = jsonResult.map { jsonEl -> searchMangaFromJson(jsonEl.jsonObject) }

            return MangasPage(mangaList, hasNextPage = false)
        }

        return super.searchMangaParse(response)
    }

    override fun searchMangaFromElement(element: Element): SManga {
        return if (element.attr("class") == "result-link") {
            SManga.create().apply {
                title = element.text().substringAfter(" ").substringBefore(" | JapScan")
                setUrlWithoutDomain(element.attr("abs:href"))
            }
        } else {
            SManga.create().apply {
                thumbnail_url = element.select("img").attr("abs:src")
                element.select("p a").let {
                    title = it.text()
                    url = it.attr("href")
                }
            }
        }
    }

    private fun searchMangaFromJson(jsonObj: JsonObject): SManga = SManga.create().apply {
        title = jsonObj["name"]!!.jsonPrimitive.content
        url = jsonObj["url"]!!.jsonPrimitive.content
    }

    override fun mangaDetailsParse(document: Document): SManga {
        val infoElement = document.selectFirst("#main .card-body")!!

        val manga = SManga.create()
        manga.thumbnail_url = infoElement.select("img").attr("abs:src")

        val infoRows = infoElement.select(".row, .d-flex")
        infoRows.select("p").forEach { el ->
            when (el.select("span").text().trim()) {
                "Auteur(s):" -> manga.author = el.text().replace("Auteur(s):", "").trim()
                "Artiste(s):" -> manga.artist = el.text().replace("Artiste(s):", "").trim()
                "Genre(s):" -> manga.genre = el.text().replace("Genre(s):", "").trim()
                "Statut:" -> manga.status = el.text().replace("Statut:", "").trim().let {
                    parseStatus(it)
                }
            }
        }
        manga.description = infoElement.select("div:contains(Synopsis) + p").text().orEmpty()

        return manga
    }

    private fun parseStatus(status: String) = when {
        status.contains("En Cours") -> SManga.ONGOING
        status.contains("Terminé") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    override fun chapterListSelector() = "#chapters_list > div.collapse > div.chapters_list" +
        if (chapterListPref() == "hide") { ":not(:has(.badge:contains(SPOILER),.badge:contains(RAW),.badge:contains(VUS)))" } else { "" }
    // JapScan sometimes uploads some "spoiler preview" chapters, containing 2 or 3 untranslated pictures taken from a raw. Sometimes they also upload full RAWs/US versions and replace them with a translation as soon as available.
    // Those have a span.badge "SPOILER" or "RAW". The additional pseudo selector makes sure to exclude these from the chapter list.

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.selectFirst("a")!!

        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.ownText()
        // Using ownText() doesn't include childs' text, like "VUS" or "RAW" badges, in the chapter name.
        chapter.date_upload = element.selectFirst("span")!!.text().trim().let { parseChapterDate(it) }
        return chapter
    }

    private fun parseChapterDate(date: String): Long {
        return try {
            dateFormat.parse(date)?.time ?: 0
        } catch (e: ParseException) {
            0L
        }
    }

    private fun extractQuotedContent(input: String): List<String> {
        val regex = Regex("'(.*?)'")
        return regex.findAll(input).map { it.groupValues[1] }.toList()
    }

    private fun listJSToKey(jsList: MutableList<String>, offsettab: Int, listKey: List<String>): MutableList<String> {
        for (i in 0 until jsList.size) {
            if (jsList[i].contains("0x")) {
                var decoupeHexa = jsList[i].split("('")[1]
                decoupeHexa = decoupeHexa.split("')")[0]
                var indexkey = Integer.decode(decoupeHexa) - offsettab - 1
                if (indexkey < 0) {
                    indexkey = listKey.size - 1
                }
                jsList[i] = listKey[indexkey]
            }
        }

        return jsList
    }

    override fun pageListParse(document: Document): List<Page> {
        /*
            JapScan stores chapter metadata in a `#data` element, and in the `data-data` attribute.

            This data is scrambled base64, and to unscramble it this code searches in the ZJS for
            two strings of length 62 (base64 minus `+` and `/`), creating a character map.

            Comment le script fonctionne globalement :

                Il garde dans un tableau les morceaux diviser en 3 partie des deux clés string.
                Une partie de ces clé peuvent aussi être en partie en direct dans le concaténage final de la clé, exemple :

                a0_0x1dc175('0x16b') + 'H1M9pwuXgyKmTLJqekNd' + 'P85arn0hFDA2RBOQUZvI' + 'Wi', 'fOu6QZGtyepSkzRsEdba' + a0_0x1dc175('0x132') + a0_0x1dc175('0x112') + 'IX'

                On peut voir que les autres parties des clés comme par exemple : a0_0x1dc175('0x16b') est appelé par une fonction.
                Pour expliquer simplement, cette fonction permet de GET un élément du tableau en fonction du paramètre, le paramètre est une chaine en hexadecimal qui est par la suite
                converti en int.
                    
                //================= Une partie du code JS pour mieux comprendre

                //il enregistre la fonction dans une variable;
                const a0_0x1dc175 = a0_0x4ed0;
                //le premier paramètre de la fonction est l'hexadecimal qu'on lui passe en paramètre, le deuxième est à null tout le temps
                function a0_0x4ed0(_0x39b48a, _0x134ab9) {
                    //cette fonction récupère l'entièrté du tableau de chaine
                    const _0x472ace = a0_0x472a();

                    (le tableau à cette forme : const _0x1ac112 = ['kcolb', 'retpahc-txen#', 'yalpsid', '}\x20gnitcepxE', 'tArahc', 'dedaoLtnetnoCMOD', 'tfeLworrA', 'egnahc',  .... etc )

                    return a0_0x4ed0 = function(_0x4ed09a, _0x4045cf) {
                        //ce calcul consiste à utiliser l'hexadecimal prit en paramètre qui a cette valeur pour exemple : '0x16b'
                        //Cela donne : 0x16b - 0x10b => 363 - 267 = 96
                        //A noter que le 0x10b est en dur dans le code, mais quand le script se reload (environ toutes les 48h actuellement) cette valeur peut changer !
                        _0x4ed09a = _0x4ed09a - 0x10b;
                        //_0x472ace étant le tableau de chaine, ici on utilise maintenant l'index qu'on a généré (96) pour aller chercher l'élément en question
                        let _0x913c35 = _0x472ace[_0x4ed09a];

                        //Donc ici la fonction retourne enfin le bon morceau de clé! En résumé il faut faire un calcul d'offset avec la valeur prit en paramètre et récupérer le résultat dans le tableau
                        return _0x913c35;
                    }
                    ,
                    //appel la fonction juste en haut, c'est juste pour brouiller le suivi visuel
                    a0_0x4ed0(_0x39b48a, _0x134ab9);
                }


                Avec tout ces éléments il est possible de récupérer les informations dans le tableau, mais ça n'est pas tout!
                Le script a mit en place une autre sécurité qu'il faut contourner, les emplacements des chaines dans le tableau ne sont pas bonne à la génération de celui-ci, il y a
                toute une partie du script qui replace correctement les élément dans le tableau avant de faire le calcul que j'ai expliqué précédemment.
                Sans le remettre en ordre le tableau va juste nous renvoyer des valeurs qui ne correspondent pas aux morceaux de clé !

                Comment le script fonctionne pour remplacer les éléments :

                ici des valeurs sont déclaré, c'est le même format que pour get les éléments du tableau, et ces effectivements aussi leurs utilités.
                Pour remettre au bon emplacement les chaines il va falloir faire des tests sur les emplacements.

                const a0_0x14d6c4 = {
                    _0x24dcbe: '0x172', //ici ce sont des valeurs utilisé par la suite, c'est seulement pour brouiller la lecture, exemple : 0x1 * (-parseInt(_0x3d53a2(a0_0x14d6c4._0x14050e))  devient => 0x1 * (-parseInt(_0x3d53a2('0x172'))
                    _0x14050e: '0x15b',
                    _0x3ece33: '0x147',
                    _0x2cd65f: '0x129',
                    _0x15deac: '0x151'
                }
                  , _0x3d53a2 = a0_0x4ed0
                  , _0x20d103 = _0x5d612f();
                //boucle jusqu'à ce que le tableau soit dans l'ordre
                while (!![]) {
                    try {
                        //on récupère à des index fixe les éléments dans le tableau, ces index sont défini de la même façon que les clé, c'est à dire avec
                        //un index en hexadecimal et un petit calcul derrière
                        //ensuite chaque élément à un parseInt, c'est à dire que tout les éléments sélectionné dans ces index doivent avoir des chiffres dans leurs chaines pour que le calcul fonctionne correctement
                        //la plupart du temps ce calcul renvoie "NaN", car il y a peu de int dans le calcul (c'est important car je m'en sers dans mon algo par la suite)
                        const _0x474f86 = -parseInt(_0x3d53a2(a0_0x14d6c4._0x24dcbe)) / 0x1 * (-parseInt(_0x3d53a2(a0_0x14d6c4._0x14050e)) / 0x2) + -parseInt(_0x3d53a2('0x12b')) / 0x3 + parseInt(_0x3d53a2('0x160')) / 0x4 * (-parseInt(_0x3d53a2('0x14a')) / 0x5) + parseInt(_0x3d53a2('0x14c')) / 0x6 + -parseInt(_0x3d53a2('0x14f')) / 0x7 + -parseInt(_0x3d53a2(a0_0x14d6c4._0x3ece33)) / 0x8 + -parseInt(_0x3d53a2(a0_0x14d6c4._0x2cd65f)) / 0x9 * (-parseInt(_0x3d53a2(a0_0x14d6c4._0x15deac)) / 0xa);

                        //ici se trouve le check permettant de savoir si l'emplacement du tableau est bon.
                        //_0x1fbb37 est une valeur fixe déclaré précemment dans le script, par exemple il peut avoir la valeur : 99005
                        //_0x474f86 est la valeur qui va changer selon les emplacements des éléments dans le tableau
                        //quand le calcul est bon, _0x474f86 a forcément la valeur de _0x1fbb37, cela signifie que le tableau est dans le bon ordre et que le script peut continuer
                        if (_0x474f86 === _0x1fbb37)
                            break;
                        else
                            //Ici est une partie TRES IMPORTANTE du code, lorsque la valeur n'est pas bonne, donc quand le tableau
                            //n'est pas trié correctement, les éléments dans le tableau bascule vers l'arrière, c'est à dire que par exemple l'élement en 3eme position passe en 2eme position
                            //l'élement en 4eme position passe en 3 etc
                            //
                            //Tout ceci provoque un décalage constant jusqu'à ce que le tableau tombe sur la bonne combinaison
                            _0x20d103['push'](_0x20d103['shift']());
                    } catch (_0x383e51) {
                        //lui non plus
                        _0x20d103['push'](_0x20d103['shift']());
                    }
                }

                Voilà ! Une fois qu'on a comprit tout ça, on peu aisément comprendre la suite du code.

         */
        val zjsurl = document.getElementsByTag("script").first {
            it.attr("src").contains("zjs", ignoreCase = true)
        }.attr("src")
        Log.d("japscan", "ZJS at $zjsurl")

        //on récupère le script JS permettant de déchiffrer le base64
        val zjs = client.newCall(GET(baseUrl + zjsurl, headers)).execute().body.string()

        /*
            On récupère le tableau qui contient toute les chaines de caractère
            (Pour rappel : le tableau à cette forme : const _0x1ac112 = ['kcolb', 'retpahc-txen#', 'yalpsid', '}\x20gnitcepxE', 'tArahc', 'dedaoLtnetnoCMOD', 'tfeLworrA', 'egnahc',  .... etc )
         */
        var tabKey = "'" + zjs.split("=['")[1]
        tabKey = tabKey.split("];")[0]
        val listKey = tabKey.split("','").toMutableList()

        /*
            On récupère l'offset permettant le calcul de l'index du tableau quand on appel la fonction qui récupère un élément dans le tableau :
             Exemple si dans le script on avait :

             _0x4ed09a = _0x4ed09a - 0x10b;

             L'objectif est de récupérer 0x10b pour pouvoir effectuer ce calcul manuellement
        */
        var decoupeOffset = zjs.split("-0x")[1]
        decoupeOffset = "0x" + decoupeOffset.split(";")[0]
        // on converti directement en int pour le calcul
        val offsettab = Integer.decode(decoupeOffset)


        /*
            Ici le but est de récupérer toute cette partie de js :

            while (!![]) {
        try {
            const _0x474f86 = -parseInt(_0x3d53a2(a0_0x14d6c4._0x24dcbe)) / 0x1 * (-parseInt(_0x3d53a2(a0_0x14d6c4._0x14050e)) / 0x2) + -parseInt(_0x3d53a2('0x12b')) / 0x3 + parseInt(_0x3d53a2('0x160')) / 0x4 * (-parseInt(_0x3d53a2('0x14a')) / 0x5) + parseInt(_0x3d53a2('0x14c')) / 0x6 + -parseInt(_0x3d53a2('0x14f')) / 0x7 + -parseInt(_0x3d53a2(a0_0x14d6c4._0x3ece33)) / 0x8 + -parseInt(_0x3d53a2(a0_0x14d6c4._0x2cd65f)) / 0x9 * (-parseInt(_0x3d53a2(a0_0x14d6c4._0x15deac)) / 0xa);
            if (_0x474f86 === _0x1fbb37)
                break;
            else
        */
        var decoupeFuncOrder = zjs.split("while(!![])")[1]
        decoupeFuncOrder = decoupeFuncOrder.split("if")[0]

        /*
           Au lieu de parse les int comme dans le script JS, ici je récupère seulement les offsets entre quote, donc dans listKeyOrder il y a ces valeurs si je reprend l'exemple juste au dessus : 0x12b, 0x160, 0x14a, 0x14c
        */
        val listKeyOrder = extractQuotedContent(decoupeFuncOrder).toMutableList()

        if (listKeyOrder.size < 3) {
            throw Exception("L'ordre des clés n'a pas pu être déterminé")
        }

        /*
           Ici je boucle sur les offsets que j'ai récupéré et je regarde si ils contiennent des chiffres, si ils contiennent tous des chiffres aux même moment.
           Si ils ne contiennent pas tous des chiffres alors je fais un décalage dans le tableau de la même manière que le fais le JS.
           Si ils ont tous des chiffres, alors je considère que le tableau est correctement trié et je passe à la suite.

           Pour faire bien à 100% il faudrait aussi récupérer la valeur qui valide le bon trie et simulé l'entièrté du calcul des ParseInt, cependant vu que les informations
           proviennent de scraping il y a une marge d'erreur et ça serait beaucoup trop imprécis pour être plus fiable que cette méthode.
           Elle n'est donc pas infaillible, mais 95% du temps cela va fonctionner.
        */
        var goodorder = false
        //je boucle sur toute les positions possible du tableau, si aucune ne fonctionne c'est qu'il y a un problème
        for (i in 0 until listKey.size) {
            //je vérifie sur chacun des offsets récupéré la valeur dans le tableau
            for (z in 0 until listKeyOrder.size) {
                if (listKey[Integer.decode(listKeyOrder[z]) - offsettab - 1].contains("[0-9]".toRegex())) {
                    goodorder = true
                } else {
                    goodorder = false
                    break
                }
            }

            //Si tout les éléments contiennent au moins un chiffre alors c'est bon on sort de la boucle
            if (goodorder) {
                break
            }

            //je bascule les éléments du tableau vers l'arrière
            val firstElement = listKey.removeAt(0)
            listKey.add(firstElement)
        }

        //pas trouver de bonne position pour le tableau
        if (!goodorder) {
            throw Exception("L'ordre des clés n'a pas pu être déterminé")
        }

        /*
            Ici l'objectif est de récupéré ceci :  a0_0x1dc175('0x16b') + 'H1M9pwuXgyKmTLJqekNd' + 'P85arn0hFDA2RBOQUZvI' + 'Wi', 'fOu6QZGtyepSkzRsEdba' + a0_0x1dc175('0x132') + a0_0x1dc175('0x112') + 'IX'
            C'est l'assemblage des morceaux de clé.
            Pour se faire je me base sur un principe du script qui est que cette information se trouve dans les paramètres d'une fonction, généralement tout à la fin du script, et qu'elle se trouve dans avant ceci /[A-Z0-9]/gi :

                , 'ecalper', /[A-Z0-9]/gi, a0_0x1dc175('0x16b') + 'H1M9pwuXgyKmTLJqekNd' + 'P85arn0hFDA2RBOQUZvI' + 'Wi', 'fOu6QZGtyepSkzRsEdba' + a0_0x1dc175('0x13

                Après ce paramètre il y a toujours l'assemblage des deux clés, puisque notre tableau est dans le bon ordre maintenant, on peut récupérer ses morceaux de clé
                et les assembler en faisant en sorte de bien parse séparemment toute les parties.

        */
        val zjscalc = zjs.split("/[A-Z0-9]/gi,")[1]

        //je découpe la première clé et la met dans un tableau
        //ensuite avec la fonction listJSToKey je remplace les clé qui se trouve dans le tableau de chaine par les vrai chaine récupéré aux bon offset
        val calc1 = zjscalc.split(",")[0]
        var calc1tab = calc1.split("+").toMutableList()
        calc1tab = listJSToKey(calc1tab, offsettab, listKey)

        //pareil ici
        val calc2 = zjscalc.split(",")[1]
        var calc2tab = calc2.split("+").toMutableList()
        calc2tab = listJSToKey(calc2tab, offsettab, listKey)

        //j'assemble les tableaux pour reformer la chaine
        var key1 = calc1tab.joinToString("")
        var key2 = calc2tab.joinToString("")

        //on clean les chaines
        key1 = key1.replace("'", "")
        key2 = key2.replace("'", "")
        key1 = key1.replace(" ", "")
        key2 = key2.replace(" ", "")

        // Once we found the 8 strings, assuming they are always in the same order
        // Since Japscan reverse the char order, reverse the strings
        val stringLookupTables = listOf(
            key1.reversed(),
            key2.reversed(),
        )

        val scrambledData = document.getElementById("data")!!.attr("data-data")

        for (i in 0..1) {
            Log.d("japscan", "descramble attempt $i")
            val otherIndice = if (i == 0) 1 else 0
            val lookupTable = stringLookupTables[i].zip(stringLookupTables[otherIndice]).toMap()
            try {
                val unscrambledData = scrambledData.map { lookupTable[it] ?: it }.joinToString("")
                if (!unscrambledData.startsWith("ey")) {
                    // `ey` is the Base64 representation of a curly bracket. Since we're expecting a
                    // JSON object, we're counting this attempt as failed if it doesn't start with a
                    // curly bracket.
                    continue
                }
                val decoded = Base64.decode(unscrambledData, Base64.DEFAULT).toString(Charsets.UTF_8)

                val data = json.parseToJsonElement(decoded).jsonObject

                return data["imagesLink"]!!.jsonArray.mapIndexed { idx, it ->
                    Page(idx, imageUrl = it.jsonPrimitive.content)
                }
            } catch (_: Throwable) {}
        }

        throw Exception("Les deux tentatives de désembrouillage ont échoué")
    }

    override fun imageUrlParse(document: Document): String = ""

    // Filters
    private class TextField(name: String) : Filter.Text(name)

    private class PageList(pages: Array<Int>) : Filter.Select<Int>("Page #", arrayOf(0, *pages))

    override fun getFilterList(): FilterList {
        val totalPages = pageNumberDoc?.select("li.page-item:last-child a")?.text()
        val pagelist = mutableListOf<Int>()
        return if (!totalPages.isNullOrEmpty()) {
            for (i in 0 until totalPages.toInt()) {
                pagelist.add(i + 1)
            }
            FilterList(
                Filter.Header("Page alphabétique"),
                PageList(pagelist.toTypedArray()),
            )
        } else {
            FilterList(
                Filter.Header("Page alphabétique"),
                TextField("Page #"),
                Filter.Header("Appuyez sur reset pour la liste"),
            )
        }
    }

    private var pageNumberDoc: Document? = null

    // Prefs
    override fun setupPreferenceScreen(screen: androidx.preference.PreferenceScreen) {
        val chapterListPref = androidx.preference.ListPreference(screen.context).apply {
            key = SHOW_SPOILER_CHAPTERS_Title
            title = SHOW_SPOILER_CHAPTERS_Title
            entries = prefsEntries
            entryValues = prefsEntryValues
            summary = "%s"

            setOnPreferenceChangeListener { _, newValue ->
                val selected = newValue as String
                val index = this.findIndexOfValue(selected)
                val entry = entryValues[index] as String
                preferences.edit().putString(SHOW_SPOILER_CHAPTERS, entry).commit()
            }
        }
        screen.addPreference(chapterListPref)
    }
}
