package com.example.betterme.presentation.share.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.betterme.domain.share.VerifiedShare
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a 1080×1350 PNG progress card for ACTION_SEND. Pure Android
 * Canvas — no Compose-to-bitmap dance, no GraphicsLayer dependency.
 *
 * Layout (top → bottom):
 *  1. Header strip          "🔥 BetterMe Progress"
 *  2. Display name + avatar circle (placeholder, no network)
 *  3. Headline stat row     Check-in / Streak / Trophies / Legendary
 *  4. Verified badge        "✔ Đã xác minh bởi Firebase"
 *  5. Watermark footer      "betterme.app"
 *
 * Output is written under `cacheDir/share_cards/progress-{ts}.png`
 * and surfaced as a `content://` URI through [FileProvider] so the
 * receiving app (Messenger / Zalo / Facebook) can read it across the
 * process boundary.
 *
 * The file is kept around — subsequent share intents reuse a fresh
 * file, and the OS reclaims `cacheDir` space whenever it needs to.
 */
class ShareCardRenderer(
    private val context: Context
) {

    /**
     * Build the bitmap, write it to cache, and return a shareable URI.
     */
    fun renderToUri(share: VerifiedShare): Uri {
        val bitmap = render(share)
        val file = writeToCacheFile(bitmap)
        bitmap.recycle()
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    // ─── Drawing ───────────────────────────────────────────────────

    private fun render(share: VerifiedShare): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas)
        drawHeader(canvas)
        drawProfile(canvas, share)
        drawStatsRow(canvas, share)
        drawVerifiedPill(canvas)
        drawFooter(canvas)
        return bitmap
    }

    private fun drawBackground(canvas: Canvas) {
        // Soft vertical gradient — Firebase-blue → near-white. Keeps
        // text legible on bright social-app previews.
        val paint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, CARD_H.toFloat(),
                Color.parseColor("#EAF1FC"),
                Color.parseColor("#FFFFFF"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CARD_W.toFloat(), CARD_H.toFloat(), paint)
    }

    private fun drawHeader(canvas: Canvas) {
        val title = "🔥 BetterMe Progress"
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F3D6E")
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title, CARD_W / 2f, 130f, paint)

        // Sub-label
        val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5B7FBF")
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Tiến độ check-in", CARD_W / 2f, 190f, sub)
    }

    private fun drawProfile(canvas: Canvas, share: VerifiedShare) {
        val centerX = CARD_W / 2f
        val avatarCenterY = 360f
        val avatarRadius = 100f

        // Avatar ring + initial (no network — keep the card offline-safe)
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFFFF")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, avatarCenterY, avatarRadius + 6f, ringPaint)
        val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0077FF")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, avatarCenterY, avatarRadius, avatarPaint)

        // Initial letter
        val initial = share.profile.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
        val initialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 120f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        // textY = baseline. Approximate vertical centering against the
        // circle: shift down by ~1/3 of textSize.
        canvas.drawText(initial, centerX, avatarCenterY + 40f, initialPaint)

        // Display name under the avatar
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111111")
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            share.profile.displayName.take(36),
            centerX,
            avatarCenterY + avatarRadius + 90f,
            namePaint
        )
    }

    private fun drawStatsRow(canvas: Canvas, share: VerifiedShare) {
        val rowTop = 660f
        val rowBottom = 920f
        val cellWidth = (CARD_W - 80f - 24f * 3) / 4f // 40px margin, 24px gutter
        val cellLeft0 = 40f

        val labels = listOf("Check-in", "Chuỗi", "Thử thách", "Huyền thoại")
        val values = listOf(
            share.summary.totalCheckIns.toString(),
            "${share.summary.currentStreakDays}🔥",
            "${share.summary.completedChallenges}🏆",
            share.summary.legendaryChallenges.toString()
        )

        labels.forEachIndexed { i, label ->
            val left = cellLeft0 + i * (cellWidth + 24f)
            drawStatCell(canvas, left, rowTop, cellWidth, rowBottom - rowTop, label, values[i])
        }
    }

    private fun drawStatCell(
        canvas: Canvas,
        left: Float, top: Float, width: Float, height: Float,
        label: String, value: String
    ) {
        val rect = RectF(left, top, left + width, top + height)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E4E4E6")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, 24f, 24f, bgPaint)
        canvas.drawRoundRect(rect, 24f, 24f, borderPaint)

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111111")
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(value, left + width / 2f, top + height / 2f, valuePaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#808080")
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, left + width / 2f, top + height - 30f, labelPaint)
    }

    private fun drawVerifiedPill(canvas: Canvas) {
        val pillTop = 1010f
        val pillHeight = 88f
        val pillWidth = 700f
        val pillLeft = (CARD_W - pillWidth) / 2f
        val rect = RectF(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EFF5FF")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B6CCEC")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, 44f, 44f, bgPaint)
        canvas.drawRoundRect(rect, 44f, 44f, borderPaint)

        // Amber flame disc on the left
        val discCenterX = pillLeft + 60f
        val discCenterY = pillTop + pillHeight / 2f
        val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FBE5BB")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(discCenterX, discCenterY, 28f, discPaint)
        val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F59E0B")
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🔥", discCenterX, discCenterY + 13f, flamePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F3D6E")
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("Đã xác minh bởi Firebase  ✓", discCenterX + 50f, discCenterY + 14f, textPaint)
    }

    private fun drawFooter(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#999999")
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("BetterMe • Habit & Challenge Tracker", CARD_W / 2f, 1280f, paint)
        // Decorative bottom rule
        val rulePaint = Paint().apply {
            color = Color.parseColor("#0077FF")
            strokeWidth = 6f
        }
        val ruleW = 240f
        canvas.drawLine(
            CARD_W / 2f - ruleW / 2f, 1310f,
            CARD_W / 2f + ruleW / 2f, 1310f,
            rulePaint
        )
        // Suppress unused-import warning until we wire `Path`-based art.
        @Suppress("UNUSED_VARIABLE") val unusedPath = Path()
    }

    // ─── File I/O ──────────────────────────────────────────────────

    private fun writeToCacheFile(bitmap: Bitmap): File {
        val dir = File(context.cacheDir, "share_cards").apply { mkdirs() }
        val file = File(dir, "progress-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        return file
    }

    private companion object {
        // 4:5 portrait — looks right on Messenger / Zalo / Facebook
        // preview cards, and matches Instagram's preferred aspect.
        const val CARD_W = 1080
        const val CARD_H = 1350
    }
}
