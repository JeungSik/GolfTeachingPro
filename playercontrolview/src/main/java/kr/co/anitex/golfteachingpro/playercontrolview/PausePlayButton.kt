package kr.co.anitex.golfteachingpro.playercontrolview

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import com.github.ogapants.playercontrolview.R

//import teachingpro.support.v4.content.ContextCompat;
//import teachingpro.support.v7.widget.AppCompatImageButton;
class PausePlayButton @JvmOverloads constructor(
	context: Context?,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = R.attr.imageButtonStyle
) : AppCompatImageButton(context, attrs, defStyleAttr) {
	var playDrawable: Drawable? = null
	var pauseDrawable: Drawable? = null
	fun toggleImage(isPlaying: Boolean) {
		if (isPlaying) {
			setImageDrawable(pauseDrawable)
		} else {
			setImageDrawable(playDrawable)
		}
	}

	init {
		if (isInEditMode) {
			setImageResource(R.drawable.ic_play_arrow_white_36dp)
		} else {
			pauseDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_pause_white_36dp)
			playDrawable =
				ContextCompat.getDrawable(getContext(), R.drawable.ic_play_arrow_white_36dp)
			toggleImage(false)
		}
	}
}