package kr.co.anitex.golfteachingpro.videolistmanager

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import kr.co.anitex.golfteachingpro.FolderListFragment
import kr.co.anitex.golfteachingpro.SavedListFragment
import kr.co.anitex.golfteachingpro.VideoListFragment

/**
 * Created by nitinagarwal on 3/15/17.
 */
@Suppress("DEPRECATION")
class ViewPagerAdapter(fm: FragmentManager?, var mTitles: Array<CharSequence>) :
    FragmentPagerAdapter(fm!!) {
    var mContext: Context? = null

    override fun getPageTitle(position: Int): CharSequence {
        return mTitles[position]
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> SavedListFragment()
            1 -> FolderListFragment()
            2 -> VideoListFragment()
            else -> SavedListFragment()
        }
    }

    override fun getCount(): Int {
        return NUM_ITEMS
    }

    companion object {
        const val NUM_ITEMS = 3
    }
}