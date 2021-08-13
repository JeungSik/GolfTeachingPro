package kr.co.anitex.golfteachingpro.videolistmanager

import java.util.*


/**
 * Created by nitinagarwal on 3/7/17.
 */
class VideoListInfo {
    var videosList: ArrayList<String>
    var videoListBackUp: ArrayList<String>
    var savedVideoList: List<String>? = null
    var folderListHashMapBackUp: HashMap<String, List<String>>
    var folderListHashMap: HashMap<String, List<String>>
    var videoIdHashMap: HashMap<String, Int>
    var videoTitleHashMap: HashMap<String, String>
    var videoSizeHashMap: HashMap<String, Int>
    var videoDurationHashMap: HashMap<String, Int>
    var videoHeightHashMap: HashMap<String, Int>
    var videoWidthHashMap: HashMap<String, Int>

    fun clearAll() {
        videoListBackUp.clear()
        videosList.clear()
        videoIdHashMap.clear()
        videoSizeHashMap.clear()
        videoWidthHashMap.clear()
        videoHeightHashMap.clear()
        videoDurationHashMap.clear()
        videoTitleHashMap.clear()
        folderListHashMapBackUp.clear()
        folderListHashMap.clear()
    }

    init {
        videosList = ArrayList()
        videoListBackUp = ArrayList()
        folderListHashMap = HashMap()
        folderListHashMapBackUp = HashMap()
        videoIdHashMap = HashMap()
        videoTitleHashMap = HashMap()
        videoDurationHashMap = HashMap()
        videoHeightHashMap = HashMap()
        videoWidthHashMap = HashMap()
        videoSizeHashMap = HashMap()
    }
}
