package kr.co.anitex.golfteachingpro.videolistmanager

/**
 * Created by nitinagarwal on 3/7/17.
 */
interface VideoListManager {
    interface VideoListManagerListener {
        fun onVideoListUpdate(videoListInfo: VideoListInfo?)
    }

    fun getVideosWithNewSorting(sortType: Int)
    fun registerListener(videoListManagerListener: VideoListManagerListener?)
    fun unRegisterListener()
}