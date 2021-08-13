package kr.co.anitex.golfteachingpro.videolistmanager

import kr.co.anitex.golfteachingpro.observablescrollview.ObservableListView

/**
 * Created by nitinagarwal on 3/12/17.
 */
interface ListFragmentView : ViewMvp {
    fun bindVideoList(videoList: List<String?>?, videoListInfo: VideoListInfo?)
    val listView: ObservableListView?
}