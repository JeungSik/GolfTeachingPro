package kr.co.anitex.golfteachingpro.videolistmanager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import kr.co.anitex.golfteachingpro.R
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by nitinagarwal on 3/15/17.
 */

class FolderListAdapter(var mContext: Context?) : BaseExpandableListAdapter() {
    private val bmp: Bitmap
    private var mFolderListHaspMap: HashMap<String, List<String>>? = null
    private var mFolderNames: ArrayList<String>? = null
    private var mVideoListInfo: VideoListInfo? = null

    fun bindVideoList(
        folderListHashMap: HashMap<String, List<String>>,
        folderNames: ArrayList<String>?,
        videoListInfo: VideoListInfo?
    ) {
        mFolderListHaspMap = folderListHashMap
        mFolderNames = folderNames
        mVideoListInfo = videoListInfo
    }

    override fun getGroupCount(): Int {
        return if (mFolderNames == null) 0 else mFolderNames!!.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return if (mFolderListHaspMap == null) 0 else mFolderListHaspMap!![mFolderNames!![groupPosition]]!!.size
    }

    override fun getGroup(groupPosition: Int): Any {
        return mFolderNames!![groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return mFolderListHaspMap!![mFolderNames!![groupPosition]]!![childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    @Suppress("NAME_SHADOWING")
    @SuppressLint("SetTextI18n")
    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        var convertView = convertView
        val group_title = getGroup(groupPosition) as String
        val lastIndexOf = group_title.lastIndexOf("/")
        val lastPartOfTitle = group_title.substring(lastIndexOf + 1)
        val videoCount = getChildrenCount(groupPosition)

        //lastPartOfTitle=lastPartOfTitle + "      " + videoCount;
        if (convertView == null) {
            val layoutInflater =
                mContext!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.tab_folderlist_parent, parent, false)
        }

        val parent_txtView = convertView!!.findViewById<View>(R.id.parent_txt) as TextView
        val folderVideoCount = convertView.findViewById<View>(R.id.videoCount) as TextView
        val folderPath_txtView = convertView.findViewById<View>(R.id.folder_path) as TextView
        parent_txtView.text = lastPartOfTitle
        folderVideoCount.text = videoCount.toString() + ""
        val folderPath = group_title.substring(0, lastIndexOf)
        folderPath_txtView.text = folderPath
        return convertView
    }

    @Suppress("NAME_SHADOWING")
    @SuppressLint("SetTextI18n")
    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View? {
        var convertView = convertView
        val videoFullPath = getChild(groupPosition, childPosition) as String
        val viewHolder: ViewHolder
        if (convertView == null) {
            val layoutInflater = mContext!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.tab_child, parent, false)
            viewHolder = ViewHolder()
            viewHolder.title = convertView.findViewById<View>(R.id.VideoTitleNew) as TextView
            viewHolder.resolution = convertView.findViewById<View>(R.id.VideoResolutionNew) as TextView
            viewHolder.size = convertView.findViewById<View>(R.id.VideoSizeNew) as TextView
            viewHolder.thumbnail = convertView.findViewById<View>(R.id.VideoThumbnailNew) as CustomImageView
            viewHolder.duration = convertView.findViewById<View>(R.id.VideoDurationNew) as TextView
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }
        viewHolder.title!!.text = mVideoListInfo!!.videoTitleHashMap[videoFullPath]
        val SizeInMb = Converters.BytesToMb(mVideoListInfo!!.videoSizeHashMap[videoFullPath].toString())
        viewHolder.size!!.text = SizeInMb
        val width: Int = mVideoListInfo!!.videoWidthHashMap[videoFullPath]!!
        val height: Int = mVideoListInfo!!.videoHeightHashMap[videoFullPath]!!
        viewHolder.resolution!!.text = width.toString() + "x" + height
        val durationOfVideoLong: Long = mVideoListInfo!!.videoDurationHashMap[videoFullPath]!!.toLong()
        viewHolder.duration!!.text = String.format("%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(durationOfVideoLong),
            TimeUnit.MILLISECONDS.toMinutes(durationOfVideoLong)
                    - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(durationOfVideoLong)),
            TimeUnit.MILLISECONDS.toSeconds(durationOfVideoLong)
                    - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationOfVideoLong))
        )
        val videoid: Int? = mVideoListInfo!!.videoIdHashMap[videoFullPath]
        val found = BitmapCache.GetInstance()!!.GetBitmapFromMemoryCache(videoFullPath)
        if (found != null) viewHolder.thumbnail!!.setImageBitmap(found)
        else {
            if (ThumbnailCreateor.cancelPotentialWork(videoid, viewHolder.thumbnail)) {
                val task = ThumbnailCreateor.BitmapWorkerTask(
                    viewHolder.thumbnail,
                    mContext!!.contentResolver,
                    videoFullPath
                )
                val downloadedDrawable =
                    ThumbnailCreateor.AsyncDrawable(mContext!!.resources, bmp, task)
                viewHolder.thumbnail!!.setImageDrawable(downloadedDrawable)
                task.execute(videoid.toString(), videoFullPath)
            }
        }
        return convertView
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    private inner class ViewHolder {
        var thumbnail: CustomImageView? = null
        var title: TextView? = null
        var resolution: TextView? = null
        var size: TextView? = null
        var duration: TextView? = null
    }

    init {
        val w = 100
        val h = 100
        val conf = Bitmap.Config.ARGB_8888 // see other conf types
        bmp = Bitmap.createBitmap(w, h, conf)
    }
}
