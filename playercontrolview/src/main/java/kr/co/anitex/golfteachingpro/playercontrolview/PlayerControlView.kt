package kr.co.anitex.golfteachingpro.playercontrolview

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.MediaController.MediaPlayerControl
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.content.ContextCompat
import com.github.ogapants.playercontrolview.R

//import teachingpro.support.annotation.Nullable;
//import teachingpro.support.v4.content.ContextCompat;
class PlayerControlView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
	val viewHolder: ViewHolder
	@JvmField
    var alwaysShow = false
	private var player: MediaPlayerControl? = null
	var isShowing = false
		private set
	private var dragging = false
	private var fastRewindMs: Int
	private var fastForwardMs: Int
	private var showTimeoutMs: Int
	private var delListener: OnClickListener? = null
	private var analyzeListener: OnClickListener? = null
	private var postListener: OnClickListener? = null
	private var speedListener: OnClickListener? = null
	private var onVisibilityChangedListener: OnVisibilityChangedListener? = null
	private val updateProgressRunnable: Runnable = object : Runnable {
		override fun run() {
			try {
				val pos = updateProgress()
				if (!dragging && isShowing && player != null && player!!.isPlaying) {
					postDelayed(this, 1000 - (pos % 1000).toLong())
				}
			}catch (e: Exception) {
				Log.d(TAG, "ERROR - unexpected exception : ${e.message}")
			}
		}
	}
	private val hideRunnable = Runnable { hide() }
	private fun toStateListDrawable(drawable: Drawable?): Drawable {
		val drawableColor = ContextCompat.getColor(context, android.R.color.white)
		return Util.createStateListDrawable(drawable!!, drawableColor)
	}

	fun setPlayer(player: MediaPlayerControl?) {
		this.player = player
		updatePausePlayImage()
	}

	fun attach(activity: Activity) {
		val rootView = activity.findViewById<View>(android.R.id.content) as ViewGroup
		attach(rootView)
	}

	fun attach(rootView: ViewGroup) {
		rootView.removeView(this)
		if (rootView is RelativeLayout) {
			val layoutParams = RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			)
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
			rootView.addView(this, layoutParams)
		} else {
			val layoutParams = LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM
			)
			rootView.addView(this, layoutParams)
		}
	}

	fun show() {
		show(showTimeoutMs)
	}

	fun show(showTimeoutMs: Int) {
		isShowing = true
		if (onVisibilityChangedListener != null) {
			onVisibilityChangedListener!!.onShown(this)
		}
		visibility = VISIBLE
		isFocusable = true
		isFocusableInTouchMode = true
		descendantFocusability = FOCUS_AFTER_DESCENDANTS
		requestFocus()
		updateProgress()
		viewHolder.pausePlayButton.requestFocus()
		disableUnsupportedButtons()
		updatePausePlayImage()
		removeCallbacks(updateProgressRunnable)
		post(updateProgressRunnable)
		removeCallbacks(hideRunnable)
		if (!alwaysShow) {
			postDelayed(hideRunnable, showTimeoutMs.toLong())
		}
	}

	fun hide() {
		isShowing = false
		if (onVisibilityChangedListener != null) {
			onVisibilityChangedListener!!.onHidden(this)
		}
		removeCallbacks(hideRunnable)
		removeCallbacks(updateProgressRunnable)
		visibility = GONE
	}

	fun toggleVisibility() {
		if (isShowing) {
			hide()
		} else {
			show()
		}
	}

	private fun updateProgress(): Int {
		if (dragging || player == null) {
			return 0
		}
		updatePausePlayImage()
		val currentTime = player!!.currentPosition
		val totalTime = player!!.duration
		if (totalTime > 0) {
			val position = 1000L * currentTime / totalTime
			viewHolder.seekBar.progress = position.toInt()
		}
		val percent = player!!.bufferPercentage
		viewHolder.seekBar.secondaryProgress = percent * 10
		viewHolder.currentTimeText.text =
			Util.formatTimeMs(currentTime)
		viewHolder.totalTimeText.text =
			Util.formatTimeMs(totalTime)
		return currentTime
	}

	private fun updateTimeText() {
		val currentTime = player!!.currentPosition
		val totalTime = player!!.duration
		viewHolder.currentTimeText.text =
			Util.formatTimeMs(currentTime)
		viewHolder.totalTimeText.text =
			Util.formatTimeMs(totalTime)
	}

	override fun dispatchKeyEvent(event: KeyEvent): Boolean {
		if (player == null) {
			return super.dispatchKeyEvent(event)
		}
		val uniqueDown = event.repeatCount == 0 && event.action == KeyEvent.ACTION_DOWN
		when (event.keyCode) {
			KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_SPACE -> {
				if (uniqueDown) {
					doPauseResume()
					show()
					viewHolder.pausePlayButton.requestFocus()
				}
				return true
			}
			KeyEvent.KEYCODE_MEDIA_PLAY -> {
				if (uniqueDown && !player!!.isPlaying) {
					player!!.start()
					show()
				}
				return true
			}
			KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_PAUSE -> {
				if (uniqueDown && player!!.isPlaying) {
					player!!.pause()
					show()
				}
				return true
			}
			KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_MUTE, KeyEvent.KEYCODE_CAMERA, KeyEvent.KEYCODE_MENU -> return super.dispatchKeyEvent(
				event
			)
			KeyEvent.KEYCODE_BACK -> {
				if (alwaysShow) {
					return super.dispatchKeyEvent(event)
				}
				if (uniqueDown) {
					hide()
				}
				return true
			}
			KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
				if (player!!.canSeekForward()) {
					player!!.seekTo(fastForwardMs)
					show()
				}
				return true
			}
			KeyEvent.KEYCODE_MEDIA_REWIND -> {
				if (player!!.canSeekForward()) {
					player!!.seekTo(fastRewindMs)
					show()
				}
				return true
			}
			else -> {
			}
		}
		show()
		return super.dispatchKeyEvent(event)
	}

	public override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		removeCallbacks(updateProgressRunnable)
		removeCallbacks(hideRunnable)
	}

	fun updatePausePlayImage() {
		if (player == null) {
			return
		}
		viewHolder.pausePlayButton.toggleImage(player!!.isPlaying)
	}

	private fun doPauseResume() {
		if (player == null) {
			return
		}
		if (player!!.isPlaying) {
			player!!.pause()
		} else {
			player!!.start()
		}
		updatePausePlayImage()
	}

	override fun setEnabled(enabled: Boolean) {
		viewHolder.pausePlayButton.isEnabled = enabled
		viewHolder.fastForwardButton.isEnabled = enabled
		viewHolder.fastRewindButton.isEnabled = enabled
		viewHolder.deleteButton.isEnabled = enabled && delListener != null
		viewHolder.poseAnalyzeButton.isEnabled = enabled && analyzeListener != null
		viewHolder.postPoseButton.isEnabled = enabled && postListener != null
		//viewHolder.speedButton.isEnabled = enabled && speedListener != null
		viewHolder.speedText.isEnabled = enabled && speedListener != null
		viewHolder.seekBar.isEnabled = enabled
		disableUnsupportedButtons()
		super.setEnabled(enabled)
	}

	private fun disableUnsupportedButtons() {
		if (player == null) {
			return
		}
		if (!player!!.canPause()) {
			viewHolder.pausePlayButton.isEnabled = false
		}
		if (!player!!.canSeekBackward()) {
			viewHolder.fastRewindButton.isEnabled = false
		}
		if (!player!!.canSeekForward()) {
			viewHolder.fastForwardButton.isEnabled = false
		}
		if (!player!!.canSeekBackward() && !player!!.canSeekForward()) {
			viewHolder.seekBar.isEnabled = false
		}
	}

	fun setSpeedOnClickListener(speedOnClickListener: OnClickListener?) {
		this.speedListener = speedOnClickListener
		//viewHolder.speedButton.visibility =
		//	if (speedOnClickListener == null) INVISIBLE else VISIBLE
		viewHolder.speedText.visibility =
			if (speedOnClickListener == null) INVISIBLE else VISIBLE
	}

	fun setDelOnClickListener(delOnClickListener: OnClickListener?) {
		this.delListener = delOnClickListener
		viewHolder.deleteButton.visibility =
			if (delOnClickListener == null) INVISIBLE else VISIBLE
	}

	fun setAnalyzeOnClickListener(analyzeOnClickListener: OnClickListener?) {
		this.analyzeListener = analyzeOnClickListener
		viewHolder.poseAnalyzeButton.visibility =
			if (analyzeOnClickListener == null) INVISIBLE else VISIBLE
	}

	fun setPostOnClickListener(postOnClickListener: OnClickListener?) {
		this.postListener = postOnClickListener
		viewHolder.postPoseButton.visibility =
			if (postOnClickListener == null) INVISIBLE else VISIBLE
	}

	fun setFastRewindMs(fastRewindMs: Int) {
		this.fastRewindMs = fastRewindMs
	}

	fun setFastForwardMs(fastForwardMs: Int) {
		this.fastForwardMs = fastForwardMs
	}

	fun setShowTimeoutMs(showTimeoutMs: Int) {
		this.showTimeoutMs = showTimeoutMs
	}

	fun setAlwaysShow(alwaysShow: Boolean) {
		this.alwaysShow = alwaysShow
		if (alwaysShow) {
			removeCallbacks(hideRunnable)
		}
	}

	fun setOnVisibilityChangedListener(listener: OnVisibilityChangedListener?) {
		onVisibilityChangedListener = listener
	}

	val mediaControllerWrapper: MediaController
		get() = MediaControllerWrapper(this)

	override fun getAccessibilityClassName(): CharSequence {
		return PlayerControlView::class.java.name
	}

	interface OnVisibilityChangedListener {
		fun onShown(view: PlayerControlView?)
		fun onHidden(view: PlayerControlView?)
	}

	class ViewHolder(view: View) {
		val controlsBackground: LinearLayout = view.findViewById<View>(R.id.controls_background) as LinearLayout
		val seekBar: SeekBar = view.findViewById<View>(R.id.seek_bar) as SeekBar
		val totalTimeText: TextView = view.findViewById<View>(R.id.total_time_text) as TextView
		val currentTimeText: TextView = view.findViewById<View>(R.id.current_time_text) as TextView
		val pausePlayButton: PausePlayButton = view.findViewById<View>(R.id.pause_play) as PausePlayButton
		val fastForwardButton: ImageButton = view.findViewById<View>(R.id.fast_forward) as ImageButton
		val fastRewindButton: ImageButton = view.findViewById<View>(R.id.fast_rewind) as ImageButton
		val deleteButton: ImageButton = view.findViewById<View>(R.id.delete) as ImageButton
		val poseAnalyzeButton: ImageButton = view.findViewById<View>(R.id.pose_analyze) as ImageButton
		val postPoseButton: ImageButton = view.findViewById<View>(R.id.post_pose) as ImageButton
		//val speedButton: ImageButton = view.findViewById<View>(R.id.speed) as ImageButton
		val speedText: TextView = view.findViewById<View>(R.id.speed_text) as TextView
	}

	private inner class ComponentListener : OnSeekBarChangeListener, OnClickListener {
		override fun onClick(v: View) {
			if (player == null) {
				return
			}
			if (v === viewHolder.pausePlayButton) {
				doPauseResume()
			} else if (v === viewHolder.fastRewindButton) {
				var position = player!!.currentPosition
				position -= fastRewindMs
				player!!.seekTo(position)
				updateProgress()
			} else if (v === viewHolder.fastForwardButton) {
				var position = player!!.currentPosition
				position += fastForwardMs
				player!!.seekTo(position)
				updateProgress()
			//} else if (v === viewHolder.speedButton) {
			} else if (v === viewHolder.speedText) {
				if (speedListener != null) {
					speedListener!!.onClick(v)
				}
			} else if (v === viewHolder.deleteButton) {
				if (delListener != null) {
					delListener!!.onClick(v)
				}
			} else if (v === viewHolder.poseAnalyzeButton) {
				if (analyzeListener != null) {
					analyzeListener!!.onClick(v)
				}
			} else if (v === viewHolder.postPoseButton) {
				if (postListener != null) {
					postListener!!.onClick(v)
				}
			}
			show()
		}

		override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
			if (!fromUser || player == null) {
				return
			}
			val duration = player!!.duration.toLong()
			val newPosition = duration * progress / 1000L
			player!!.seekTo(newPosition.toInt())
			viewHolder.currentTimeText.text = Util.formatTimeMs(
				newPosition.toInt()
			)
		}

		override fun onStartTrackingTouch(seekBar: SeekBar) {
			show()
			dragging = true
			removeCallbacks(hideRunnable)
			removeCallbacks(updateProgressRunnable)
		}

		override fun onStopTrackingTouch(seekBar: SeekBar) {
			dragging = false
			show()
			post(updateProgressRunnable)
		}
	}

	companion object {
		private val TAG = PlayerControlView::class.java.simpleName
		private const val DEFAULT_FAST_REWIND_MS = 5000
		private const val DEFAULT_FAST_FORWARD_MS = 15000
		private const val DEFAULT_SHOW_TIMEOUT_MS = 3000
	}

	init {
		inflate(getContext(), R.layout.player_control_view, this)
		viewHolder = ViewHolder(this)
		fastRewindMs = DEFAULT_FAST_REWIND_MS
		fastForwardMs = DEFAULT_FAST_FORWARD_MS
		showTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS
		if (attrs != null) {
			val a = context.theme.obtainStyledAttributes(attrs, R.styleable.PlayerControlView, 0, 0)
			fastRewindMs =
				a.getInt(R.styleable.PlayerControlView_pcv_fast_rewind_ms, DEFAULT_FAST_REWIND_MS)
			fastForwardMs =
				a.getInt(R.styleable.PlayerControlView_pcv_fast_forward_ms, DEFAULT_FAST_FORWARD_MS)
			showTimeoutMs =
				a.getInt(R.styleable.PlayerControlView_pcv_show_timeout_ms, DEFAULT_SHOW_TIMEOUT_MS)
			alwaysShow = a.getBoolean(R.styleable.PlayerControlView_pcv_always_show, false)
			a.recycle()
		}
		if (isInEditMode) {
		} else {
			val componentListener = ComponentListener()
			viewHolder.pausePlayButton.setOnClickListener(componentListener)
			viewHolder.fastForwardButton.setOnClickListener(componentListener)
			viewHolder.fastRewindButton.setOnClickListener(componentListener)
			//viewHolder.speedButton.setOnClickListener(componentListener)
			viewHolder.deleteButton.setOnClickListener(componentListener)
			viewHolder.poseAnalyzeButton.setOnClickListener(componentListener)
			viewHolder.postPoseButton.setOnClickListener(componentListener)
			viewHolder.speedText.setOnClickListener(componentListener)
			viewHolder.seekBar.setOnSeekBarChangeListener(componentListener)
			viewHolder.seekBar.max = 1000
			val pauseDrawable = toStateListDrawable(viewHolder.pausePlayButton.pauseDrawable)
			viewHolder.pausePlayButton.pauseDrawable = pauseDrawable
			val playDrawable = toStateListDrawable(viewHolder.pausePlayButton.playDrawable)
			viewHolder.pausePlayButton.playDrawable = playDrawable
			viewHolder.fastForwardButton.setImageDrawable(toStateListDrawable(viewHolder.fastForwardButton.drawable))
			viewHolder.fastRewindButton.setImageDrawable(toStateListDrawable(viewHolder.fastRewindButton.drawable))
			viewHolder.deleteButton.setImageDrawable(toStateListDrawable(viewHolder.deleteButton.drawable))
			//viewHolder.poseAnalyzeButton.setImageDrawable(toStateListDrawable(viewHolder.poseAnalyzeButton.drawable))
			//viewHolder.postPoseButton.setImageDrawable(toStateListDrawable(viewHolder.postPoseButton.drawable))
			//viewHolder.speedButton.setImageDrawable(toStateListDrawable(viewHolder.speedButton.drawable))
			viewHolder.deleteButton.visibility = INVISIBLE
			//viewHolder.poseAnalyzeButton.visibility = INVISIBLE
			//viewHolder.postPoseButton.visibility = INVISIBLE
			//viewHolder.speedButton.visibility = INVISIBLE
			hide()
		}
	}
}