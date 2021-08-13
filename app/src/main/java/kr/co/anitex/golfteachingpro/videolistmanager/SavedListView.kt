package kr.co.anitex.golfteachingpro.videolistmanager

import kr.co.anitex.golfteachingpro.observablescrollview.ObservableListView

/**
 * Created by nitinagarwal on 3/13/17.
 */
interface SavedListView : ViewMvp {
    fun bindSavedVideoList(savedVideoList: List<String?>?, videoListInfo: VideoListInfo?)
    val savedListView: ObservableListView?
}
