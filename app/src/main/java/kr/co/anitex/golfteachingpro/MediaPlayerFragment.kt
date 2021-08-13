package kr.co.anitex.golfteachingpro

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.AudioManager
import android.media.MediaActionSound
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.mocircle.cidrawing.DrawingBoard
import com.mocircle.cidrawing.DrawingBoardManager
import com.mocircle.cidrawing.element.shape.LineElement
import com.mocircle.cidrawing.element.shape.OvalElement
import com.mocircle.cidrawing.element.shape.RectElement
import com.mocircle.cidrawing.mode.InsertShapeMode
import com.mocircle.cidrawing.mode.PointerMode
import com.mocircle.cidrawing.mode.eraser.ObjectEraserMode
import com.mocircle.cidrawing.mode.selection.RectSelectionMode
import com.mocircle.cidrawing.view.CiDrawingView
import kotlinx.android.synthetic.main.draw_menubar.*
import kotlinx.android.synthetic.main.fragment_camera.*
import kr.co.anitex.golfteachingpro.playercontrolview.PlayerControlView
import kr.co.anitex.golfteachingpro.recycler.DrawMenuAdapter
import kr.co.anitex.golfteachingpro.recycler.DrawMenuItem
import kr.co.anitex.golfteachingpro.recycler.ItemUtils
import org.bytedeco.javacv.AndroidFrameConverter
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileInputStream
import java.nio.FloatBuffer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.round
import kotlin.math.roundToInt

@Suppress("DEPRECATION", "UNREACHABLE_CODE")
class MediaPlayerFragment : Fragment(), DrawMenuAdapter.DrawMenuViewHolder.Delegate {

	private lateinit var mAdView : AdView

	/** AndroidX navigation arguments */
	private val mArgs: MediaPlayerFragmentArgs by navArgs()

	/** Host's navigation controller */
	private val mNavController: NavController by lazy {
		Navigation.findNavController(requireActivity(), R.id.camera_fragment_container)
	}

	private var mSurfaceView: SurfaceView? = null
	private var mSurfaceHolder: SurfaceHolder? = null

	private var mMediaPlayer: MediaPlayer? = null
	private var mPlayerControlView: PlayerControlView? = null
	private var mSeekbar: SeekBar? = null
	private var mFastForwardBtn: ImageButton? = null
	private var mFastRewindBtn: ImageButton? = null
	private var mPoseAnalyzeBtn: ImageButton? = null
	private var mCurrentTimeText: TextView? = null
	private var mSpeedText: TextView? = null
	private var mPlayerState = false

	private var mPlaybackSpeed = 0.25f
	private var mSeekStep = 0f
	private var mDuration: Int? = null
	private var mCurrentPosition = 0

	private var mThreadService: ScheduledExecutorService? = null

	/** AI Model related*/
	private lateinit var mAImodule: Module                // AI module
	private var mBuffer: FloatBuffer = Tensor.allocateFloatBuffer(AI_SEQUENCE_LEN * AI_FRAME_SIZE)
	private var mTempBuffer: FloatBuffer = Tensor.allocateFloatBuffer((AI_SEQUENCE_LEN -1) * AI_FRAME_SIZE)
	private var mTempFloatArray = FloatArray(AI_FRAME_SIZE)
	private var mTwoBuffer: FloatBuffer = Tensor.allocateFloatBuffer(2 * AI_FRAME_SIZE)
	private val mArrayTensor: Tensor = Tensor.fromBlob(mTwoBuffer, longArrayOf(1, 2, 3, 160, 160))
	private var mBufferIndex = 0

	private fun formatTimeMs(timeMs: Int): String {
		val mil = ((timeMs % 1000) / 10)
		val sec = ((timeMs / 1000) % 60)
		val min = ((timeMs / 1000) / 60)

		return "%02d:%02d.%02d".format(min, sec, mil)
	}

	private fun updateCurrentTimeText() {
		val currentTime = mMediaPlayerControl.currentPosition
		mCurrentTimeText?.text = formatTimeMs(currentTime)

	}

	private val mMediaPlayerControl = object : MediaController.MediaPlayerControl {
		override fun start() {
			mMediaPlayer?.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)

			// Move Seekbar smoothly
			//mThreadService = Executors.newScheduledThreadPool(1)
			mThreadService = Executors.newSingleThreadScheduledExecutor()
			mThreadService?.scheduleWithFixedDelay(
				{
					try {
						val position = mMediaPlayer!!.currentPosition
						val progress = mSeekbar!!.progress

						Log.d(
								TAG,
								"Seek Progress * Step : ${(mSeekStep * progress).toInt()}   MediaPlayer CurrentPosition : $position"
						)

						if (position > (mSeekStep * progress).toInt()) {
							if (mSeekStep == 1F) {
								mSeekbar?.progress = position
							} else {
								mSeekbar?.progress =
									((position * 1000) / mDuration!!)
							}
						}

					} catch (e: Exception) {
						Log.d(TAG, "ERROR - unexpected exception : ${e.message}")
					}
				},
					1,
					(mSeekStep / mPlaybackSpeed * 10).toLong(),
					TimeUnit.MILLISECONDS
			)

			mPoseAnalyzeBtn?.visibility = View.INVISIBLE

			Log.d(TAG, "Start Button Pressed!!!!")
			mMediaPlayer?.start()
		}

		override fun pause() {
			Log.d(
					TAG,
					"Pause Button Pressed!!!! (MediaPlayer Current Position = ${mMediaPlayer?.currentPosition})"
			)
			mMediaPlayer?.pause()

			mPoseAnalyzeBtn?.visibility = View.VISIBLE

			if(mThreadService != null)	{
				try {
					mThreadService?.shutdown()
					mThreadService?.awaitTermination(10, TimeUnit.MILLISECONDS)
					Log.d(TAG, "mThreadService shutdown : ${mThreadService?.isShutdown}!!!!!!")
					Log.d(TAG, "mThreadService terminated : ${mThreadService?.isTerminated}!!!!!!")
					mThreadService = null
				} catch (e: Exception) {
					Log.d(TAG, "ERROR - unexpected exception : ${e.message}")
				}
			}

			mMediaPlayer?.setWakeMode(context, PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
		}

		override fun getDuration(): Int {
			return mDuration!!
		}

		override fun getCurrentPosition(): Int {
			return mMediaPlayer?.currentPosition!!
		}

		@RequiresApi(Build.VERSION_CODES.O)
		override fun seekTo(pos: Int) {
			Log.d(TAG, "MediaPlayerControl SeekTo Call (pos = $pos)")
			if(mSeekStep == 1F)
				mMediaPlayer?.seekTo(pos.toLong(), MediaPlayer.SEEK_CLOSEST)
			else
				mMediaPlayer?.seekTo((pos * mSeekStep).toLong(), MediaPlayer.SEEK_CLOSEST)

			mSeekbar?.progress = pos
		}

		override fun isPlaying(): Boolean {
			return mMediaPlayer?.isPlaying!!
		}

		override fun getBufferPercentage(): Int {
			return 0
		}

		override fun canPause(): Boolean {
			return true
		}

		override fun canSeekBackward(): Boolean {
			return true
		}

		override fun canSeekForward(): Boolean {
			return true
		}

		override fun getAudioSessionId(): Int {
			return 0
		}
	}

	private val mOnSeekbarChangeListener = object : SeekBar.OnSeekBarChangeListener {
		@RequiresApi(Build.VERSION_CODES.O)
		override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
			// Update current position here
			if(fromUser) {
				mMediaPlayerControl.seekTo(progress)
			}

			if(VERBOSE) {
				Log.d(
						TAG,
						"Seekbar Changed : $progress (MediaPlayer POS = ${mMediaPlayer?.currentPosition})-----------------------------------------"
				)
			}
			updateCurrentTimeText()
		}

		override fun onStartTrackingTouch(seekBar: SeekBar?) {
			if(VERBOSE) {
				Log.d(TAG, "Seekbar Start Tracking Touch!!!!!!")
			}

			mPlayerState = mMediaPlayerControl.isPlaying
			mMediaPlayerControl.pause()
			mPlayerControlView?.updatePausePlayImage()
		}

		override fun onStopTrackingTouch(seekBar: SeekBar?) {
			if(VERBOSE) {
				Log.d(TAG, "Seekbar Stop Tracking Touch!!!!!!")
			}

			//if(mPlayerState) mMediaPlayer?.start()
			if(mPlayerState) mMediaPlayerControl.start()
			mPlayerControlView?.updatePausePlayImage()
		}
	}

	/** Shows a [Toast] on the UI thread.	*/
	//@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
	//private fun showToast(text: String) {
	//	Handler(context?.mainLooper!!).post {
	//		Toast.makeText(context?.applicationContext, text, Toast.LENGTH_SHORT).show()
	//	}
	//}

	@RequiresApi(Build.VERSION_CODES.O)
	private val mButtonOnTouchListener = object : View.OnTouchListener {
		private var mHandler : Handler? = null

		private val fastForwardAction = object : Runnable {
			override fun run() {
				val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
				audioManager.playSoundEffect(SoundEffectConstants.CLICK)

				var position = mMediaPlayerControl.currentPosition
				val duration = mMediaPlayerControl.duration
				if(VERBOSE) {
					Log.d(
							TAG,
							"FastForward Button Click (pos=$position+$mSeekStep*$FASTSTEP)!!!!!!"
					)
				}
				if (position < (duration-round(mSeekStep * FASTSTEP).toInt())) {
					position += round(mSeekStep * FASTSTEP).toInt()
				} else {
					position = duration
				}

				if (duration > 0) {
					val newPosition = 1000L * position / duration
					mMediaPlayerControl.seekTo(newPosition.toInt())
				} else {
					mMediaPlayerControl.seekTo(0)
				}

				mHandler!!.postDelayed(this, FASTSTEPINTERVAIL)
			}
		}

		private val fastRewindAction = object : Runnable {
			override fun run() {
				val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
				audioManager.playSoundEffect(SoundEffectConstants.CLICK)

				var position = mMediaPlayerControl.currentPosition
				val duration = mMediaPlayerControl.duration
				if(VERBOSE) {
					Log.d(
							TAG,
							"FastRewind Button Click (pos=$position-$mSeekStep*$FASTSTEP)!!!!!!"
					)
				}

				if (position > round(mSeekStep * FASTSTEP).toInt() )	{
					position -= round(mSeekStep * FASTSTEP).toInt()
				} else {
					position = 0
				}

				if (duration > 0) {
					val newPosition = 1000L * position / duration
					mMediaPlayerControl.seekTo(newPosition.toInt())
				} else {
					mMediaPlayerControl.seekTo(0)
				}

				mHandler!!.postDelayed(this, FASTSTEPINTERVAIL)
			}
		}

		@SuppressLint("ClickableViewAccessibility")
		@RequiresApi(Build.VERSION_CODES.O)
		override fun onTouch(v: View?, event: MotionEvent?): Boolean {

			when (event?.action) {
				MotionEvent.ACTION_DOWN -> {
					if (mHandler != null) return true
					mHandler = Handler()
					if (v === mFastRewindBtn) {
						mHandler!!.postDelayed(fastRewindAction, FASTSTEPINTERVAIL)
					} else if (v === mFastForwardBtn) {
						mHandler!!.postDelayed(fastForwardAction, FASTSTEPINTERVAIL)
					}
				}
				MotionEvent.ACTION_UP -> {
					if (mHandler == null) return true
					if (v === mFastRewindBtn) {
						mHandler!!.removeCallbacks(fastRewindAction)
					} else if (v === mFastForwardBtn) {
						mHandler!!.removeCallbacks(fastForwardAction)
					}
					mHandler = null
				}
			}
			return true
		}
	}

	/**
	 * Returns the Uri which can be used to delete/work with images in the photo gallery.
	 * @param filePath Path to IMAGE on SD card
	 * @return Uri in the format of... content://media/external/images/media/[NUMBER]
	 */
	private fun getUriFromPath(filePath: String): Uri {
		val videoId: Long
		val videoUri = MediaStore.Video.Media.getContentUri("external")
		val projection = arrayOf(MediaStore.Video.Media._ID)
		// TODO This will break if we have no matching item in the MediaStore.
		val cursor: Cursor? = requireContext().contentResolver.query(videoUri, projection, MediaStore.Video.Media.DATA + " LIKE ?", arrayOf(filePath), null)
		cursor!!.moveToFirst()
		val columnIndex: Int = cursor.getColumnIndex(projection[0])
		videoId = cursor.getLong(columnIndex)
		cursor.close()
		return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId)
	}

	private fun preprocessBitmap(input: Bitmap): Bitmap? {
		var bitmap: Bitmap? = input

		val matrix = Matrix()
		matrix.postRotate(90.0f)

		matrix.postScale(
			SCALE_WIDTH.coerceAtMost(SCALE_HEIGHT),
			SCALE_WIDTH.coerceAtMost(SCALE_HEIGHT)
		)

		bitmap = Bitmap.createBitmap(
			bitmap!!,
			0,
			0,
			bitmap.width,
			bitmap.height,
			matrix,
			true
		)

		val resizedBitmap = Bitmap.createBitmap(
			AI_IMAGE_SIZE,
			AI_IMAGE_SIZE,
			Bitmap.Config.ARGB_8888
		)
		val canvas = Canvas(resizedBitmap)
		canvas.drawBitmap(
			bitmap,
			((AI_IMAGE_SIZE - bitmap.width) / 2).toFloat(),
			((AI_IMAGE_SIZE - bitmap.height) / 2).toFloat(),
			null
		)

		return resizedBitmap
	}

	@SuppressLint("ClickableViewAccessibility", "SetTextI18n")
	private val mOnPreparedListener = MediaPlayer.OnPreparedListener { // Set PlayerControlView listener
		mPlayerControlView?.setPlayer(mMediaPlayerControl)

		// Set Video Duration
		mDuration = mMediaPlayer?.duration!! - 50

		// Set Seekbar change listener
		mSeekbar = mPlayerControlView?.findViewById(R.id.seek_bar)

		if(mDuration!! > 1000) {
			mSeekbar?.max = 1000
			mSeekStep = mDuration!! / 1000f
		} else {
			mSeekbar?.max = mDuration!!
			mSeekStep = 1F
		}
		if(VERBOSE) {
			Log.d(
				TAG,
				"Seekbar Max : ${mSeekbar?.max} (MediaPlayer DUR = $mDuration, ${mMediaPlayer?.duration}) seekStep = $mSeekStep -----------------------------"
			)
		}

		mSeekbar?.setOnSeekBarChangeListener(mOnSeekbarChangeListener)

		// Set Fast forward and rewind btn listener
		mFastForwardBtn = mPlayerControlView?.findViewById(R.id.fast_forward)
		mFastRewindBtn = mPlayerControlView?.findViewById(R.id.fast_rewind)
		mSpeedText = mPlayerControlView?.findViewById(R.id.speed_text)
		mCurrentTimeText = mPlayerControlView?.findViewById(R.id.current_time_text)
		mPoseAnalyzeBtn = mPlayerControlView?.findViewById(R.id.pose_analyze)

		mFastForwardBtn?.setOnTouchListener(mButtonOnTouchListener)
		mFastRewindBtn?.setOnTouchListener(mButtonOnTouchListener)

		// Customizing PlayerControlView Play/Pause Button and Colors
		val viewHolder = mPlayerControlView?.viewHolder
		viewHolder?.pausePlayButton?.pauseDrawable = ContextCompat.getDrawable(
			requireContext(),
			R.drawable.ic_pause_circle_filled_white_36dp_vector
		)
		viewHolder?.pausePlayButton?.playDrawable = ContextCompat.getDrawable(
			requireContext(),
			R.drawable.ic_play_circle_filled_white_36dp_vector
		)
		viewHolder?.controlsBackground?.setBackgroundColor(
			ContextCompat.getColor(
				requireContext(),
				R.color.colorToolBar_orange
			)
		)
		viewHolder?.currentTimeText?.setTextColor(
			ContextCompat.getColor(
				requireContext(),
				R.color.main_title_color
			)
		)
		viewHolder?.totalTimeText?.textSize = 18f

		mPlayerControlView?.setDelOnClickListener {
			val file = File(mArgs.fileUri)
			if(file.exists()) {
				if(VERBOSE) Log.d(TAG, "Delete File : ${file.absolutePath}")
				//file.delete()

				val out = getUriFromPath(mArgs.fileUri)
				requireContext().contentResolver.delete(out, null, null)

				/*
					requireContext().contentResolver.delete(
							MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
							MediaStore.Video.Media.DATA + "=" + file.absolutePath, null
					)
				*/
			}


			val fragmentManager = requireFragmentManager()
			if(fragmentManager.backStackEntryCount > 0) {
				//fragmentManager.popBackStack()
				// Launch camera fragment
				val action = MediaPlayerFragmentDirections.actionPlayerToPreview()
				mNavController.navigate(action)
			} else {
				requireActivity().onBackPressed()
			}
		}

		mPlayerControlView?.setSpeedOnClickListener {
			when(mPlaybackSpeed){
				0.25f -> {
					mPlaybackSpeed = 0.5f
					mSpeedText?.text = "1/2x"
				}
				0.5f -> {
					mPlaybackSpeed = 1.0f
					mSpeedText?.text = "1.0x"
				}
				1.0f -> {
					mPlaybackSpeed = 0.25f
					mSpeedText?.text = "1/4x"
				}
				else -> {
					mPlaybackSpeed = 0.25f
					mSpeedText?.text = "1/4x"
				}
			}
			// Set playback speed
			if(VERBOSE) Log.d(TAG, "Change Playback Speed : $mPlaybackSpeed")
			val isPlaying = mMediaPlayerControl.isPlaying
			val currentPosition = mMediaPlayerControl.currentPosition

			mMediaPlayer?.playbackParams = mMediaPlayer?.playbackParams!!.setSpeed(mPlaybackSpeed)

			if(!isPlaying) {
				mMediaPlayerControl.pause()
				mMediaPlayerControl.seekTo(currentPosition * 1000 / mDuration!!)
			}
		}

		mPlayerControlView?.setAnalyzeOnClickListener {
			val grabber = FFmpegFrameGrabber(File(mArgs.fileUri))
			val coverterToBitmap = AndroidFrameConverter()

			grabber.start()

			for(framecnt in 0..grabber.lengthInVideoFrames) {
				val nthFrame = grabber.grabImage()
				var bitmap = coverterToBitmap.convert(nthFrame)
				preprocessBitmap(bitmap).also { bitmap = it }

				val inputTensor: Tensor = TensorImageUtils.bitmapToFloat32Tensor(
					bitmap,
					TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
					TensorImageUtils.TORCHVISION_NORM_STD_RGB,
				)

				if (mBufferIndex < AI_SEQUENCE_LEN) {
					mBuffer.position(mBufferIndex * AI_FRAME_SIZE)
					mBuffer.put(inputTensor.dataAsFloatArray)
				} else {
					// decode inputTensor Image
					/*
                    for(i in 0..7) {
                        val tempBuffer: FloatArray = FloatArray(mFrameSize)
                        mBuffer.position(i * mFrameSize)
                        mBuffer.get(tempBuffer)
                        val floatImage = floatArrayToBitmap(tempBuffer, 160, 160)
                        if(VERBOSE) Log.d(TAG, "image frame....")
                    }
                    */

					mBuffer.position((mBufferIndex % AI_SEQUENCE_LEN) * AI_FRAME_SIZE)
					mBuffer.put(inputTensor.dataAsFloatArray)
				}

				mBufferIndex += 1

				// complete to make Input data
				/*
				if (mBufferIndex >= AI_SEQUENCE_LEN) {
					mBuffer.position((mBufferIndex % mSequence_len) * mFrameSize)
					mBuffer.get(mTempFloatArray, 0, mFrameSize)
					mTwoBuffer.position(0)
					mTwoBuffer.put(mTempFloatArray, 0, mFrameSize)
					mTwoBuffer.put(inputTensor.dataAsFloatArray)

					val outputTensor =
						mAImodule.forward(IValue.from(mArrayTensor)).toTensor()
					val outputs = softmax(outputTensor.dataAsFloatArray)
					val result = argmax(outputs)
					val endTime = System.currentTimeMillis()
					if (CameraFragment.VERBOSE) Log.d(CameraFragment.TAG, "AI Golf Pose Detection Result : $result  ----------------- 소요시간(${endTime-startTime})")
					if (result == 0 && outputs[result] >= mAccuracy && !mAddressPoseDetect) {
						mAddressPoseDetect = true
						Handler(Looper.getMainLooper()).post { progressBar_cyclic.visibility = View.INVISIBLE }

						// Play Sound Effect to Start Video Recording
						CameraFragment.sound.play(MediaActionSound.SHUTTER_CLICK)
						Handler(Looper.getMainLooper()).post {
							recording_notify.visibility = View.VISIBLE
							recording_time_signal.text = "REC"
						}

						startVideoRecording()

						// Start recording timer
						mRecordingTimer.start()
						mRecordingStartMillis = System.currentTimeMillis()

						if (CameraFragment.VERBOSE) Log.d(CameraFragment.TAG, "Video Recording started!!!!!")

						// Button image change
						Handler(Looper.getMainLooper()).post {
							recording_button.visibility = View.INVISIBLE
							recording_button.isEnabled = false
							recording_button.isSelected = true
						}

					} else if(result == 7 && outputs[result] >= mAccuracy && !mFinishPoseDetect) {
						if (mAddressPoseDetect) {
							mFinishPoseDetect = true
						}
					} else if(result == 8 && outputs[result] >= mAccuracy && mFinishPoseDetect) {
						mEndToFinishDetect = true
					}
				}
				*/

				if(VERBOSE) Log.d(TAG, "Loading Images...... : $framecnt")
			}
		}

		mPlayerControlView?.show()

		// Media Looping
		mMediaPlayer?.isLooping = true

		// Set playback speed
		mMediaPlayer?.playbackParams = mMediaPlayer?.playbackParams!!.setSpeed(mPlaybackSpeed)

		// Playback pause
		mMediaPlayerControl.pause()

		// Set current position
		if(mCurrentPosition != 0){
			mMediaPlayerControl.seekTo(mCurrentPosition * 1000 / mDuration!!)
		}
	}

	private val mOnCompletionListener = MediaPlayer.OnCompletionListener {
		Log.d(
			TAG,
			"Playback Completed!!!! (MediaPlayer Current Position = ${mMediaPlayer?.currentPosition})"
		)

		if( !mMediaPlayer!!.isLooping ) {
			mMediaPlayerControl.seekTo(0)
			mSeekbar!!.progress = 0

			if(mThreadService != null)	{
				mThreadService?.shutdown()
				mThreadService = null
			}

			mMediaPlayer?.setWakeMode(context, PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
		}
	}

	private var mSurfaceCallback = object : SurfaceHolder.Callback {
		override fun surfaceCreated(holder: SurfaceHolder) {
			if(mMediaPlayer == null) {
				mMediaPlayer = MediaPlayer()
			} else {
				mMediaPlayer?.reset()
			}

			try{
				// Media Data loading
				val fs = FileInputStream(File(mArgs.fileUri))
				val fd = fs.fd

				mMediaPlayer?.setDataSource(fd)
				mMediaPlayer?.setDisplay(mSurfaceHolder)
				mMediaPlayer?.setOnPreparedListener(mOnPreparedListener)
				mMediaPlayer?.setOnCompletionListener(mOnCompletionListener)
				mMediaPlayer?.prepare()

				if(VERBOSE) {
					Log.d(
							TAG,
							"MediaPlayer Size : ${mMediaPlayer?.videoWidth} X ${mMediaPlayer?.videoHeight}"
					)
				}

				// SurfaceView Resizing (aspect ratio 4:3) only portrait mode
				val viewfinderSize = mSurfaceView?.layoutParams

				// Preview height aspect ratio ( 640 * 480 )
				val aspectRatio : Float = mMediaPlayer?.videoHeight!!.toFloat() / mMediaPlayer?.videoWidth!!.toFloat()
				val previewRatio : Float = PREVIEW_WIDTH.toFloat() / PREVIEW_HEIGHT.toFloat()

				if(aspectRatio > previewRatio) {
					val previewWidthRatio : Float = PREVIEW_HEIGHT.toFloat() / PREVIEW_WIDTH.toFloat()
					viewfinderSize?.width = (mSurfaceView?.width!!.toFloat() * previewWidthRatio).roundToInt()
					viewfinderSize?.height = (mSurfaceView?.width!!.toFloat() * previewRatio).roundToInt()
						(mSurfaceView?.width!!.toFloat() * aspectRatio).roundToInt()
				} else {
					viewfinderSize?.width = mSurfaceView?.width
					viewfinderSize?.height =
						(mSurfaceView?.width!!.toFloat() * aspectRatio).roundToInt()
				}
				mSurfaceView?.layoutParams = viewfinderSize

				if(VERBOSE) {
					Log.d(
							TAG,
							"SurfaceView Size : ${viewfinderSize?.width} X ${viewfinderSize?.height}"
					)
				}

				// Set Drawing View Size
				drawingView?.layoutParams = viewfinderSize

			} catch (e: java.lang.Exception) {
				Log.e(TAG, "Surface view error : ${e.message}")
			}
		}

		override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
		}

		override fun surfaceDestroyed(holder: SurfaceHolder) {
			if(VERBOSE) Log.d(TAG, "SurfaceDestroyed!!!!")
			if(mMediaPlayerControl.isPlaying) {
				mMediaPlayerControl.pause()
			}
			mCurrentPosition = mMediaPlayerControl.currentPosition
			mMediaPlayer?.release()
			mMediaPlayer = null
		}
	}

	private val brushMenuAdapter by lazy { DrawMenuAdapter(this) }
	private val brushMenuBalloon by lazy { BalloonUtils.getDrawMenuBalloon(requireContext(), this)}
	private val paletteMenuAdapter by lazy { DrawMenuAdapter(this) }
	private val paletteMenuBalloon by lazy { BalloonUtils.getDrawMenuBalloon(requireContext(), this)}

	private var drawingBoard: DrawingBoard? = null
	private var drawingView: CiDrawingView? = null

	private fun setupDrawingBoard() {
		drawingBoard!!.setupDrawingView(drawingView)
		drawingBoard!!.drawingContext.paint.color = Color.WHITE
		drawingBoard!!.drawingContext.paint.strokeWidth = 6f
		drawingBoard!!.drawingContext.drawingMode = PointerMode()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// This callback will only be called when MyFragment is at least Started.
		/*
		val callback: OnBackPressedCallback =
			object : OnBackPressedCallback(true /* enabled by default */) {
				override fun handleOnBackPressed() {
					// Handle the back button event

					// Launch camera fragment
					val action = MediaPlayerFragmentDirections.actionPlayerToPreview()
					mNavController.navigate(action)
				}
			}
		requireActivity().onBackPressedDispatcher.addCallback(this, callback)
		 */

		// gets Brush menu Balloon's recyclerView.
		val brushRecycler: RecyclerView =
			brushMenuBalloon.getContentView().findViewById(R.id.list_recyclerView)
		brushRecycler.adapter = brushMenuAdapter
		brushMenuAdapter.addMenuItems(ItemUtils.getBrushMenu(requireContext()))

		// gets Palette menu Balloon's recyclerView.
		val paletteRecycler: RecyclerView =
			paletteMenuBalloon.getContentView().findViewById(R.id.list_recyclerView)
		paletteRecycler.adapter = paletteMenuAdapter
		paletteMenuAdapter.addMenuItems(ItemUtils.getPaletteMenu(requireContext()))
	}

	override fun onCreateView(
			inflater: LayoutInflater,
			container: ViewGroup?,
			savedInstanceState: Bundle?
	): View? = inflater.inflate(R.layout.fragment_mediaplayer, container, false)

	@SuppressLint("CutPasteId")
	@RequiresApi(Build.VERSION_CODES.O)
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		// Set AdMob
		mAdView = view.findViewById(R.id.adView)
		val adRequest = AdRequest.Builder().build()
		mAdView.loadAd(adRequest)

		val fileUri = mArgs.fileUri.toUri()
		Log.d(TAG, "FileUri : $fileUri")

		mSurfaceView = view.findViewById(R.id.video_viewer)
		mPlayerControlView = view.findViewById(R.id.video_controller) as PlayerControlView

		drawingView = view.findViewById(R.id.drawing_view) as CiDrawingView

		mSurfaceHolder = mSurfaceView?.holder
		mSurfaceHolder?.addCallback(mSurfaceCallback)

		drawingBoard = DrawingBoardManager.getInstance().createDrawingBoard()
		setupDrawingBoard()
		drawingBoard?.elementManager!!.createNewLayer()
		drawingBoard?.elementManager!!.selectFirstVisibleLayer()

		/*
		menubar_back.setOnClickListener {
			// Launch camera fragment
			val action = MediaPlayerFragmentDirections.actionPlayerToPreview()
			mNavController.navigate(action)
		}
	 	*/

		menubar_home.setOnClickListener {
			// Launch MainActivity
			val intent = Intent(activity, MainActivity::class.java)
			startActivity(intent)
		}

		menubar_brush.setOnClickListener {
			brushMenuBalloon.showAlignBottom(it)
		}

		menubar_eraser.setOnClickListener {
			val mode = ObjectEraserMode()
			drawingBoard!!.drawingContext.drawingMode = mode

			menubar_brush.setImageResource(R.drawable.ic_brush_white)
		}

		menubar_select.setOnClickListener {
			val mode = PointerMode()
			mode.setSelectionMode(RectSelectionMode())
			drawingBoard!!.drawingContext.drawingMode = mode

			menubar_brush.setImageResource(R.drawable.ic_brush_white)
		}

		menubar_palette.setOnClickListener {
			paletteMenuBalloon.showAlignBottom(it)
		}

		menubar_undo.setOnClickListener {
			drawingBoard!!.operationManager.undo()
		}

		menubar_redo.setOnClickListener {
			drawingBoard!!.operationManager.redo()
		}
	}

	override fun onResume() {
		super.onResume()
		if(VERBOSE) Log.d(
				TAG,
				"MediaPlayerFragment Resume!!!!! (CurrentPosition : $mCurrentPosition)"
		)

	}

	override fun onPause() {
		super.onPause()
		if(VERBOSE) Log.d(
				TAG,
				"MediaPlayerFragment Pause!!!!! (CurrentPosition : $mCurrentPosition)"
		)
	}

	override fun onStop() {
		super.onStop()
		if(VERBOSE) Log.d(
				TAG,
				"MediaPlayerFragment Stop!!!!! (CurrentPosition : $mCurrentPosition)"
		)
	}

	override fun onDestroy() {
		super.onDestroy()
		if(VERBOSE) Log.d(
				TAG,
				"MediaPlayerFragment Destroy!!!!! (CurrentPosition : $mCurrentPosition)"
		)
	}

	override fun onDrawMenuItemClick(drawMenuItem: DrawMenuItem) {
		when(drawMenuItem.title){
			"Line", "Oval", "Rectangle", "Nothing" -> {
				val shape_mode = InsertShapeMode()
				drawingBoard!!.drawingContext.drawingMode = shape_mode

				when (drawMenuItem.title) {
					"Line" -> {
						menubar_brush.setImageResource(R.drawable.ic_line_white)
						shape_mode.setShapeType(LineElement::class.java)
					}
					"Oval" -> {
						menubar_brush.setImageResource(R.drawable.ic_circle_white)
						shape_mode.setShapeType(OvalElement::class.java)
					}
					"Rectangle" -> {
						menubar_brush.setImageResource(R.drawable.ic_rectangle_white)
						shape_mode.setShapeType(RectElement::class.java)
					}
					"Nothing" -> {
						menubar_brush.setImageResource(R.drawable.ic_brush_white)
						val pointer_mode = PointerMode()
						drawingBoard!!.drawingContext.drawingMode = pointer_mode
					}
				}
				brushMenuBalloon.dismiss()
			}
			"White", "Red", "Blue", "Yellow", "Green", "Black" -> {
				when (drawMenuItem.title) {
					"White" -> {
						menubar_palette.setColorFilter(Color.WHITE)
						drawingBoard!!.drawingContext.paint.color = Color.WHITE
					}
					"Red" -> {
						menubar_palette.setColorFilter(Color.RED)
						drawingBoard!!.drawingContext.paint.color = Color.RED
					}
					"Blue" -> {
						menubar_palette.setColorFilter(Color.BLUE)
						drawingBoard!!.drawingContext.paint.color = Color.BLUE
					}
					"Yellow" -> {
						menubar_palette.setColorFilter(Color.YELLOW)
						drawingBoard!!.drawingContext.paint.color = Color.YELLOW
					}
					"Green" -> {
						menubar_palette.setColorFilter(Color.GREEN)
						drawingBoard!!.drawingContext.paint.color = Color.GREEN
					}
					"Black" -> {
						menubar_palette.setColorFilter(Color.BLACK)
						drawingBoard!!.drawingContext.paint.color = Color.BLACK
					}
				}
				paletteMenuBalloon.dismiss()
			}
		}
	}

	companion object {
		private val TAG = MediaPlayerFragment::class.java.simpleName
		private const val VERBOSE = true 		// lots of logging
		private const val FASTSTEP = 1F
		private const val FASTSTEPINTERVAIL = 100L
	}
}
