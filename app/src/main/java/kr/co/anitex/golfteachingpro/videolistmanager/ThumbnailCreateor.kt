@file:Suppress("DEPRECATION")

package kr.co.anitex.golfteachingpro.videolistmanager

import android.content.ContentResolver
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.provider.MediaStore
import android.widget.ImageView
import kr.co.anitex.golfteachingpro.TeachingProApp
import java.lang.ref.WeakReference

/**
 * Created by nitinagarwal on 10/29/15.
 */
object ThumbnailCreateor {
    private fun getBitmapWorkerTask(imageView: ImageView?): BitmapWorkerTask? {
        if (imageView != null) {
            val drawable = imageView.drawable
            if (drawable is AsyncDrawable) {
                return drawable.bitmapWorkerTask
            }
        }
        return null
    }

    fun cancelPotentialWork(data: Int?, imageView: ImageView?): Boolean {
        val bitmapWorkerTask = getBitmapWorkerTask(imageView)
        if (bitmapWorkerTask != null) {
            val bitmapData = bitmapWorkerTask.data
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == 0L || bitmapData != data!!.toLong()) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true)
            } else {
                // The same work is already in progress
                return false
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true
    }

    @Suppress("DEPRECATION")
    class BitmapWorkerTask(view: CustomImageView?, cr: ContentResolver, url: String) :
        AsyncTask<String?, Void?, Bitmap?>() {
        var data: Long = 0
        private var videoData: String? = null
        private val imageViewReference: WeakReference<ImageView>?
        private val cr: ContentResolver
        private val url: String

        override fun doInBackground(vararg p0: String?): Bitmap? {
            data = p0[0]!!.toLong()
            videoData = p0[1]
            val found = BitmapCache.GetInstance()!!.getBitmapFromDiskCache(videoData!!)
            if (found != null) return found
            var bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                cr,
                data, MediaStore.Video.Thumbnails.MINI_KIND, null
            )
            bitmap =
                BitmapUtil.scaleDownBitmap(TeachingProApp.appContext!!, bitmap, 105)
            return bitmap
        }

        override fun onPostExecute(bm: Bitmap?) {
            BitmapCache.GetInstance()!!.AddBitmapToCache(url, bm)
            if (imageViewReference != null) {
                val imageView = imageViewReference.get()
                if (imageView != null) {
                    val bitmapDownloaderTask = getBitmapWorkerTask(imageView)
                    if (this === bitmapDownloaderTask) {
                        imageView.setImageBitmap(bm)
                    }
                }
            }
        }

        init {
            imageViewReference = WeakReference(view)
            this.cr = cr
            this.url = url
        }
    }

    class AsyncDrawable(
        res: Resources?, bitmap: Bitmap?,
        bitmapWorkerTask: BitmapWorkerTask
    ) : BitmapDrawable(res, bitmap) {
        private val bitmapWorkerTaskReference: WeakReference<BitmapWorkerTask>
        val bitmapWorkerTask: BitmapWorkerTask?
            get() = bitmapWorkerTaskReference.get()

        init {
            bitmapWorkerTaskReference = WeakReference(bitmapWorkerTask)
        }
    }
}
