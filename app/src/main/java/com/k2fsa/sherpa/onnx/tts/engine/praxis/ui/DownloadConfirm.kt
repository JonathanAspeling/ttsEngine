package com.k2fsa.sherpa.onnx.tts.engine.praxis.ui

import android.app.Activity
import android.app.AlertDialog
import com.k2fsa.sherpa.onnx.tts.engine.Downloader
import com.k2fsa.sherpa.onnx.tts.engine.databinding.ActivityManageLanguagesBinding
import java.net.HttpURLConnection
import java.net.URL

object DownloadConfirm {

    /**
     * Makes a HEAD request to get the model's file size, then shows a
     * "~X MB — Download?" confirmation dialog before starting the download.
     *
     * [onConfirm] is invoked on the main thread when the user taps Download.
     */
    fun checkSizeAndConfirm(
        activity: Activity,
        model: String,
        onnxUrl: String,
        onConfirm: () -> Unit,
    ) {
        val loading = AlertDialog.Builder(activity)
            .setTitle(model)
            .setMessage("Checking model size…")
            .setCancelable(true)
            .show()

        Thread {
            val bytes = headContentLength(onnxUrl)
            activity.runOnUiThread {
                if (!loading.isShowing) return@runOnUiThread
                loading.dismiss()
                val sizeText = if (bytes > 0) formatBytes(bytes) else "size unknown"
                AlertDialog.Builder(activity)
                    .setTitle(model)
                    .setMessage("Download this voice model?\n\nApproximate size: $sizeText")
                    .setPositiveButton("Download") { _, _ -> onConfirm() }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }.start()
    }

    private fun headContentLength(url: String): Long {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "HEAD"
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.instanceFollowRedirects = true
            conn.connect()
            val length = conn.contentLengthLong
            conn.disconnect()
            length
        } catch (e: Exception) {
            -1L
        }
    }

    internal fun formatBytes(bytes: Long): String = when {
        bytes >= 1_000_000_000L -> "%.1f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000L     -> "%.0f MB".format(bytes / 1_000_000.0)
        else                    -> "%.0f KB".format(bytes / 1_000.0)
    }
}
