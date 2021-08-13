package kr.co.anitex.golfteachingpro.videolistmanager

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kr.co.anitex.golfteachingpro.R
import kr.co.anitex.golfteachingpro.observablescrollview.ObservableListView

class ListFragmentViewImpl(context: Context?, container: ViewGroup?, inflater: LayoutInflater) :
        ListFragmentView {
    private var mFragmentVideoListView: View =
        inflater.inflate(R.layout.tab_videolist, container, false)
    private var mVideoListAdapter: VideoListAdapter = VideoListAdapter(context!!, R.layout.tab_child)
    private var mListView: ObservableListView

    override val listView: ObservableListView
        get() = mListView

    override val rootView: View
        get() = mFragmentVideoListView

    override fun bindVideoList(videoList: List<String?>?, videoListInfo: VideoListInfo?) {
        mVideoListAdapter.bindVideoList(videoList, videoListInfo)
        mVideoListAdapter.notifyDataSetChanged()
    }

    override val viewState: Bundle?
        get() = null

    init {
        mListView = rootView.findViewById<View>(R.id.ListView) as ObservableListView
        mListView.adapter = mVideoListAdapter
    }
}
