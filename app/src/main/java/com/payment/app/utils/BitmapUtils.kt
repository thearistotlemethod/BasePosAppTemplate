package com.payment.app.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
object BitmapUtils {
    enum class Horizontal {
        LEFT, CENTER, RIGHT
    }

    fun textAsBitmap(
        text: String,
        textColor: Int,
        fontSize: Float,
        font: Typeface?,
        width: Int,
        height: Int,
        isBold: Boolean,
        isItalic: Boolean,
        horizontalAlign: Horizontal
    ): Bitmap {
        var inputText = text
        //float maxWidth = 384;
        val pageWidth = inputText.length * fontSize
        val paint = TextPaint(1)
        paint.isLinearText = true
        paint.isSubpixelText = true
        paint.textSize = fontSize
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.color = textColor
        paint.textAlign = Align.LEFT
        var style = Typeface.NORMAL // not bold
        if (isBold) {
            style = Typeface.BOLD
            if (isItalic) {
                style = Typeface.BOLD_ITALIC
            }
        } else if (isItalic) {
            style = Typeface.ITALIC
        }
        paint.typeface = Typeface.create(font, style)
        val baseline = -paint.ascent()
        var textWidth = (paint.measureText(inputText) + 0.0f).toInt()
        while (textWidth > width) {
            val len = inputText.length
            inputText = inputText.substring(0, len - 1)
            textWidth = (paint.measureText(inputText) + 0.0f).toInt()
        }
        val image = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        image.eraseColor(-1)
        val canvas = Canvas(image)
        var x: Layout.Alignment = Layout.Alignment.ALIGN_CENTER
        if (horizontalAlign === Horizontal.LEFT) //left
            x =
                Layout.Alignment.ALIGN_NORMAL else if (horizontalAlign === Horizontal.RIGHT) //right
            x = Layout.Alignment.ALIGN_OPPOSITE
        val mTextLayout = StaticLayout(inputText, paint, width, x, 0.0f, 0.0f, false)
        mTextLayout.draw(canvas)
        return image
    }

    fun combineVertical(bitmaps: ArrayList<Bitmap>): Bitmap? {
        val bitmapList: MutableList<Bitmap> = ArrayList()
        for (bitmap in bitmaps) {
            bitmapList.add(bitmap)
        }
        if (bitmapList.isEmpty()) return null
        val bitmap = bitmapList[0]
        bitmapList.removeAt(0)
        return combineVertical(bitmap, bitmapList)
    }

    private fun combineVertical(bitmap: Bitmap, bitmaps: MutableList<Bitmap>): Bitmap {
        if (bitmaps.isEmpty()) return bitmap
        var result = bitmap
        while (!bitmaps.isEmpty()) {
            val bitmapNext = bitmaps[0]
            bitmaps.removeAt(0)
            result = combineVertical(result, bitmapNext)
        }
        return result
    }

    fun combineVertical(bitmap1: Bitmap, bitmap2: Bitmap): Bitmap {
        val a = bitmap1.width
        val b = bitmap1.height
        val c = bitmap2.width
        val d = bitmap2.height
        val width = Math.max(a, c)
        val height = b + d
        return combine(bitmap1, bitmap2, width, height, 0, b)
    }

    fun combineHorizontal(bitmap1: Bitmap, bitmap2: Bitmap): Bitmap {
        val a = bitmap1.width
        val b = bitmap1.height
        val c = bitmap2.width
        val d = bitmap2.height
        val width = a + c
        val height = Math.max(b, d)
        return combine(bitmap1, bitmap2, width, height, a, 0)
    }
    private fun combine(
        bitmap1: Bitmap,
        bitmap2: Bitmap,
        width: Int,
        height: Int,
        left: Int,
        top: Int
    ): Bitmap {
        val b3 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(b3)
        canvas.drawBitmap(bitmap1, 0f, 0f, null)
        canvas.drawBitmap(bitmap2, left.toFloat(), top.toFloat(), null)
        return b3
    }
}