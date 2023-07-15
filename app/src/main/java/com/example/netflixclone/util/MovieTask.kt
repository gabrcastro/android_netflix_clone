package com.example.netflixclone.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.netflixclone.model.Movie
import com.example.netflixclone.model.MovieDetail
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class MovieTask(private val callback: Callback) {

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    interface Callback {
        fun onPreExecute()
        fun onResult(movieDetail: MovieDetail)
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

                if (statusCode == 400) {
                    stream = urlConnection.errorStream
                    buffer = BufferedInputStream(stream)
                    val jsonAsString = toString(buffer)

                    val json = JSONObject(jsonAsString)
                    val message = json.getString("message")
                    throw IOException(message)

                } else if (statusCode > 400) { // problem
                    throw IOException("Server communication error!")
                }

                stream = urlConnection.inputStream

                buffer = BufferedInputStream(stream)
                val jsonAsString = toString(buffer)

                val categories = toMovieDetail(jsonAsString)

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

    private fun toMovieDetail(jsonAsString: String) : MovieDetail {
        val json = JSONObject(jsonAsString)

        val id = json.getInt("id")
        val title = json.getString("title")
        val desc = json.getString("desc")
        val cast = json.getString("cast")
        val coverUrl = json.getString("cover_url")
        val jsonMovies = json.getJSONArray("movie")

        val similars = mutableListOf<Movie>()

        for (i in 0 until jsonMovies.length()) {
            val jsonMovie = jsonMovies.getJSONObject(i)

            val similarId =  jsonMovie.getInt("id")
            val similarCoverUrl = jsonMovie.getString("cover_url")

            val m = Movie(similarId, similarCoverUrl)

            similars.add(m)
        }

        val movie = Movie(id, coverUrl, title, desc, cast)

        return MovieDetail(movie, similars)
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