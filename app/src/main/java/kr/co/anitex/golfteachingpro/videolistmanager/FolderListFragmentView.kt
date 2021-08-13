package kr.co.anitex.golfteachingpro.videolistmanager

import java.util.*

/**
 * Created by nitinagarwal on 3/15/17.
 */
interface FolderListFragmentView : ViewMvp {
    val expandableListView: ObservableExpandableListView?

    fun bindVideoList(
        folderListHashMap: HashMap<String, List<String>>,
        folderNames: ArrayList<String>?,
        videoListInfo: VideoListInfo?
    )
}
