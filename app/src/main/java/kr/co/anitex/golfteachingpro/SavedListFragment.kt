package kr.co.anitex.golfteachingpro

import android.os.Bundle
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.AdapterView.OnItemClickListener
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import kr.co.anitex.golfteachingpro.observablescrollview.ObservableScrollViewCallbacks
import kr.co.anitex.golfteachingpro.observablescrollview.ScrollState
import kr.co.anitex.golfteachingpro.videolistmanager.SavedListViewImpl
import kr.co.anitex.golfteachingpro.videolistmanager.VideoListFragmentInterface
import kr.co.anitex.golfteachingpro.videolistmanager.VideoListInfo
import kr.co.anitex.golfteachingpro.videolistmanager.VideoUserInteraction

/**
 * Created by nitinagarwal on 3/13/17.
 */
@Suppress("DEPRECATION")
class SavedListFragment : Fragment(), VideoListFragmentInterface {
    private var mSavedListViewImpl: SavedListViewImpl? = null
    private var mVideoListInfo: VideoListInfo? = null
    private var mCallback: VideoUserInteraction? = null
    var mObservableScrollViewCallbacks: ObservableScrollViewCallbacks? = null

    @Nullable
    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        mSavedListViewImpl = SavedListViewImpl(activity, container, inflater)
        mSavedListViewImpl!!.savedListView.addHeaderView(
            inflater.inflate(
                R.layout.padding,
                mSavedListViewImpl!!.savedListView,
                false
            )
        )
        return mSavedListViewImpl!!.rootView
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as VideoListActivity?)!!.registerListener(this)
        (activity as VideoListActivity?)!!.fetchSavedList()
        registerForContextMenu(mSavedListViewImpl!!.savedListView)
        mCallback = try {
            activity as VideoListActivity?
        } catch (ex: ClassCastException) {
            throw ClassCastException(
                activity.toString() + " must implement VideoUserInteraction"
            )
        }

        try {
            mObservableScrollViewCallbacks =
                (activity as VideoListActivity).videoListActivityView
        } catch (ex: ClassCastException) {
            throw ClassCastException("VideoListActivity View must implement ObservalbleScrollViewCallbacks")
        }

        mSavedListViewImpl!!.savedListView.onItemClickListener =
            OnItemClickListener { _, _, i, _ ->
                val selectedVideo: String = mVideoListInfo?.savedVideoList!![i - 1]
                mCallback!!.onVideoSelected(selectedVideo)
            }
        mSavedListViewImpl!!.savedListView
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

    override fun bindVideoList(videoListInfo: VideoListInfo?) {
        mVideoListInfo = videoListInfo
        mSavedListViewImpl!!.bindSavedVideoList(videoListInfo!!.savedVideoList, videoListInfo)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (userVisibleHint) {
            val info = item.menuInfo as AdapterContextMenuInfo
            val selectedVideo: String = mVideoListInfo!!.savedVideoList!![info.position - 1]
            mCallback!!.onVideoLongPressed(selectedVideo, item.itemId)
            return true
        }
        return false
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val info = menuInfo as AdapterContextMenuInfo
        val selectedVideo: String = mVideoListInfo!!.savedVideoList!![info.position - 1]
        menu.setHeaderTitle(selectedVideo)
        val menuInflater: MenuInflater = requireActivity().menuInflater
        menuInflater.inflate(R.menu.menu_video_long_press, menu)
    }
}
