package kr.co.anitex.golfteachingpro.videolistmanager

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kr.co.anitex.golfteachingpro.R
import kr.co.anitex.golfteachingpro.observablescrollview.ObservableListView

/**
 * Created by nitinagarwal on 3/13/17.
 */
class SavedListViewImpl(context: Context?, container: ViewGroup?, inflater: LayoutInflater) :
    SavedListView {
    override val rootView: View = inflater.inflate(R.layout.tab_videolist, container, false)
    private var mSavedListAdapter: VideoListAdapter = VideoListAdapter(context!!, R.layout.tab_child)
    private var mSavedListView: ObservableListView

    override fun bindSavedVideoList(savedVideoList: List<String?>?, videoListInfo: VideoListInfo?) {
        mSavedListAdapter.bindVideoList(savedVideoList, videoListInfo)
        mSavedListAdapter.notifyDataSetChanged()
    }

    override val savedListView: ObservableListView
        get() = mSavedListView

    override val viewState: Bundle?
        get() = null

    init {
        mSavedListView = rootView.findViewById<View>(R.id.ListView) as ObservableListView
        mSavedListView.adapter = mSavedListAdapter
    }
}
