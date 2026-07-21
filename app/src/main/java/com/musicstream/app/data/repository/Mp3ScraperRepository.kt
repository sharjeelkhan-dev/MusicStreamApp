package com.musicstream.app.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Mp3ScraperRepository @Inject constructor() {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    suspend fun resolveMp3Link(query: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("Mp3Scraper", "Searching for: $query")
            
            // 1. Search on PagalWorld (reliable for Bollywood/Pop)
            val searchUrl = "https://www.pagalworld.com.se/search?q=${query.replace(" ", "+")}"
            val searchDoc = Jsoup.connect(searchUrl).userAgent(userAgent).get()
            
            val firstResult = searchDoc.select("div.list-show").firstOrNull()?.select("a")?.firstOrNull()
            val detailPath = firstResult?.attr("href")
            
            if (!detailPath.isNullOrEmpty()) {
                val detailUrl = if (detailPath.startsWith("http")) detailPath else "https://www.pagalworld.com.se$detailPath"
                Log.d("Mp3Scraper", "Found detail page: $detailUrl")
                
                val detailDoc = Jsoup.connect(detailUrl).userAgent(userAgent).get()
                
                // Extract direct download link (usually 320kbps is the primary button)
                val downloadBtn = detailDoc.select("a.download-btn").firstOrNull { 
                    it.text().contains("320", ignoreCase = true) 
                } ?: detailDoc.select("a.download-btn").firstOrNull()
                
                val mp3Url = downloadBtn?.attr("href")
                if (!mp3Url.isNullOrEmpty()) {
                    val finalUrl = if (mp3Url.startsWith("http")) mp3Url else "https://www.pagalworld.com.se$mp3Url"
                    Log.d("Mp3Scraper", "Resolved MP3 Link: $finalUrl")
                    return@withContext finalUrl
                }
            }
            
            // 2. Fallback to Mr-Jatt
            val mrJattSearch = "https://www.mr-jatt.im/search.php?q=${query.replace(" ", "+")}"
            val mrJattDoc = Jsoup.connect(mrJattSearch).userAgent(userAgent).get()
            val mrJattResult = mrJattDoc.select("div.list-show a").firstOrNull()?.attr("href")
            
            if (!mrJattResult.isNullOrEmpty()) {
                val mrJattDetail = if (mrJattResult.startsWith("http")) mrJattResult else "https://www.mr-jatt.im$mrJattResult"
                val mrJattDetailDoc = Jsoup.connect(mrJattDetail).userAgent(userAgent).get()
                val mrJattDownload = mrJattDetailDoc.select("a[href$=.mp3]").firstOrNull { it.text().contains("320") }
                    ?: mrJattDetailDoc.select("a[href$=.mp3]").firstOrNull()
                
                val finalMrJatt = mrJattDownload?.attr("href")
                if (!finalMrJatt.isNullOrEmpty()) {
                    Log.d("Mp3Scraper", "Resolved Mr-Jatt Link: $finalMrJatt")
                    return@withContext finalMrJatt
                }
            }

            null
        } catch (e: Exception) {
            Log.e("Mp3Scraper", "Error scraping MP3: ${e.message}")
            null
        }
    }
}
