package kr.co.anitex.golfteachingpro.videolistmanager

import android.view.MenuItem

/**
 * Created by nitinagarwal on 3/5/17.
 */
interface ViewMvpSearch {
    interface SearchVideo {
        fun onVideoSearched(seachText: String?)
    }

    fun SetSearchListener(searchListener: SearchVideo?)
    fun AddSearchBar(searchViewMenuItem: MenuItem?)
    fun searchClose()
}