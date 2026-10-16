package com.captionai.studio.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.OutputStream

object ImageUtils {

    fun shareCaptionCard(context: Context, caption: String, platform: String) {
        val bitmap = generateCaptionCard(caption, platform)
        val uri = saveBitmapToPublicFolder(context, bitmap, "caption_${System.currentTimeMillis()}.png")
        
        if (uri != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "$caption\n\n— Generated with Caption AI ✨")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Caption Card"))
        }
    }

    private fun generateCaptionCard(caption: String, platform: String): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint()
        
        // Dynamic Gradient based on Platform
        val colors = when(platform.uppercase()) {
            "INSTAGRAM" -> intArrayOf(Color.parseColor("#833ab4"), Color.parseColor("#fd1d1d"), Color.parseColor("#fcb045"))
            "TIKTOK" -> intArrayOf(Color.parseColor("#000000"), Color.parseColor("#25F4EE"), Color.parseColor("#FE2C55"))
            "WHATSAPP" -> intArrayOf(Color.parseColor("#128C7E"), Color.parseColor("#25D366"), Color.parseColor("#DCF8C6"))
            "FACEBOOK" -> intArrayOf(Color.parseColor("#1877F2"), Color.parseColor("#3b5998"), Color.parseColor("#8b9dc3"))
            "LINKEDIN" -> intArrayOf(Color.parseColor("#0077B5"), Color.parseColor("#00a0dc"), Color.parseColor("#313335"))
            else -> intArrayOf(Color.parseColor("#7D5FFF"), Color.parseColor("#6C5CE7"), Color.parseColor("#a29bfe"))
        }
        
        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            colors,
            null, Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Large Background Watermark
        paint.shader = null
        val watermarkBgPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 180f
            isAntiAlias = true
            alpha = 15 // Very subtle
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        canvas.save()
        canvas.rotate(-45f, width / 2f, height / 2f)
        val repeatText = "CAPTION AI  "
        val bgText = repeatText.repeat(5)
        for (i in -5..10) {
            canvas.drawText(bgText, -width.toFloat(), i * 220f, watermarkBgPaint)
        }
        canvas.restore()

        // Decorative circles
        paint.color = Color.WHITE
        paint.alpha = 25
        canvas.drawCircle(width * 0.9f, height * 0.1f, 300f, paint)
        canvas.drawCircle(width * 0.1f, height * 0.9f, 400f, paint)

        val labelPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 42f
            isAntiAlias = true
            alpha = 200
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(platform.uppercase(), 80f, 120f, labelPaint)

        val quotePaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 240f
            isAntiAlias = true
            alpha = 120
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("“", 80f, 320f, quotePaint)

        val textPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 68f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(10f, 0f, 5f, Color.parseColor("#44000000"))
        }

        val textWidth = width - 160
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(caption, 0, caption.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.3f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(caption, textPaint, textWidth, Layout.Alignment.ALIGN_CENTER, 1.3f, 0f, false)
        }

        canvas.save()
        val textHeight = staticLayout.height
        val dy = (height - textHeight) / 2f
        canvas.translate(80f, dy)
        staticLayout.draw(canvas)
        canvas.restore()

        val bottomWatermarkPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 38f
            isAntiAlias = true
            alpha = 220
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val watermark = "Generated with Caption AI"
        val watermarkWidth = bottomWatermarkPaint.measureText(watermark)
        canvas.drawText(watermark, (width - watermarkWidth) / 2f, height - 100f, bottomWatermarkPaint)

        return bitmap
    }

    private fun saveBitmapToPublicFolder(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CaptionAI")
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        return try {
            uri?.let {
                val outputStream: OutputStream? = resolver.openOutputStream(it)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                it
            }
        } catch (e: Exception) {
            null
        }
    }
}
