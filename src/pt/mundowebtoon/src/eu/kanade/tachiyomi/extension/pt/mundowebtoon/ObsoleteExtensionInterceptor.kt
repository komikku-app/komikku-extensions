package eu.kanade.tachiyomi.extension.pt.mundowebtoon

import eu.kanade.tachiyomi.network.GET
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.io.IOException

class ObsoleteExtensionInterceptor : Interceptor {

    private val json: Json by injectLazy()
    private var isObsolete: Boolean? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        if (isObsolete == null) {
            val extRepoResponse = chain.proceed(GET(REPO_URL))
            val extRepo = json.decodeFromString<List<ExtensionJsonObject>>(extRepoResponse.body.string())

            isObsolete = !extRepo.any { ext ->
                ext.pkg == this.javaClass.`package`?.name && ext.lang == "pt-BR"
            }
        }

        if (isObsolete == true) {
            throw IOException("Extensão obsoleta. Desinstale e migre para outras fontes.")
        }

        return chain.proceed(chain.request())
    }

    @Serializable
    private data class ExtensionJsonObject(
        val pkg: String,
        val lang: String,
    )

    companion object {
        private const val REPO_URL = "https://raw.githubusercontent.com/tachiyomiorg/tachiyomi-extensions/repo/index.min.json"
    }
}
