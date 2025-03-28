package com.dosxdos.dosxdos.app.webview

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.dosxdos.dosxdos.app.Clases.FileData
import java.io.InputStream

object FileChooserHelper {

    fun getResultUri(data: Intent?): Array<Uri>? {
        return if (data?.clipData != null) {
            val itemCount = data.clipData?.itemCount ?: 0
            Array(itemCount) { i ->
                data.clipData?.getItemAt(i)?.uri ?: Uri.EMPTY
            }
        } else {
            arrayOf(data?.data ?: Uri.EMPTY)
        }
    }

    fun convertUrisToFileData(contentResolver: ContentResolver, uris: Array<Uri>): List<FileData> {
        return uris.mapNotNull { uri ->
            try {
                val base64Data = uriToBase64(contentResolver, uri)
                val fileName = uri.lastPathSegment ?: "unknown"
                FileData(fileName, "image/jpeg", base64Data)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun uriToBase64(contentResolver: ContentResolver, uri: Uri): String {
        val inputStream: InputStream = contentResolver.openInputStream(uri) ?: return ""
        val byteArray = inputStream.readBytes()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}