package com.example.betterme.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Stages check-in photos into `cacheDir/share_images/` as resized + compressed
 * JPEGs and returns FileProvider [Uri]s ready for `ACTION_SEND_MULTIPLE`.
 *
 * Handles three input forms:
 *  - `https://…` / `http://…` remote URLs (Cloudinary uploads) — fetched via OkHttp
 *  - `file://…` or absolute paths — read directly
 *  - blank / null / 404 / decode failure — silently skipped (per spec, missing
 *    images must not break sharing)
 *
 * Output knobs:
 *  - `maxLongEdgePx` defaults to 1080 (1080p — Messenger / Zalo cap thumbnails
 *    around this size anyway; sharing 4K originals just slows the OS chooser)
 *  - `jpegQuality` defaults to 82 (sweet spot for photos)
 *  - `maxCount` defaults to 9 (typical chooser gallery limit; bigger payloads
 *    are silently truncated)
 *
 * Each output filename embeds the SHA-like hash of the source URL so re-shares
 * of the same set hit the same cache entries — the OS chooser opens fast and
 * the disk footprint stays bounded.
 */
class ChallengeShareImagePrep(
    private val context: Context
) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Prepare up to [maxCount] images, returning the FileProvider URIs.
     * Missing / unreachable / undecodable sources are dropped silently.
     */
    suspend fun prepare(
        sources: List<String>,
        maxCount: Int = MAX_IMAGES,
        maxLongEdgePx: Int = MAX_LONG_EDGE_PX,
        jpegQuality: Int = JPEG_QUALITY
    ): List<Uri> = withContext(Dispatchers.IO) {
        val outDir = File(context.cacheDir, OUT_SUBDIR).apply { mkdirs() }
        val results = mutableListOf<Uri>()

        sources.asSequence()
            .filter { !it.isNullOrBlank() }
            .distinct()
            .take(maxCount)
            .forEach { source ->
                val uri = runCatching {
                    val bytes = fetch(source) ?: return@runCatching null
                    val resized = decodeAndResize(bytes, maxLongEdgePx) ?: return@runCatching null
                    val file = File(outDir, "${stableName(source)}.jpg")
                    FileOutputStream(file).use { os ->
                        resized.compress(Bitmap.CompressFormat.JPEG, jpegQuality, os)
                    }
                    resized.recycle()
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                }.onFailure {
                    Log.w(TAG, "Skipping image: $source — ${it.javaClass.simpleName}: ${it.message}")
                }.getOrNull()
                if (uri != null) results.add(uri)
            }

        Log.i(TAG, "Prepared ${results.size}/${sources.size} share images at $outDir")
        results
    }

    /** Fetch bytes from a URL or local path. Returns null on any failure. */
    private fun fetch(source: String): ByteArray? {
        return try {
            if (source.startsWith("http://") || source.startsWith("https://")) {
                val req = Request.Builder().url(source).build()
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "Fetch failed ${resp.code} for $source")
                        return null
                    }
                    resp.body?.bytes()
                }
            } else {
                val path = source.removePrefix("file://")
                val file = File(path)
                if (file.exists() && file.canRead()) file.readBytes() else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch threw for $source", e)
            null
        }
    }

    /**
     * Decode the byte stream with an inSampleSize so we never load the full
     * resolution into memory, then scale to [maxLongEdge] preserving aspect
     * ratio. Returns null on decode failure.
     */
    private fun decodeAndResize(bytes: ByteArray, maxLongEdge: Int): Bitmap? {
        // Pass 1: read dimensions only.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(ByteArrayInputStream(bytes), null, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val longEdge = maxOf(bounds.outWidth, bounds.outHeight)
        val sample = computeInSampleSize(longEdge, maxLongEdge)

        // Pass 2: real decode at sampled size.
        val decoded = BitmapFactory.decodeStream(
            ByteArrayInputStream(bytes),
            null,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return null

        // Pass 3: final scale (the sampled bitmap is now within ~2x of target —
        // a single matrix scale finishes the job without burning more memory).
        val scaledLong = maxOf(decoded.width, decoded.height)
        if (scaledLong <= maxLongEdge) return decoded
        val ratio = maxLongEdge.toFloat() / scaledLong.toFloat()
        val newW = (decoded.width * ratio).toInt().coerceAtLeast(1)
        val newH = (decoded.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(decoded, newW, newH, true).also {
            if (it !== decoded) decoded.recycle()
        }
    }

    /**
     * Computes the largest power-of-two [BitmapFactory.Options.inSampleSize]
     * that still keeps the source's long edge ≥ target. Powers of two are
     * specifically recommended by the Bitmap docs because libjpeg downscales
     * cheapest at those steps.
     */
    private fun computeInSampleSize(longEdge: Int, target: Int): Int {
        if (longEdge <= target) return 1
        var sample = 1
        while (longEdge / (sample * 2) >= target) sample *= 2
        return sample
    }

    /**
     * Deterministic filename from the source URL/path so repeated shares of
     * the same set reuse the same cached file (and overwrite gracefully when
     * the source bytes change).
     */
    private fun stableName(source: String): String {
        // Plain hashCode is fine — collisions inside one user's share batch are
        // astronomically unlikely with strings this short, and the cache is
        // throwaway anyway.
        val h = source.hashCode().toLong() and 0xFFFFFFFFL
        return "share_$h"
    }

    private companion object {
        const val TAG = "ChallengeShareImagePrep"
        const val OUT_SUBDIR = "share_images"
        const val MAX_IMAGES = 9
        const val MAX_LONG_EDGE_PX = 1080
        const val JPEG_QUALITY = 82
    }
}
