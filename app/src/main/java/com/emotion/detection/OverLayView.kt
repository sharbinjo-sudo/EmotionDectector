package com.emotion.detection

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.camera.view.PreviewView

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
        color = Color.WHITE
    }

    private var faces: List<Rect> = emptyList()
    private var srcWidth = 0
    private var srcHeight = 0
    private var previewView: PreviewView? = null

    fun setFaces(faces: List<Rect>, srcWidth: Int, srcHeight: Int, previewView: PreviewView) {
        this.faces = faces
        this.srcWidth = srcWidth
        this.srcHeight = srcHeight
        this.previewView = previewView
        postInvalidate()
    }

    fun clear() {
        faces = emptyList()
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (faces.isEmpty() || srcWidth == 0 || srcHeight == 0 || previewView == null) return

        // Map face rects from source (bitmap) coordinates to this view's coordinate system (which overlays previewView)
        val pv = previewView!!

        // Get transformation: compute scale and offsets between source image (srcWidth x srcHeight) and PreviewView display size
        val pvWidth = pv.width.toFloat()
        val pvHeight = pv.height.toFloat()

        // Compute scale preserving aspect ratio (fit center)
        val scale = minOf(pvWidth / srcWidth.toFloat(), pvHeight / srcHeight.toFloat())
        val scaledW = srcWidth * scale
        val scaledH = srcHeight * scale
        val dx = (pvWidth - scaledW) / 2f
        val dy = (pvHeight - scaledH) / 2f

        for (r in faces) {
            val left = r.left * scale + dx
            val top = r.top * scale + dy
            val right = r.right * scale + dx
            val bottom = r.bottom * scale + dy
            canvas.drawRect(left, top, right, bottom, paint)
        }
    }
}
