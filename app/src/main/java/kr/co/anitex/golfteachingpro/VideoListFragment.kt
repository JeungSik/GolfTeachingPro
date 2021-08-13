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
import kr.co.anitex.golfteachingpro.videolistmanager.ListFragmentViewImpl
import kr.co.anitex.golfteachingpro.videolistmanager.VideoListFragmentInterface
import kr.co.anitex.golfteachingpro.videolistmanager.VideoListInfo
import kr.co.anitex.golfteachingpro.videolistmanager.VideoUserInteraction


/**
 * Created by nitinagarwal on 3/12/17.
 */
@Suppress("DEPRECATION")
class VideoListFragment : Fragment(), VideoListFragmentInterface {
    private var mListFragmentViewImpl: ListFragmentViewImpl? = null
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
        mListFragmentViewImpl = ListFragmentViewImpl(activity, container, inflater)
        mListFragmentViewImpl!!.listView.addHeaderView(
            inflater.inflate(
                R.layout.padding,
                mListFragmentViewImpl!!.listView,
                false
            )
        )
        return mListFragmentViewImpl!!.rootView!!
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as VideoListActivity?)!!.registerListener(this)
        (activity as VideoListActivity?)!!.fetchVideoList()
        registerForContextMenu(mListFragmentViewImpl!!.listView)
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

        mListFragmentViewImpl!!.listView.onItemClickListener =
            OnItemClickListener { _, _, i, _ ->
                val selectedVideo: String = mVideoListInfo!!.videosList[i - 1]
                mCallback!!.onVideoSelected(selectedVideo)
            }
        mListFragmentViewImpl!!.listView
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
        mListFragmentViewImpl!!.bindVideoList(videoListInfo!!.videosList, videoListInfo)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (userVisibleHint) {
            val info = item.menuInfo as AdapterContextMenuInfo
            val selectedVideo: String = mVideoListInfo!!.videosList[info.position - 1]
            mCallback!!.onVideoLongPressed(selectedVideo, item.itemId)
            return true
        }
        return false
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val info = menuInfo as AdapterContextMenuInfo
        val selectedVideo: String = mVideoListInfo!!.videosList[info.position - 1]
        val selectedVideoTitleWithExtension: String? = mVideoListInfo!!.videoTitleHashMap[selectedVideo]
        menu.setHeaderTitle(selectedVideoTitleWithExtension)
        val menuInflater: MenuInflater = requireActivity().menuInflater
        menuInflater.inflate(R.menu.menu_video_long_press, menu)
    }
}
