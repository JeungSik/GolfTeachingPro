@file:Suppress("NAME_SHADOWING")

package kr.co.anitex.golfteachingpro.videolistmanager

import java.io.File
import java.util.*

/**
 * Created by nitinagarwal on 3/14/17.
 */
object VideoSearch {
    fun SearchResult(searchQuery: String, videoList: List<String>): ArrayList<String> {
        var searchQuery = searchQuery
        searchQuery = searchQuery.toLowerCase(Locale.ROOT)
        val newList = ArrayList<String>()
        for (fileName in videoList) {
            var tempFileName = fileName
            if (fileName.contains(File.separator)) {
                val index = fileName.lastIndexOf(File.separator)
                tempFileName = fileName.substring(index, fileName.length)
            }
            if (tempFileName.toLowerCase(Locale.ROOT).contains(searchQuery)) {
                newList.add(fileName)
            }
        }
        return newList
    }

    fun SearchResult(searchQuery: String, folderListHashMap: HashMap<String, List<String>>): HashMap<String, List<String>> {
        var searchQuery = searchQuery
        val result = HashMap<String, List<String>>()
        for (key in folderListHashMap.keys) {
            searchQuery = searchQuery.toLowerCase(Locale.ROOT)
            val fileNames = folderListHashMap[key]!!
            val newList = LinkedList<String>()
            for (fileName in fileNames) {
                var tempFileName = fileName
                if (fileName.contains(File.separator)) {
                    val index = fileName.lastIndexOf(File.separator)
                    tempFileName = fileName.substring(index, fileName.length)
                }
                if (tempFileName.toLowerCase(Locale.ROOT).contains(searchQuery)) {
                    newList.add(fileName)
                }
                if (!newList.isEmpty()) {
                    result[key] = newList
                }
            }
        }
        return result
    }
}