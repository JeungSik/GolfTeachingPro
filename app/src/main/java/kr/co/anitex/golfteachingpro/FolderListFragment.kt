package kr.co.anitex.golfteachingpro

import android.os.Bundle
import android.view.*
import android.widget.ExpandableListView
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import kr.co.anitex.golfteachingpro.observablescrollview.ObservableScrollViewCallbacks
import kr.co.anitex.golfteachingpro.observablescrollview.ScrollState
import kr.co.anitex.golfteachingpro.videolistmanager.FolderListFragmentViewImpl
import kr.co.anitex.golfteachingpro.videolistmanager.VideoListFragmentInterface
import kr.co.anitex.golfteachingpro.videolistmanager.VideoListInfo
import kr.co.anitex.golfteachingpro.videolistmanager.VideoUserInteraction
import java.util.*

/**
 * Created by nitinagarwal on 3/15/17.
 */
@Suppress("DEPRECATION")
class FolderListFragment : Fragment(), VideoListFragmentInterface {
    private var mFolderListFragmentView: FolderListFragmentViewImpl? = null
    private var mVideoListInfo: VideoListInfo? = null
    private var mFolderNames: ArrayList<String>? = null
    private var mCallback: VideoUserInteraction? = null
    var mObservableScrollViewCallbacks: ObservableScrollViewCallbacks? = null

    @Nullable
    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        mFolderListFragmentView = FolderListFragmentViewImpl(activity, container, inflater)
        mFolderListFragmentView!!.expandableListView.addHeaderView(
            inflater.inflate(
                R.layout.padding,
                mFolderListFragmentView!!.expandableListView,
                false
            )
        )
        return mFolderListFragmentView!!.rootView!!
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as VideoListActivity?)!!.registerListener(this)
        (activity as VideoListActivity?)!!.fetchFolderList()
        registerForContextMenu(mFolderListFragmentView!!.expandableListView)
        mCallback = try {
            activity as VideoListActivity?
        } catch (ex: ClassCastException) {
            throw ClassCastException(
                activity.toString() + " must implement VideoUserInteraction"
            )
        }

        try {
            mObservableScrollViewCallbacks =
                (activity as VideoListActivity?)!!.videoListActivityView
        } catch (ex: ClassCastException) {
            throw ClassCastException("VideoListActivity View must implement ObservalbleScrollViewCallbacks")
        }

        mFolderListFragmentView!!.expandableListView
            .setOnChildClickListener { _, _, i, i1, _ ->
                val selectedVideo: String =
                    mVideoListInfo?.folderListHashMap!![mFolderNames!![i]]!![i1]
                mCallback!!.onVideoSelected(selectedVideo)
                false
            }

        mFolderListFragmentView!!.expandableListView
            .setScrollViewCallbacks(object : ObservableScrollViewCallbacks {
                override fun onScrollChanged(scrollY: Int, firstScroll: Boolean, dragging: Boolean) {
                    mObservableScrollViewCallbacks?.onScrollChanged(scrollY, firstScroll, dragging)
                }

                override fun onDownMotionEvent() {
                    mObservableScrollViewCallbacks?.onDownMotionEvent()
                }

                override fun onUpOrCancelMotionEvent(scrollState: ScrollState?) {
                    mObservableScrollViewCallbacks?.onUpOrCancelMotionEvent(scrollState)
                }
            })
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (userVisibleHint) {
            val info = item.menuInfo as ExpandableListView.ExpandableListContextMenuInfo
            val group = ExpandableListView.getPackedPositionGroup(info.packedPosition)
            val child = ExpandableListView.getPackedPositionChild(info.packedPosition)
            val selectedVideo: String = mVideoListInfo!!.folderListHashMap[mFolderNames!![group]]!![child]
            mCallback!!.onVideoLongPressed(selectedVideo, item.itemId)
            return true
        }
        return false
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val info = menuInfo as ExpandableListView.ExpandableListContextMenuInfo
        val type = ExpandableListView.getPackedPositionType(info.packedPosition)
        if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            return
        }
        val group = ExpandableListView.getPackedPositionGroup(info.packedPosition)
        val child = ExpandableListView.getPackedPositionChild(info.packedPosition)
        val selectedVideo: String =
            mVideoListInfo!!.folderListHashMap[mFolderNames!![group]]!![child]
        menu.setHeaderTitle(selectedVideo)
        val menuInflater = requireActivity().menuInflater
        menuInflater.inflate(R.menu.menu_video_long_press, menu)
    }

    override fun bindVideoList(videoListInfo: VideoListInfo?) {
        mVideoListInfo = videoListInfo
        mFolderNames = ArrayList()
        mFolderNames!!.addAll(videoListInfo!!.folderListHashMap.keys)
        mFolderNames!!.sortWith { lhs, rhs ->
            if (lhs!!.lastIndexOf('/') > 0 && rhs!!.lastIndexOf('/') > 0) {
                val lhsString = lhs.substring(lhs.lastIndexOf('/') + 1)
                val rhsString = rhs.substring(rhs.lastIndexOf('/') + 1)
                lhsString.compareTo(rhsString, ignoreCase = true)
            } else {
                -1
            }
        }
        mFolderListFragmentView!!.bindVideoList(
            videoListInfo.folderListHashMap,
            mFolderNames,
            videoListInfo
        )
    }
}
