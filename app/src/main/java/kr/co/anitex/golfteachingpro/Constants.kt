/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("Constants")

package kr.co.anitex.golfteachingpro

import android.hardware.camera2.CameraCharacteristics

/** Request camera and external storage permission.   */
const val REQUEST_CAMERA_PERMISSION = 1

/** Model input shape for images.   */
const val MODEL_WIDTH = 257
const val MODEL_HEIGHT = 257

/** A shape for extracting frame data.   */
const val PREVIEW_WIDTH = 640
const val PREVIEW_HEIGHT = 480
const val MINIMUM_PREVIEW_SIZE = 480
const val RECORDER_VIDEO_BITRATE: Int = 12582912      // 1.2Mbps
//const val RECORDER_VIDEO_BITRATE: Int = 2097152       // 2Mbps
//const val RECORDER_VIDEO_BITRATE: Int = 2621440       // 2.5Mbps

const val AI_IMAGE_SIZE = 160
const val AI_SEQUENCE_LEN = 4
const val AI_FRAME_SIZE = 3*AI_IMAGE_SIZE*AI_IMAGE_SIZE
const val AI_ACCURACY = 0.85F
const val SCALE_WIDTH = (AI_IMAGE_SIZE.toFloat() / PREVIEW_WIDTH.toFloat())
const val SCALE_HEIGHT = (AI_IMAGE_SIZE.toFloat() / PREVIEW_HEIGHT.toFloat())

        /** Camera face directions   */
const val BACKSIDE_CAMERA = CameraCharacteristics.LENS_FACING_BACK
const val FRONTSIDE_CAMERA = CameraCharacteristics.LENS_FACING_FRONT

// parameters for the video encoder
const val MIME_TYPE = "video/avc" 	// H.264 Advanced Video
const val FRAME_RATE = 30 			// Camera fps
const val IFRAME_INTERVAL = 0 		// I-Frame interval

const val APP_NAME = "Golf Teaching Pro"
