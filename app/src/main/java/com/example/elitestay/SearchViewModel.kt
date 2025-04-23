package com.example.elitestay


import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class SearchViewModel : ViewModel() {

    private val client = OkHttpClient()
    private val apiKey = "AIzaSyBEUsccO5elkQd3fYEzICIN6VOQiInZLJU"

    suspend fun getSuggestions(query: String): List<String> {
        return withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()

            try {
                val url = "https://maps.googleapis.com/maps/api/place/autocomplete/json" +
                        "?input=${query.replace(" ", "%20")}" +
                        "&types=geocode" +
                        "&key=$apiKey"

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val json = response.body?.string() ?: return@withContext emptyList()

                val predictions = JSONObject(json).optJSONArray("predictions") ?: return@withContext emptyList()

                val results = mutableListOf<String>()
                for (i in 0 until predictions.length()) {
                    val desc = predictions.getJSONObject(i).optString("description")
                    if (desc.isNotBlank()) results.add(desc)
                }

                results
            } catch (e: Exception) {
                Log.e("SearchViewModel", "API Error: ${e.message}")
                emptyList()
            }
        }
    }
}
