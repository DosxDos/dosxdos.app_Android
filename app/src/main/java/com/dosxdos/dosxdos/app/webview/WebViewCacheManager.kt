package com.dosxdos.dosxdos.app.webview

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

object WebViewCacheManager {

    fun getCachedFile(context: Context, url: String): File {
        val uri = Uri.parse(url)
        val fileName = uri.lastPathSegment ?: "index.html"
        return File(context.filesDir, fileName)
    }

    fun getCachedWebResource(file: File, url: String): WebResourceResponse? {
        return try {
            WebResourceResponse(getMimeType(url), "UTF-8", FileInputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getMimeType(url: String): String {
        return when {
            url.endsWith(".html") -> "text/html"
            url.endsWith(".css") -> "text/css"
            url.endsWith(".js") -> "application/javascript"
            url.endsWith(".json") -> "application/json"
            url.endsWith(".png") -> "image/png"
            url.endsWith(".jpg") || url.endsWith(".jpeg") -> "image/jpeg"
            url.endsWith(".svg") -> "image/svg+xml"
            url.endsWith(".webp") -> "image/webp"
            url.endsWith(".gif") -> "image/gif"
            else -> "text/html"
        }
    }

    fun overWriteUrl(file: File, url: String): WebResourceResponse? {
        return try {
            if (file.exists()) file.delete()
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(file)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
                WebResourceResponse("text/html", "UTF-8", FileInputStream(file))
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
