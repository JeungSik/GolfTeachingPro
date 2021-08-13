package kr.co.anitex.golfteachingpro.videolistmanager

import kr.co.anitex.golfteachingpro.APP_NAME
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by nitinagarwal on 3/15/17.
 */
object FolderListGenerator {
    fun generateFolderHashMap(
        videoList: ArrayList<String>,
        folderListHashMap: HashMap<String, List<String>>
    ) {
        var videoFullPath: String
        var folderName: String
        for (i in videoList.indices) {
            videoFullPath = videoList[i]
            folderName = if (videoFullPath.lastIndexOf('/') > 0) {
                videoFullPath.substring(0, videoFullPath.lastIndexOf('/'))
            } else {
                ""
            }
            if (folderListHashMap[folderName] == null) {
                val innerFolderVideosList = ArrayList<String?>()
                innerFolderVideosList.add(videoFullPath)
                folderListHashMap[folderName] = innerFolderVideosList as List<String>
            } else {
                val innerFolderVideosList = folderListHashMap[folderName] as ArrayList
                innerFolderVideosList.add(videoFullPath)
            }
        }
    }

    fun getSavedVideoListFromFolderHashMap(folderListHashMap: HashMap<String, List<String>>): List<String?>? {
        val folderNames: Set<String> = folderListHashMap.keys
        for (key in folderListHashMap.keys) {
            if (key.endsWith("/$APP_NAME")) return folderListHashMap[key]
        }
        return null
    }
}
