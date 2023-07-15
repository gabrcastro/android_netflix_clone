package com.example.netflixclone.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.netflixclone.model.Category
import com.example.netflixclone.model.Movie
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class CategoryTask(private val callback: Callback) {

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    interface Callback {
        fun onPreExecute()
        fun onResult(categories: List<Category>)
        fun onFailure(message: String)
    }

    fun execute(url: String) {
        // in this moment is using first thread (1)
        callback.onPreExecute()
        executor.execute {
            // now is using a new thread (2)
            var urlConnection: HttpsURLConnection? = null
            var buffer: BufferedInputStream? = null
            var stream: InputStream? = null

            try {
                val requestURL = URL(url) // open an URL
                urlConnection = requestURL.openConnection() as HttpsURLConnection // open a connection with the server
                urlConnection.readTimeout = 2000 //read timeout (2s)
                urlConnection.connectTimeout = 2000 // connect timeout (2s)

                val statusCode = urlConnection.responseCode
                if (statusCode > 400) { // problem
                    throw IOException("Server communication error!")
                }

                stream = urlConnection.inputStream
                // 1 - simple (stream = bytes sequence)
                // val jsonAsString = stream.bufferedReader().use { it.readText() } // index all bytes in a memory space

                // 2 - byte by byte
                buffer = BufferedInputStream(stream)
                val jsonAsString = toString(buffer)

                val categories = toCategories(jsonAsString)

                handler.post {
                    // retorna dentro da UI Thread
                    callback.onResult(categories)
                }

            } catch (e: Exception) {
                val message = e.message ?: "unkown error"
                Log.e("Test", message, e)

                handler.post {
                    callback.onFailure(message)
                }
            } finally {
                urlConnection?.disconnect()
                buffer?.close()
                stream?.close()
            }
        }
    }

    private fun toCategories(jsonAsString: String) : List<Category> {
        val categories = mutableListOf<Category>()

        val jsonRoot = JSONObject(jsonAsString)
        val jsonCategories = jsonRoot.getJSONArray("category")

        for (i in 0 until jsonCategories.length()) {
            val jsonCategory = jsonCategories.getJSONObject(i)

            val title = jsonCategory.getString("title")
            val jsonMovies = jsonCategory.getJSONArray("movie")

            val movies = mutableListOf<Movie>()

            for (j in 0 until jsonMovies.length()) {
                val jsonMovie = jsonMovies.getJSONObject(j)
                val id = jsonMovie.getInt("id")
                val coverUrl = jsonMovie.getString("cover_url")

                movies.add(Movie(id, coverUrl))
            }

            categories.add(Category(title, movies))
        }

        return categories
    }

    private fun toString(stream: InputStream) : String {
        val bytes = ByteArray(1024)
        val baos = ByteArrayOutputStream()
        var read: Int
        while(true) {
            read = stream.read(bytes)
            if (read <= 0) {
                break
            }
            baos.write(bytes, 0, read)
        }

        return String(baos.toByteArray())
    }
}