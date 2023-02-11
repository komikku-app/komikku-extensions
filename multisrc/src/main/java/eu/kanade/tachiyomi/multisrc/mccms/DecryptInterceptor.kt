package eu.kanade.tachiyomi.multisrc.mccms

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object DecryptInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val key = when (request.url.topPrivateDomain()) {
            "bcebos.com" -> key1
            null -> key2
            else -> return response
        }
        val data = decrypt(response.body.bytes(), key)
        val body = data.toResponseBody("image/jpeg".toMediaType())
        return response.newBuilder().body(body).build()
    }

    @Synchronized
    private fun decrypt(input: ByteArray, key: SecretKeySpec): ByteArray {
        val cipher = cipher
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        return cipher.doFinal(input)
    }

    private val cipher by lazy(LazyThreadSafetyMode.NONE) { Cipher.getInstance("DESede/CBC/PKCS5Padding") }
    private val key1 by lazy(LazyThreadSafetyMode.NONE) { SecretKeySpec("OW84U8Eerdb99rtsTXWSILDO".toByteArray(), "DESede") }
    private val key2 by lazy(LazyThreadSafetyMode.NONE) { SecretKeySpec("OW84U8Eerdb99rtsTXWSILEC".toByteArray(), "DESede") }
    private val iv by lazy(LazyThreadSafetyMode.NONE) { IvParameterSpec("SK8bncVu".toByteArray()) }
}
