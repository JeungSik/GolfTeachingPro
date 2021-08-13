package kr.co.anitex.golfteachingpro.videolistmanager

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import kr.co.anitex.golfteachingpro.R
import java.util.concurrent.TimeUnit

/**
 * Created by nitinagarwal on 3/12/17.
 */
@Suppress("DEPRECATION")
class VideoListAdapter(private val mContext: Context, private val mVideoListingLayout: Int) :
    ArrayAdapter<String?>(mContext, mVideoListingLayout) {
    private var bmp: Bitmap? = null
    private var videos: List<String>? = null
    private var mVideoListInfo: VideoListInfo? = null

    override fun getCount(): Int {
        return if (videos != null) videos!!.size else 0
    }

    @Suppress("NAME_SHADOWING")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val videoFullPath = videos!![position]
        var viewHolder = ViewHolder()
        if (convertView == null) {
            val layoutInflater =
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(mVideoListingLayout, parent, false)
            viewHolder.title = convertView.findViewById<View>(R.id.VideoTitleNew) as TextView
            viewHolder.resolution =
                convertView.findViewById<View>(R.id.VideoResolutionNew) as TextView
            viewHolder.size = convertView.findViewById<View>(R.id.VideoSizeNew) as TextView
            viewHolder.thumbnail =
                convertView.findViewById<View>(R.id.VideoThumbnailNew) as CustomImageView
            viewHolder.duration = convertView.findViewById<View>(R.id.VideoDurationNew) as TextView
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }
        viewHolder.title!!.text = videoFullPath
        val titleValue: String? = mVideoListInfo!!.videoTitleHashMap[videoFullPath]
        val durationValue: Long = mVideoListInfo!!.videoDurationHashMap[videoFullPath]!!.toLong()
        val sizeValue: Int? = mVideoListInfo!!.videoSizeHashMap[videoFullPath]
        val widthValue: Int? = mVideoListInfo!!.videoWidthHashMap[videoFullPath]
        val heightValue: Int? = mVideoListInfo!!.videoHeightHashMap[videoFullPath]
        val size: String = Converters.BytesToMb(sizeValue.toString())
        val resolution = widthValue.toString() + "X" + heightValue
        viewHolder.title!!.text = titleValue
        viewHolder.size!!.text = size
        viewHolder.duration!!.text = String.format("%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(durationValue),
            TimeUnit.MILLISECONDS.toMinutes(durationValue)
                    - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(durationValue)),
            TimeUnit.MILLISECONDS.toSeconds(durationValue)
                    - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationValue))
        )
        viewHolder.resolution!!.text = resolution
        val videoid: Int? = mVideoListInfo!!.videoIdHashMap[videoFullPath]
        val found: Bitmap? = BitmapCache.GetInstance()?.GetBitmapFromMemoryCache(videoFullPath)
        if (found != null) viewHolder.thumbnail?.setImageBitmap(found) else {
            if (ThumbnailCreateor.cancelPotentialWork(videoid, viewHolder.thumbnail)) {
                val task: ThumbnailCreateor.BitmapWorkerTask =
                    ThumbnailCreateor.BitmapWorkerTask(
                        viewHolder.thumbnail,
                        mContext.contentResolver,
                        videoFullPath
                    )
                val downloadedDrawable: ThumbnailCreateor.AsyncDrawable =
                    ThumbnailCreateor.AsyncDrawable(mContext.resources, bmp, task)
                viewHolder.thumbnail!!.setImageDrawable(downloadedDrawable)
                task.execute(videoid.toString(), videoFullPath)
            }
        }
        return convertView!!
    }

    @Suppress("UNCHECKED_CAST")
    fun bindVideoList(videoList: List<String?>?, videoListInfo: VideoListInfo?) {
        videos = videoList as List<String>?
        mVideoListInfo = videoListInfo
    }

    private inner class ViewHolder {
        var thumbnail: CustomImageView? = null
        var title: TextView? = null
        var resolution: TextView? = null
        var size: TextView? = null
        var duration: TextView? = null
    }
}