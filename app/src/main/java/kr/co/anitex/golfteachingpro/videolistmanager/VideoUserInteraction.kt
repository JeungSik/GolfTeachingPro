package kr.co.anitex.golfteachingpro.videolistmanager

/**
 * Created by nitinagarwal on 4/9/17.
 */
interface VideoUserInteraction {
    fun onVideoSelected(videoPath: String?)
    fun onVideoLongPressed(videoPath: String?, itemId: Int)
}