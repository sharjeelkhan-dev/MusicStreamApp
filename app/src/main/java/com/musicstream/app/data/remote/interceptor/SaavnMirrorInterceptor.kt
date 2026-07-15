package com.musicstream.app.data.remote.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaavnMirrorInterceptor @Inject constructor() : Interceptor {

    private val mirrors = listOf(
        "https://saavn.dev/",
        "https://saavn-api.vercel.app/",
        "https://jiosaavn-api.vercel.app/",
        "https://jiosaavn-api-liard.vercel.app/",
        "https://saavn-api-beta.vercel.app/"
    )

    private var currentMirrorIndex = 0

    @Synchronized
    fun rotateMirror() {
        currentMirrorIndex = (currentMirrorIndex + 1) % mirrors.size
        Log.d("SaavnRotation", "Rotating to mirror: ${mirrors[currentMirrorIndex]}")
    }

    fun getCurrentMirror(): String = mirrors[currentMirrorIndex]

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url.toString()

        // Only intercept requests for Saavn domains
        if (!originalUrl.contains("saavn") && !originalUrl.contains("jiosaavn")) {
            return chain.proceed(originalRequest)
        }

        var lastException: Exception? = null

        // Try up to all available mirrors
        for (i in mirrors.indices) {
            try {
                val currentMirror = getCurrentMirror().trimEnd('/')
                val pathAndQuery = originalRequest.url.encodedPath
                val query = originalRequest.url.encodedQuery?.let { "?$it" } ?: ""
                
                val newUrlString = "$currentMirror$pathAndQuery$query"
                val newRequest = originalRequest.newBuilder()
                    .url(newUrlString)
                    .build()

                val response = chain.proceed(newRequest)

                if (response.isSuccessful) {
                    return response
                }

                // If mirror fails (403, 429, 404, 5xx), rotate and try next
                Log.w("SaavnInterceptor", "Mirror $currentMirror failed with code: ${response.code}. Rotating...")
                response.close()
                rotateMirror()
            } catch (e: Exception) {
                Log.e("SaavnInterceptor", "Error connecting to ${getCurrentMirror()}: ${e.message}")
                lastException = e
                rotateMirror()
            }
        }

        throw lastException ?: IOException("Failed to connect to any working Saavn mirror")
    }
}
