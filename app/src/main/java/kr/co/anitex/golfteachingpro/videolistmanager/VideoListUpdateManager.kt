package kr.co.anitex.golfteachingpro.videolistmanager

/**
 * Created by nitinagarwal on 4/10/17.
 */
interface VideoListUpdateManager {
    fun updateForDeleteVideo(id: Int)
    fun updateForRenameVideo(id: Int, newFilePath: String?, updatedTitle: String?)
}