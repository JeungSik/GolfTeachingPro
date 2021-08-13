/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package kr.co.anitex.golfteachingpro.utils

import android.graphics.*
import android.media.Image
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


/** Utility class for manipulating images.  */
object ImageUtils {
  // This value is 2 ^ 18 - 1, and is used to hold the RGB values together before their ranges
  // are normalized to eight bits.
  private const val MAX_CHANNEL_VALUE = 262143

  /** Helper function to convert y,u,v integer values to RGB format */
  private fun convertYUVToRGB(y: Int, u: Int, v: Int): Int {
    // Adjust and check YUV values
    val yNew = if (y - 16 < 0) 0 else y - 16
    val uNew = u - 128
    val vNew = v - 128
    val expandY = 1192 * yNew
    var r = expandY + 1634 * vNew
    var g = expandY - 833 * vNew - 400 * uNew
    var b = expandY + 2066 * uNew

    // Clipping RGB values to be inside boundaries [ 0 , MAX_CHANNEL_VALUE ]
    val checkBoundaries = { x: Int ->
      when {
        x > MAX_CHANNEL_VALUE -> MAX_CHANNEL_VALUE
        x < 0 -> 0
        else -> x
      }
    }
    r = checkBoundaries(r)
    g = checkBoundaries(g)
    b = checkBoundaries(b)
    return -0x1000000 or (r shl 6 and 0xff0000) or (g shr 2 and 0xff00) or (b shr 10 and 0xff)
  }

  /** Converts YUV420 format image data (ByteArray) into ARGB8888 format with IntArray as output. */
  fun convertYUV420ToARGB8888(
    yData: ByteArray,
    uData: ByteArray,
    vData: ByteArray,
    width: Int,
    height: Int,
    yRowStride: Int,
    uvRowStride: Int,
    uvPixelStride: Int,
    out: IntArray
  ) {
    var outputIndex = 0
    for (j in 0 until height) {
      val positionY = yRowStride * j
      val positionUV = uvRowStride * (j shr 1)

      for (i in 0 until width) {
        val uvOffset = positionUV + (i shr 1) * uvPixelStride

        // "0xff and" is used to cut off bits from following value that are higher than
        // the low 8 bits
        out[outputIndex] = convertYUVToRGB(
          0xff and yData[positionY + i].toInt(), 0xff and uData[uvOffset].toInt(),
          0xff and vData[uvOffset].toInt()
        )
        outputIndex += 1
      }
    }
  }

  fun imageToMat(image: Image): Mat {
    var buffer: ByteBuffer
    var rowStride: Int
    var pixelStride: Int
    val width = image.width
    val height = image.height
    var offset = 0
    val planes = image.planes
    val data =
      ByteArray(image.width * image.height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)
    val rowData = ByteArray(planes[0].rowStride)
    val bytesPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8

    for (i in planes.indices) {
      buffer = planes[i].buffer
      rowStride = planes[i].rowStride
      pixelStride = planes[i].pixelStride
      val w = if (i == 0) width else width / 2
      val h = if (i == 0) height else height / 2
      val length = w * bytesPerPixel

      for (row in 0 until h) {
        if (pixelStride == bytesPerPixel) {
          buffer[data, offset, length]

          // Advance buffer the remainder of the row stride, unless on the last row.
          // Otherwise, this will throw an IllegalArgumentException because the buffer
          // doesn't include the last padding.
          //if (h - row != 1) {
          //  buffer.position(buffer.position() + rowStride - length)
          //}
          offset += length
        } else {
          // On the last row only read the width of the image minus the pixel stride
          // plus one. Otherwise, this will throw a BufferUnderflowException because the
          // buffer doesn't include the last padding.
          if (h - row == 1) {
            buffer[rowData, 0, width - pixelStride + 1]
          } else {
            buffer[rowData, 0, rowStride]
          }
          for (col in 0 until w) {
            data[offset++] = rowData[col * pixelStride]
          }
        }
      }
      buffer.rewind()
    }

    // Finally, create the Mat.
    val mat = Mat(height + height / 2, width, CvType.CV_8UC1)
    mat.put(0, 0, data)
    return mat
  }

  fun imageToMatDegree180(image: Image): Mat {
    var buffer: ByteBuffer
    var rowStride: Int
    var pixelStride: Int
    val width = image.width
    val height = image.height
    var offset = 0
    val planes = image.planes
    val data =
      ByteArray(image.width * image.height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)
    val rowData = ByteArray(planes[0].rowStride)
    val bytesPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8

    for (i in planes.indices) {
      buffer = planes[i].buffer
      rowStride = planes[i].rowStride
      pixelStride = planes[i].pixelStride
      val w = if (i == 0) width else width / 2
      val h = if (i == 0) height else height / 2
      val length = w * bytesPerPixel

      for (row in 0 until h) {
        if (pixelStride == bytesPerPixel) {
          buffer[data, offset, length]

          // Advance buffer the remainder of the row stride, unless on the last row.
          // Otherwise, this will throw an IllegalArgumentException because the buffer
          // doesn't include the last padding.
          //if (h - row != 1) {
          //  buffer.position(buffer.position() + rowStride - length)
          //}
          offset += length
        } else {
          // On the last row only read the width of the image minus the pixel stride
          // plus one. Otherwise, this will throw a BufferUnderflowException because the
          // buffer doesn't include the last padding.
          if (h - row == 1) {
            buffer[rowData, 0, width - pixelStride + 1]
          } else {
            buffer[rowData, 0, rowStride]
          }
          for (col in 0 until w) {
            data[offset++] = rowData[col * pixelStride]
          }
        }
      }
      buffer.rewind()
    }

    // rotate 180 degree
    val data180 = rotateYUV420Degree180(data, width, height)

    // Finally, create the Mat.
    val mat = Mat(height + height / 2, width, CvType.CV_8UC1)
    mat.put(0, 0, data180)
    return mat
  }

  private fun rotateYUV420Degree180(
    data: ByteArray,
    imageWidth: Int,
    imageHeight: Int
  ): ByteArray {
    val defaultSize = imageWidth * imageHeight
    val yuv = ByteArray(defaultSize * 3 / 2)
    var count = 0
    // rotate Y ection
    for(i in defaultSize - 1 downTo 0)
      yuv[count++] = data[i]
    // rotate U section
    for(i in defaultSize+(defaultSize/4)-1 downTo defaultSize)
      yuv[count++] = data[i]
    // rotate V section
    for(i in defaultSize * 3 / 2 - 1 downTo defaultSize+(defaultSize/4))
      yuv[count++] = data[i]

    return yuv
  }

  fun imgToBitmap(image: Image): Bitmap? {
    val planes = image.planes
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer[nv21, 0, ySize]
    vBuffer[nv21, ySize, vSize]
    uBuffer[nv21, ySize + vSize, uSize]
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)
    val imageBytes = out.toByteArray()

    yBuffer.rewind()
    uBuffer.rewind()
    vBuffer.rewind()

    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
  }

}
