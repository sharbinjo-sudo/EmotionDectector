package com.emotion.detection

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

object ImageUtils {
    fun imageProxyToGrayscaleBitmap(image: ImageProxy, reqWidth: Int, reqHeight: Int): Bitmap {
        val yPlane = image.planes[0].buffer
        val rowStride = image.planes[0].rowStride
        val width = image.width
        val height = image.height

        val pixels = IntArray(width * height)

        yPlane.rewind()

        for (row in 0 until height) {
            val rowStart = row * rowStride
            for (col in 0 until width) {
                if (rowStart + col < yPlane.limit()) {
                    val y = yPlane.get(rowStart + col).toInt() and 0xFF
                    pixels[row * width + col] = (0xFF shl 24) or (y shl 16) or (y shl 8) or y
                }
            }
        }

        val grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        grayBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        val rotated = when (image.imageInfo.rotationDegrees) {
            90 -> grayBitmap.rotate(90f)
            180 -> grayBitmap.rotate(180f)
            270 -> grayBitmap.rotate(270f)
            else -> grayBitmap
        }

        return scaleCenterCrop(rotated, reqWidth, reqHeight)
    }

    private fun scaleCenterCrop(src: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val srcWidth = src.width
        val srcHeight = src.height

        val scale = maxOf(
            newWidth.toFloat() / srcWidth.toFloat(),
            newHeight.toFloat() / srcHeight.toFloat()
        )

        val scaledWidth = (scale * srcWidth).toInt()
        val scaledHeight = (scale * srcHeight).toInt()

        val scaled = Bitmap.createScaledBitmap(src, scaledWidth, scaledHeight, true)
        val x = (scaledWidth - newWidth) / 2
        val y = (scaledHeight - newHeight) / 2

        return Bitmap.createBitmap(scaled, x, y, newWidth, newHeight)
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}
