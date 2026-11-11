package com.turkeyproje.gozlerai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

class ModelStore(context: Context) {
    private val directory = File(context.filesDir, "models").apply { mkdirs() }

    fun fileFor(model: ModelInfo) = File(directory, model.filename)
    fun isDownloaded(model: ModelInfo) = fileFor(model).let { it.exists() && it.length() > 100_000_000L }
    fun delete(model: ModelInfo) { fileFor(model).delete() }

    suspend fun download(model: ModelInfo, progress: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        val target = fileFor(model)
        val partial = File(directory, model.filename + ".part")
        val connection = (URL(model.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "GozlerAI-Android/1.0")
        }
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("İndirme sunucusu HTTP ${connection.responseCode} döndürdü.")
            }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    var downloaded = 0L
                    var lastReport = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        if (downloaded - lastReport >= 1024 * 1024) {
                            progress(downloaded, total)
                            lastReport = downloaded
                        }
                    }
                    output.fd.sync()
                    progress(downloaded, total)
                }
            }
            if (partial.length() < 100_000_000L) throw IllegalStateException("İndirilen dosya beklenenden küçük; model dosyası doğrulanamadı.")
            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) throw IllegalStateException("İndirilen model dosyası kaydedilemedi.")
        } catch (e: Exception) {
            partial.delete()
            throw e
        } finally {
            connection.disconnect()
        }
    }
}
