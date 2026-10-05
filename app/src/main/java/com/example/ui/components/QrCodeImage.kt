package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DeepEmerald
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Générateur de QR Code haute précision & haute fidélité pour BaobabTicket.
 * Intègre la correction d'erreur maximale (Niveau H - 30%), un contraste net
 * et une bordure dorée pour un scan instantané via CameraX ou scanner optique.
 */
@Composable
fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    qrColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val bitmap = remember(content, size, qrColor, backgroundColor) {
        generatePristineQrBitmap(
            text = content,
            pixelDimension = 512,
            qrColorArgb = qrColor.toArgb(),
            bgColorArgb = backgroundColor.toArgb()
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(2.dp, ChampagneGold.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Code QR Sécurisé : $content",
                modifier = Modifier
                    .size(size - 8.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        }
    }
}

private fun generatePristineQrBitmap(
    text: String,
    pixelDimension: Int,
    qrColorArgb: Int,
    bgColorArgb: Int
): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, pixelDimension, pixelDimension, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) qrColorArgb else bgColorArgb
            }
        }

        val baseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }

        // Overlay central emblem watermark with Baobab leaf/emblem pattern
        val canvas = Canvas(baseBitmap)
        val centerSize = (pixelDimension * 0.18f).toInt()
        val centerLeft = (pixelDimension - centerSize) / 2f
        val centerTop = (pixelDimension - centerSize) / 2f

        val badgePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#C9A96E") // Champagne Gold
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        val innerCirclePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#123C2A") // Deep Emerald
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Central protective circular badge
        val radius = centerSize / 2f
        val cx = centerLeft + radius
        val cy = centerTop + radius
        canvas.drawCircle(cx, cy, radius, badgePaint)
        canvas.drawCircle(cx, cy, radius - 2f, innerCirclePaint)
        canvas.drawCircle(cx, cy, radius, borderPaint)

        // Monogram "BT" on central emblem
        val textPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#C9A96E")
            textSize = radius * 0.9f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val yPos = (cy - ((textPaint.descent() + textPaint.ascent()) / 2))
        canvas.drawText("B", cx, yPos, textPaint)

        baseBitmap
    } catch (_: Exception) {
        null
    }
}
