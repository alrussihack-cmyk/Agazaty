package com.alrussihack.agazaty.export

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File

const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

/** Saving straight into the public Downloads folder needs no permission from Android 10 on. */
val canSaveToDownloads: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

/** Writes the file to Downloads. Throws if the system refuses. */
@RequiresApi(Build.VERSION_CODES.Q)
fun saveToDownloads(context: Context, name: String, bytes: ByteArray) {
    check(canSaveToDownloads)
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, name)
        put(MediaStore.Downloads.MIME_TYPE, XLSX_MIME)
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("MediaStore refused the file")
    try {
        resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Cannot open $uri")
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    } catch (e: Exception) {
        resolver.delete(uri, null, null)
        throw e
    }
}

/** Writes the file to the app cache (old exports are removed) and returns a shareable content:// uri. */
fun writeShareable(context: Context, name: String, bytes: ByteArray): Uri {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }
    val file = File(dir, name).apply { writeBytes(bytes) }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun shareXlsx(context: Context, uri: Uri, name: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = XLSX_MIME
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, name)
        clipData = ClipData.newRawUri(name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "مشاركة ملف Excel"))
}
