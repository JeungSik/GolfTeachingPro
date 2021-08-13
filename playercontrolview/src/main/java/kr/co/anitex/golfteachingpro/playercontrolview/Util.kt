package kr.co.anitex.golfteachingpro.playercontrolview

import android.R
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import androidx.annotation.ColorInt
import kotlin.math.max

//import teachingpro.support.annotation.ColorInt;
internal object Util {
	/*
	fun formatTime(timeMs: Int): String {
		if (timeMs < 1000) {
			return "00:00"
		}
		var result = ""
		val totalSec = timeMs / 1000
		val hours = totalSec / 3600
		val minutes = totalSec / 60 % 60
		val seconds = totalSec % 60

		if (hours > 0) {
			result += "$hours:"
		}
		result += if (minutes >= 10) {
			"$minutes:"
		} else {
			"0$minutes:"
		}
		if (seconds >= 10) {
			result += seconds
		} else {
			result += "0$seconds"
		}
		return result
	}
	*/

	fun formatTimeMs(timeMs: Int): String {
		val mil = ((timeMs % 1000) / 10)
		val sec = ((timeMs / 1000) % 60)
		val min = ((timeMs / 1000) / 60)

		return "%02d:%02d.%02d".format(min, sec, mil)
	}

	fun createStateListDrawable(
		drawable: Drawable,
		@ColorInt drawableColor: Int
	): StateListDrawable {
		val stateListDrawable = StateListDrawable()
		val defaultStateSet =
			intArrayOf(-R.attr.state_pressed, -R.attr.state_focused, R.attr.state_enabled)
		stateListDrawable.addState(defaultStateSet, drawable)
		val focusedStateSet = intArrayOf(-R.attr.state_pressed, R.attr.state_focused)
		val focusedDrawable = darkenDrawable(drawable, drawableColor, 0.7f)
		stateListDrawable.addState(focusedStateSet, focusedDrawable)
		val pressedStateSet = intArrayOf(R.attr.state_pressed)
		val pressedDrawable = darkenDrawable(drawable, drawableColor, 0.6f)
		stateListDrawable.addState(pressedStateSet, pressedDrawable)
		val disableStateSet = intArrayOf(-R.attr.state_enabled)
		val disableDrawable = darkenDrawable(drawable, drawableColor, 0.4f)
		stateListDrawable.addState(disableStateSet, disableDrawable)
		return stateListDrawable
	}

	@Suppress("DEPRECATION")
	private fun darkenDrawable(
		drawable: Drawable,
		@ColorInt drawableColor: Int,
		factor: Float
	): Drawable {
		val color = darkenColor(drawableColor, factor)
		val d = drawable.constantState!!.newDrawable().mutate()
		d.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
		return d
	}

	@ColorInt
	private fun darkenColor(@ColorInt color: Int, factor: Float): Int {
		if (factor < 0 || factor > 1) {
			return color
		}
		val alpha = Color.alpha(color)
		val red = Color.red(color)
		val green = Color.green(color)
		val blue = Color.blue(color)
		return Color.argb(
			alpha,
			max((red * factor).toInt(), 0),
			max((green * factor).toInt(), 0),
			max((blue * factor).toInt(), 0)
		)
	}
}