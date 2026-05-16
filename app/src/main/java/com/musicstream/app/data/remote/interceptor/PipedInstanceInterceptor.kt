package com.musicstream.app.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipedInstanceInterceptor @Inject constructor() : Interceptor {

    private val instances = listOf(
        "https://pipedapi.in.projectsegfau.lt/", // India (Best for Hindi content)
        "https://pipedapi.kavin.rocks/",         // Global/Official
        "https://pipedapi.us.projectsegfau.lt/", // USA
        "https://piped-api.garudalinux.org/",    // Germany
        "https://api-piped.mha.fi/",             // Finland
        "https://pipedapi.rivo.lol/"             // Community
    )

    private var currentInstanceIndex = 0

    @Synchronized
    fun rotateInstance() {
        currentInstanceIndex = (currentInstanceIndex + 1) % instances.size
        android.util.Log.d("PipedRotation", "Rotating to instance: ${instances[currentInstanceIndex]}")
    }

    fun getCurrentBaseUrl(): String = instances[currentInstanceIndex]

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url.toString()
        
        // Only intercept Piped API requests (YouTubeApi uses BASE_URL which contains "piped")
        // But we must be careful not to block MusicApi which is Saavn
        if (!originalUrl.contains("piped")) {
            return chain.proceed(originalRequest)
        }

        var request = originalRequest
        var response: Response? = null
        var lastException: Exception? = null

        // Try up to 3 different instances if one fails
        repeat(3) {
            try {
                val newUrl = request.url.newBuilder()
                    .scheme(originalRequest.url.scheme)
                    .host(java.net.URL(getCurrentBaseUrl()).host)
                    // Piped instances might have different port or path prefixes, 
                    // but usually they follow the same structure after the host.
                    .build()

                request = request.newBuilder().url(newUrl).build()
                response = chain.proceed(request)

                if (response.isSuccessful) {
                    return response
                }

                // If not successful (e.g., 403, 429, 500), rotate and try again
                response.close()
                rotateInstance()
            } catch (e: Exception) {
                lastException = e
                rotateInstance()
            }
        }

        return response ?: throw lastException ?: IOException("Failed to connect to any Piped instance")
    }
}
