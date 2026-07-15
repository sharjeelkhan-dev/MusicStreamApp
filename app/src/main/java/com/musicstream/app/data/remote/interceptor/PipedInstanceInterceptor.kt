package com.musicstream.app.data.remote.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipedInstanceInterceptor @Inject constructor() : Interceptor {

    private val instances = listOf(
        "https://pipedapi.in.projectsegfau.lt/", // India
        "https://pipedapi.kavin.rocks/",         // Global
        "https://pipedapi.us.projectsegfau.lt/", // USA
        "https://piped-api.garudalinux.org/",    // Germany
        "https://api-piped.mha.fi/",             // Finland
        "https://pipedapi.rivo.lol/"             // Community
    )

    private var currentInstanceIndex = 0

    @Synchronized
    fun rotateInstance() {
        currentInstanceIndex = (currentInstanceIndex + 1) % instances.size
        Log.d("PipedRotation", "Rotating to instance: ${instances[currentInstanceIndex]}")
    }

    fun getCurrentBaseUrl(): String = instances[currentInstanceIndex]

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url.toString()
        
        // ZAROORI: Never redirect these domains
        if (originalUrl.contains("jiosaavn.com") || originalUrl.contains("saavn.dev") ||
            originalUrl.contains("saavn-api") || originalUrl.contains("google.com") ||
            originalUrl.contains("googlevideo.com") || originalUrl.contains("ytimg.com") || 
            originalUrl.contains("ggpht.com") || originalUrl.contains("youtube.com")) {
            return chain.proceed(originalRequest)
        }

        var lastException: Exception? = null
        
        // Try up to 4 different instances
        for (i in 0 until 4) {
            try {
                val currentBase = getCurrentBaseUrl().trimEnd('/')
                val pathAndQuery = originalRequest.url.encodedPath
                val query = originalRequest.url.encodedQuery?.let { "?$it" } ?: ""
                
                val newUrlString = "$currentBase$pathAndQuery$query"
                val newRequest = originalRequest.newBuilder()
                    .url(newUrlString)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                    .build()

                val response = chain.proceed(newRequest)

                if (response.isSuccessful) {
                    return response
                }

                // If 403, 429, or 5xx, rotate and try next
                Log.w("PipedInterceptor", "Instance $currentBase failed with code: ${response.code}. Rotating...")
                response.close()
                rotateInstance()
            } catch (e: Exception) {
                Log.e("PipedInterceptor", "Error connecting to ${getCurrentBaseUrl()}: ${e.message}")
                lastException = e
                rotateInstance()
            }
        }

        throw lastException ?: IOException("Failed to connect to any working Piped instance")
    }
}
