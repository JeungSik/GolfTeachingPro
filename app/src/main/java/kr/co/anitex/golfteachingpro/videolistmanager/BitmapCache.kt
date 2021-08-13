package kr.co.anitex.golfteachingpro.videolistmanager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import android.util.LruCache
import kr.co.anitex.golfteachingpro.TeachingProApp
import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Created by nitinagarwal on 11/25/15.
 */
@Suppress("DEPRECATION")
class BitmapCache private constructor() {
    private var mDiskLruCache: DiskLruCache?
    private val mDiskCacheLock = Object()
    private var mDiskCacheStarting = true
    private val mLruCache: LruCache<String, Bitmap?>
    fun Clear() {
        mLruCache.evictAll()
    }

    fun AddBitmapToCache(data: String, value: Bitmap?) {
        if (value == null) return
        if (mLruCache[data] == null) {
            mLruCache.put(data, value)
        }
        synchronized(mDiskCacheLock) {
            // Add to disk cache
            if (mDiskLruCache != null) {
                val key = hashKeyForDisk(data)
                var out: OutputStream? = null
                try {
                    val snapshot: DiskLruCache.Snapshot? = mDiskLruCache!![key]
                    if (snapshot == null) {
                        val editor: DiskLruCache.Editor? = mDiskLruCache!!.edit(key)
                        if (editor != null) {
                            out = editor.newOutputStream(DISK_CACHE_INDEX)
                            value.compress(
                                CompressFormat.JPEG,
                                DEFAULT_COMPRESS_QUALITY,
                                out
                            )
                            editor.commit()
                            out.close()
                        }
                    } else {
                        snapshot.getInputStream(DISK_CACHE_INDEX)!!.close()
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "addBitmapToCache - $e")
                } catch (e: Exception) {
                    Log.e(TAG, "addBitmapToCache - $e")
                } finally {
                    try {
                        out?.close()
                    } catch (e: IOException) {
                    }
                }
            }
        }
    }

    /**
     * Get from disk cache.
     *
     * @param data Unique identifier for which item to get
     * @return The bitmap if found in cache, null otherwise
     */
    fun getBitmapFromDiskCache(data: String): Bitmap? {
        //BEGIN_INCLUDE(get_bitmap_from_disk_cache)
        val key = hashKeyForDisk(data)
        var bitmap: Bitmap? = null
        synchronized(mDiskCacheLock) {
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait()
                } catch (e: InterruptedException) {
                }
            }
            if (mDiskLruCache != null) {
                var inputStream: InputStream? = null
                try {
                    val snapshot: DiskLruCache.Snapshot? = mDiskLruCache!![key]
                    if (snapshot != null) {
//                        if (BuildConfig.DEBUG) {
//                            Log.d(TAG, "Disk cache hit");
//                        }
                        inputStream =
                            snapshot.getInputStream(DISK_CACHE_INDEX)
                        if (inputStream != null) {
                            val fd =
                                (inputStream as FileInputStream?)!!.fd

                            // Decode bitmap, but we don't want to sample so give
                            // MAX_VALUE as the target dimensions
                            bitmap = decodeSampledBitmapFromDescriptor(
                                fd, Int.MAX_VALUE, Int.MAX_VALUE
                            )
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "getBitmapFromDiskCache - $e")
                } finally {
                    try {
                        inputStream?.close()
                    } catch (e: IOException) {
                    }
                }
            }
            return bitmap
        }
        //END_INCLUDE(get_bitmap_from_disk_cache)
    }

    fun GetBitmapFromMemoryCache(key: String): Bitmap? {
        return mLruCache[key]
    }

    @SuppressLint("StaticFieldLeak")
    internal inner class InitDiskCacheTask :
        AsyncTask<File?, Void?, Void?>() {
        override fun doInBackground(vararg p0: File?): Void? {
            synchronized(mDiskCacheLock) {
                val cacheDir = p0[0]
                try {
                    mDiskLruCache =
                            DiskLruCache.open(cacheDir!!, 1, 1, DISK_CACHE_SIZE.toLong())
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                mDiskCacheStarting = false // Finished initialization
                mDiskCacheLock.notifyAll() // Wake any waiting threads
            }
            return null
        }
    }

    companion object {
        private var mInstance: BitmapCache? = null
        private const val DISK_CACHE_SIZE = 1024 * 1024 * 10 // 10MB

        // Compression settings when writing images to disk cache
        private val DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG
        private const val DEFAULT_COMPRESS_QUALITY = 70
        private const val DISK_CACHE_INDEX = 0
        const val DISK_CACHE_SUBDIR = "thumbnails"
        private const val TAG = "BitmapCache2"

        fun GetInstance(): BitmapCache? {
            if (mInstance == null) {
                mInstance = BitmapCache()
            }
            return mInstance
        }

        private fun bytesToHexString(bytes: ByteArray): String {
            // http://stackoverflow.com/questions/332079
            val sb = StringBuilder()
            for (i in bytes.indices) {
                val hex = Integer.toHexString(0xFF and bytes[i].toInt())
                if (hex.length == 1) {
                    sb.append('0')
                }
                sb.append(hex)
            }
            return sb.toString()
        }

        /**
         * A hashing method that changes a string (like a URL) into a hash suitable for using as a
         * disk filename.
         */
        fun hashKeyForDisk(key: String): String {
            val cacheKey: String
            cacheKey = try {
                val mDigest = MessageDigest.getInstance("MD5")
                mDigest.update(key.toByteArray())
                bytesToHexString(mDigest.digest())
            } catch (e: NoSuchAlgorithmException) {
                key.hashCode().toString()
            }
            return cacheKey
        }

        fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int, reqHeight: Int
        ): Int {
            // BEGIN_INCLUDE (calculate_sample_size)
            // Raw height and width of image
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize > reqHeight
                    && halfWidth / inSampleSize > reqWidth
                ) {
                    inSampleSize *= 2
                }

                // This offers some additional logic in case the image has a strange
                // aspect ratio. For example, a panorama may have a much larger
                // width than height. In these cases the total pixels might still
                // end up being too large to fit comfortably in memory, so we should
                // be more aggressive with sample down the image (=larger inSampleSize).
                var totalPixels = width * height / inSampleSize.toLong()

                // Anything more than 2x the requested pixels we'll sample down further
                val totalReqPixelsCap = reqWidth * reqHeight * 2.toLong()
                while (totalPixels > totalReqPixelsCap) {
                    inSampleSize *= 2
                    totalPixels /= 2
                }
            }
            return inSampleSize
            // END_INCLUDE (calculate_sample_size)
        }

        fun decodeSampledBitmapFromDescriptor(
            fileDescriptor: FileDescriptor?, reqWidth: Int, reqHeight: Int
        ): Bitmap {

            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
        }

        // Creates a unique subdirectory of the designated app cache directory. Tries to use external
        // but if not mounted, falls back on internal storage.
        fun getDiskCacheDir(context: Context, uniqueName: String): File {
            // Check if media is mounted or storage is built-in, if so, try and use external cache dir
            // otherwise use internal cache dir
            val cachePath = context.cacheDir.path
            return File(cachePath + File.separator + uniqueName)
        }
    }

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        mDiskLruCache = null
        val cacheDir =
            getDiskCacheDir(TeachingProApp.appContext!!, DISK_CACHE_SUBDIR)
        InitDiskCacheTask().execute(cacheDir)
        mLruCache = object : LruCache<String, Bitmap?>(cacheSize) {
            override fun sizeOf(key: String?, value: Bitmap?): Int {
                return value!!.byteCount / 1024
            }
        }
    }
}
