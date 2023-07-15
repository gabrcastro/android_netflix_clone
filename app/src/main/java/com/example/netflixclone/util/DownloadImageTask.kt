package com.example.netflixclone.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class DownloadImageTask(private val callback: Callback) {

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    interface Callback {
        fun onResult(bitmap: Bitmap)
    }

    fun execute(url: String) {
        executor.execute {
            var urlConnection: HttpsURLConnection? = null
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

                val bitmap = BitmapFactory.decodeStream(stream)

                handler.post {
                    callback.onResult(bitmap)
                }

            } catch (e: IOException) {
                val message = e.message ?: "unkown error"
                Log.e("Test", message, e)
            } finally {
                urlConnection?.disconnect()
                stream?.close()
            }
        }
    }
}