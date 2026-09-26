package com.synfusion.attendance.utils.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.DecimalFormat

object PdfFileManager {

    fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt()
        val decFormat = DecimalFormat("#,##0.#")
        return "${decFormat.format(sizeInBytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups.coerceAtMost(units.size - 1)]}"
    }

    suspend fun getPdfPageCount(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = openParcelFileDescriptor(context, uri)
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                return@withContext renderer.pageCount
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                renderer?.close()
            } catch (_: Exception) {}
            try {
                pfd?.close()
            } catch (_: Exception) {}
        }
        return@withContext 0
    }

    suspend fun getFileSize(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        try {
            if (uri.scheme == "file" && uri.path != null) {
                val file = File(uri.path!!)
                if (file.exists()) return@withContext file.length()
            }
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                return@withContext it.length
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext 0L
    }

    suspend fun savePdfToUri(context: Context, sourceUri: Uri, destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(sourceUri)
            outputStream = context.contentResolver.openOutputStream(destinationUri)
            if (inputStream != null && outputStream != null) {
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
        }
        return@withContext false
    }

    fun openParcelFileDescriptor(context: Context, uri: Uri): ParcelFileDescriptor? {
        return try {
            if (uri.scheme == "file" && uri.path != null) {
                val file = File(uri.path!!)
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
