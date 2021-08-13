package kr.co.anitex.golfteachingpro.videolistmanager

import android.content.Context
import android.graphics.*

object BitmapUtil {
    const val TAG = "VLC/Util/BitmapUtil"
    fun scaleDownBitmap(context: Context, bitmap: Bitmap?, width: Int): Bitmap? {
        var bitmap = bitmap
        if (bitmap != null) {
            try {
                val densityMultiplier = context.resources.displayMetrics.density
                val bitmapHeight = bitmap.height
                val bitmapWidth = bitmap.width
                val w: Int
                val h: Int
                val x: Int
                val y: Int
                if (bitmapHeight > bitmapWidth * 3 / 4) {
                    w = bitmapWidth
                    h = bitmapWidth * 3 / 4
                    x = 0
                    y = (bitmapHeight - h) / 2
                } else if (bitmapWidth > bitmapHeight * 4 / 3) {
                    h = bitmapHeight
                    w = bitmapHeight * 4 / 3
                    y = 0
                    x = (bitmapWidth - w) / 2
                } else {
                    w = bitmapWidth
                    h = bitmapHeight
                    x = 0
                    y = 0
                }
                bitmap = Bitmap.createBitmap(bitmap, x, y, w, h)
                val dstWidth = (width * densityMultiplier).toInt()
                val dstHeight = dstWidth * 3 / 4
                if (w > dstWidth || h > dstHeight) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, false)
                }
            } catch (ex: Exception) {
                //under some situation the the value of h becomes <= zero in createbitmap, adding a try catch and returning the original bitmap
                return bitmap
            }
        }
        return bitmap
    }

    fun getRoundedCornerBitmap(bitmap: Bitmap?, pixels: Int): Bitmap? {
        if (bitmap == null) return null
        val output: Bitmap
        try {
            output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val color = -0xbdbdbe
            val paint = Paint()
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            val rectF = RectF(rect)
            val roundPx = pixels.toFloat()
            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            paint.color = color
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
        } catch (ex: Exception) {

            //adding as a precaution because finding a few crashes in bitmap modification
            return bitmap
        }
        return output
    }
}
