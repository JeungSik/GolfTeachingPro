package kr.co.anitex.golfteachingpro

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.media.*
import android.media.MediaCodecInfo.CodecCapabilities
import android.util.Log
import org.opencv.core.Mat
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * Generates a series of video frames, encodes them, decodes them, and tests for
 * significant divergence from the original.
 */
@Suppress("DEPRECATION")
class MediaEncoder(outputFile: File?, orientation: Int) {
	// I-frames
	// size of a frame, in pixels
	private var mWidth = -1
	private var mHeight = -1

	// bit rate, in bits per second
	private var mBitRate = -1

	// largest color component delta seen (i.e. actual vs. expected)
	private var mLargestColorDelta = 0
	private var mOutputFile: File? = null
	private var mEncoder: MediaCodec? = null
	private var mMuxer: MediaMuxer? = null
	private var mTrackIndex = 0
	private var mMuxerStarted = false
	private var mOrientation: Int = Configuration.ORIENTATION_UNDEFINED

	//private lateinit var mFrames: ArrayList<Mat>
	private lateinit var mFrame: Mat
	private lateinit var mFrameData: ByteArray
	private lateinit var mEncoderInputBuffers: Array<ByteBuffer>
	private lateinit var mBufferInfo: MediaCodec.BufferInfo
	private var mGenerateIndex = 0

	/**
	 * Tests streaming of AVC video through the encoder and decoder. Data is
	 * encoded from a series of byte[] buffers and decoded into Surfaces. The
	 * output is checked for validity.
	 */
	@Throws(Throwable::class)
	fun initMediaEncoder(
		width: Int, height: Int,
		bitRate: Int
	) {
		setParameters(width, height, bitRate)
		configEncoderMuxer()
	}

	/**
	 * Sets the desired frame size and bit rate.
	 */
	private fun setParameters(width: Int, height: Int, bitRate: Int) {
		if (width % 16 != 0 || height % 16 != 0) {
			Log.w(TAG, "WARNING: width or height not multiple of 16")
		}
		mWidth = width
		mHeight = height
		mBitRate = bitRate
	}

	/**
	 * Tests encoding and subsequently decoding video from frames generated into
	 * a buffer.
	 */
	@SuppressLint("InlinedApi")
	@Throws(Exception::class)
	fun configEncoderMuxer() {
		mLargestColorDelta = -1
		//var result = true
		try {
			val codecInfo = selectCodec(MIME_TYPE)
			if (codecInfo == null) {
				// Don't fail CTS if they don't have an AVC codec
				Log.e(TAG, "Unable to find an appropriate codec for $MIME_TYPE")
			}
			if (VERBOSE) Log.d(TAG, "found codec: " + codecInfo!!.name)
			val colorFormat: Int = try {
				selectColorFormat(codecInfo!!, MIME_TYPE)
			} catch (e: Exception) {
				CodecCapabilities.COLOR_FormatYUV420SemiPlanar
			}
			if (VERBOSE) Log.d(TAG, "found colorFormat: $colorFormat")
			// We avoid the device-specific limitations on width and height by
			// using values that
			// are multiples of 16, which all tested devices seem to be able to
			// handle.
			val format = MediaFormat.createVideoFormat(
				MIME_TYPE,
				mWidth, mHeight
			)
			// Set some properties. Failing to specify some of these can cause
			// the MediaCodec
			// configure() call to throw an unhelpful exception.
			format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
			format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate)
			format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
			format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
			format.getInteger(MediaFormat.KEY_ROTATION, 90)
			//format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 14000000)

			if (VERBOSE) Log.d(TAG, "format: $format")

			// Create a MediaCodec for the desired codec, then configure it as
			// an encoder with
			// our desired properties.
			mEncoder = MediaCodec.createByCodecName(codecInfo!!.name)

			try {
				mEncoder!!.configure(
					format, null, null,
					MediaCodec.CONFIGURE_FLAG_ENCODE
				)
			} catch(e: MediaCodec.CodecException) {
				format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, -1)

				mEncoder!!.configure(
					format, null, null,
					MediaCodec.CONFIGURE_FLAG_ENCODE
				)
			}

			mEncoder!!.start()

			// Create a MediaCodec for the decoder, just based on the MIME type.
			// The various
			// format details will be passed through the csd-0 meta-data later
			// on.
			val outputPath = mOutputFile!!.absolutePath
			try {
				mMuxer = MediaMuxer(
					outputPath,
					MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
				)
				if(mOrientation == Configuration.ORIENTATION_PORTRAIT)
					mMuxer!!.setOrientationHint(90)
			} catch (ioe: IOException) {
				// throw new RuntimeException("MediaMuxer creation failed",
				// ioe);
				ioe.printStackTrace()
			}

			mFrameData = ByteArray(mWidth * mHeight * 3 / 2)
			mEncoderInputBuffers = mEncoder!!.inputBuffers
			mBufferInfo = MediaCodec.BufferInfo()

		} finally {
			if (VERBOSE) Log.i(TAG, "Largest color delta: $mLargestColorDelta")
		}
	}

	private fun closeEncoderMuxer() {
		if (mEncoder != null) {
			mEncoder!!.stop()
			mEncoder!!.release()
			mEncoder = null
		}
		if (mMuxer != null) {
			mMuxer!!.stop()
			mMuxer!!.release()
			mMuxer = null
		}
	}

	/**
	 * Does the actual work for encoding frames from buffers of byte[].
	 */
	@SuppressLint("InlinedApi")
	fun doEncodeVideoFromBuffer(frame: Mat, endOfStream: Boolean): Boolean {
		mFrame = frame

		// If we're not done submitting frames, generate a new one and submit
		// it. By
		// doing this on every loop we're working to ensure that the encoder
		// always has
		// work to do.
		val inputBufIndex = mEncoder!!.dequeueInputBuffer(TIMEOUT_USEC.toLong())
		if (inputBufIndex >= 0) {
			val ptsUsec = computePresentationTime(mGenerateIndex)

			if (endOfStream) {
				// Send an empty frame with the end-of-stream flag set. If
				// we set EOS
				// on a frame with data, that frame data will be ignored,
				// and the
				// output will be short one frame.
				mEncoder!!.queueInputBuffer(
					inputBufIndex, 0, 0, ptsUsec,
					MediaCodec.BUFFER_FLAG_END_OF_STREAM
				)
				drainEncoder(true)
				closeEncoderMuxer()
			} else {
				try {
					generateFrame(/*mGenerateIndex*/)
				} catch (e: Exception) {
					Log.i(TAG, "meet a different type of image")
					Arrays.fill(mFrameData, 0.toByte())
				}
				if (VERBOSE) Log.i(TAG, "GenerateIndex : $mGenerateIndex")
				val inputBuf = mEncoderInputBuffers[inputBufIndex]
				// the buffer should be sized to hold one full frame
				inputBuf.clear()
				inputBuf.put(mFrameData)
				mEncoder!!.queueInputBuffer(
					inputBufIndex, 0,
					mFrameData.size, ptsUsec, 0
				)
				drainEncoder(false)
				mGenerateIndex++
			}
		} else {
			// either all in use, or we timed out during initial setup
			if (VERBOSE) Log.i(TAG, "input buffer not available($inputBufIndex)")
		}

		return true
	}

	/**
	 * use Muxer to generate mp4 file with data from encoder
	 *
	 * @param endOfStream
	 * if this is the last frame
	 * @param mBufferInfo
	 * the BufferInfo of data from encoder
	 */
	private fun drainEncoder(endOfStream: Boolean) {
		if (endOfStream) {
			try {
				mEncoder!!.signalEndOfInputStream()
			} catch (e: Exception) {
			}
		}
		var encoderOutputBuffers = mEncoder!!.outputBuffers
		while (true) {
			val encoderStatus = mEncoder!!.dequeueOutputBuffer(
				mBufferInfo,
				TIMEOUT_USEC.toLong()
			)

			if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
				// no output available yet
				if (!endOfStream) {
					break // out of while
				} else {
					if (VERBOSE) Log.i(TAG, "no output available, spinning to await EOS")
				}
			} else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				// not expected for an encoder
				encoderOutputBuffers = mEncoder!!.outputBuffers
			} else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				// should happen before receiving buffers, and should only
				// happen once
				if (mMuxerStarted) {
					throw RuntimeException("format changed twice")
				}
				val newFormat = mEncoder!!.outputFormat
				if (VERBOSE) Log.i(TAG, "encoder output format changed: $newFormat")

				// now that we have the Magic Goodies, start the muxer
				mTrackIndex = mMuxer!!.addTrack(newFormat)
				mMuxer!!.start()
				mMuxerStarted = true
			} else if (encoderStatus < 0) {
				if (VERBOSE) Log.i(
					TAG, "unexpected result from encoder.dequeueOutputBuffer: "
							+ encoderStatus
				)
			} else {
				val encodedData = encoderOutputBuffers[encoderStatus]
					?: throw RuntimeException(
						"encoderOutputBuffer "
								+ encoderStatus + " was null"
					)
				if (mBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
					// The codec config data was pulled out and fed to the muxer
					// when we got
					// the INFO_OUTPUT_FORMAT_CHANGED status. Ignore it.
					if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG")
					mBufferInfo.size = 0
				}
				if (mBufferInfo.size != 0) {
					if (!mMuxerStarted) {
						throw RuntimeException("muxer hasn't started")
					}

					// adjust the ByteBuffer values to match BufferInfo
					encodedData.position(mBufferInfo.offset)
					encodedData.limit(mBufferInfo.offset + mBufferInfo.size)
					if (VERBOSE) Log.d(
						TAG, "BufferInfo: offset=" + mBufferInfo.offset
								+ ", size=" + mBufferInfo.size
								+ ", time=" + mBufferInfo.presentationTimeUs
					)
					try {
						mMuxer!!.writeSampleData(
							mTrackIndex, encodedData,
							mBufferInfo
						)
					} catch (e: Exception) {
						Log.i(TAG, "Too many frames")
					}
				}
				mEncoder!!.releaseOutputBuffer(encoderStatus, false)
				if (mBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
					if (!endOfStream) {
						if (VERBOSE) Log.i(TAG, "reached end of stream unexpectedly")
					} else {
						if (VERBOSE) Log.i(TAG, "end of stream reached")
					}
					if (VERBOSE) Log.d(TAG, "mBufferInfo.flage : ${mBufferInfo.flags}")
					break // out of while
				}
			}
		}
	}

	/**
	 * Generates data for frame N into the supplied buffer.
	 */
	private fun generateFrame(/*frameIndex: Int*/) {
		// Set to zero. In YUV this is a dull green.
		Arrays.fill(mFrameData, 0.toByte())
		val dst: Mat = mFrame

		//Mat dst = new Mat(mWidth, mHeight * 3 / 2, CvType.CV_8UC1);
		//Mat dst = new Mat();
		//Imgproc.cvtColor(mat, dst, Imgproc.COLOR_RGBA2YUV_I420);

		// use array instead of mat to improve the speed
		dst.get(0, 0, mFrameData)
		val temp = mFrameData.clone()
		val margin = mHeight / 4
		var location = mHeight
		var step = 0
		for (i in mHeight until mHeight + margin) {
			for (j in 0 until mWidth) {
				val uValue = temp[i * mWidth + j]
				val vValue = temp[(i + margin) * mWidth + j]
				mFrameData[location * mWidth + step] = uValue
				mFrameData[location * mWidth + step + 1] = vValue
				step += 2
				if (step >= mWidth) {
					location++
					step = 0
				}
			}
		}
	}

	companion object {
		private const val TAG = "MediaEncoder"
		private const val VERBOSE = true 			// lots of logging

		private const val TIMEOUT_USEC = 10000

		/**
		 * Returns the first codec capable of encoding the specified MIME type, or
		 * null if no match was found.
		 */
		private fun selectCodec(mimeType: String): MediaCodecInfo? {
			val numCodecs = MediaCodecList.getCodecCount()
			for (i in 0 until numCodecs) {
				val codecInfo = MediaCodecList.getCodecInfoAt(i)
				if (!codecInfo.isEncoder) {
					continue
				}
				val types = codecInfo.supportedTypes
				for (j in types.indices) {
					if (types[j].equals(mimeType, ignoreCase = true)) {
						return codecInfo
					}
				}
			}
			return null
		}

		/**
		 * Returns a color format that is supported by the codec and by this test
		 * code. If no match is found, this throws a test failure -- the set of
		 * formats known to the test should be expanded for new platforms.
		 */
		private fun selectColorFormat(
			codecInfo: MediaCodecInfo,
			mimeType: String
		): Int {
			val capabilities = codecInfo
				.getCapabilitiesForType(mimeType)
			for (i in capabilities.colorFormats.indices) {
				val colorFormat = capabilities.colorFormats[i]
				//if (isRecognizedFormat(colorFormat)) {
				if (colorFormat == CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
					return colorFormat
				}
			}
			return 0 // not reached
		}

		/**
		 * Returns true if this is a color format that this test code understands
		 * (i.e. we know how to read and generate frames in this format).
		 */
		private fun isRecognizedFormat(colorFormat: Int): Boolean {
			return when (colorFormat) {
				CodecCapabilities.COLOR_FormatYUV420Planar,
				CodecCapabilities.COLOR_FormatYUV420PackedPlanar,
				CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
				CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar,
				CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar -> true
				else -> false
			}
		}

		/**
		 * Generates the presentation time for frame N, in microseconds.
		 */
		private fun computePresentationTime(frameIndex: Int): Long {
			val value = frameIndex.toLong()
			return 132 + value * 1000000 / FRAME_RATE
		}
	}

	init {
		this.mOutputFile = outputFile
		this.mOrientation = orientation
	}
}