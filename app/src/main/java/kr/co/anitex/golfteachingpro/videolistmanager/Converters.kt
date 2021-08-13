package kr.co.anitex.golfteachingpro.videolistmanager

import android.content.Context
import java.text.DecimalFormat

/**
 * Created by lenovo on 9/28/2014.
 */
object Converters {
    fun convertPixelsToDp(px: Float, context: Context): Float {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return px / (metrics.densityDpi / 160f)
    }

    fun convertDpToPixel(dp: Float, context: Context): Float {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return dp * (metrics.densityDpi / 160f)
    }

    fun SlowMoFactorConverter(slowMoConstant: Int): Int {
        var slowMoConstant = slowMoConstant
        slowMoConstant = slowMoConstant - 150
        val y = slowMoConstant / 50
        return y + 2
    }

    fun BytesToMb(bytes: String): String {
        val size: String
        val bytesInDouble = bytes.toDouble()
        val kb = bytesInDouble / 1024.0
        val mb = kb / 1024.0
        val gb = kb / 1048576.0
        val dec = DecimalFormat("0.00")
        size = if (gb > 1) {
            dec.format(gb) + " GB"
        } else if (mb > 1) {
            dec.format(mb) + " MB"
        } else {
            dec.format(kb) + " KB"
        }
        return size
    }
}
