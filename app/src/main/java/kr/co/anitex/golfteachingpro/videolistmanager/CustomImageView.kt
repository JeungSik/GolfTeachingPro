package kr.co.anitex.golfteachingpro.videolistmanager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * Created by nitinagarwal on 3/24/17.
 */
class CustomImageView : AppCompatImageView {
    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!,
        attrs,
        defStyle
    )

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val clipPath = Path()
        val rect = RectF(0F, 0F, this.width.toFloat(), this.height.toFloat())
        clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW)
        canvas.clipPath(clipPath)
        super.onDraw(canvas)
    }

    companion object {
        var radius = 10.0f
    }
}
