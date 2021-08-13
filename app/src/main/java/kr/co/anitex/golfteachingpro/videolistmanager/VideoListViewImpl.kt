package kr.co.anitex.golfteachingpro.videolistmanager

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.SearchView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager.widget.ViewPager
import kr.co.anitex.golfteachingpro.R
import kr.co.anitex.golfteachingpro.observablescrollview.ObservableScrollViewCallbacks
import kr.co.anitex.golfteachingpro.observablescrollview.ScrollState
import kr.co.anitex.golfteachingpro.observablescrollview.ScrollUtils
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.nineoldandroids.view.ViewHelper
import com.nineoldandroids.view.ViewPropertyAnimator


/**
 * Created by nitinagarwal on 3/5/17.
 */
@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class VideoListViewImpl(private val mContext: Context, container: ViewGroup?) :
    ViewMvpVideoList, ViewMvpSearch, SearchView.OnCloseListener,
    SearchView.OnQueryTextListener, ObservableScrollViewCallbacks, TabLayout.OnTabSelectedListener {
    override val rootView: View
    private var mSearchView: SearchView? = null
    private var mSearchListener: ViewMvpSearch.SearchVideo? = null
    private val mViewPager: ViewPager
    private val mTabLayout: TabLayout
    private val mAppBarLayout: AppBarLayout
    private val mToolbar: Toolbar
    private val mPixelDensityFactor: Float
    private var mWindow: Window
    private var mBaseTranslationY = 0

    override val viewPager: ViewPager
        get() = mViewPager

    //this mvp view has no state that should be retrieved
    override val viewState: Bundle?
        get() =//this mvp view has no state that should be retrieved
            null

    override fun onClose(): Boolean {
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        mSearchListener!!.onVideoSearched(query)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        mSearchListener!!.onVideoSearched(newText)
        return true
    }

    fun onClick() {}

    override fun SetSearchListener(searchListener: ViewMvpSearch.SearchVideo?) {
        mSearchListener = searchListener
    }

    override fun AddSearchBar(searchViewMenuItem: MenuItem?) {
        mSearchView = searchViewMenuItem!!.actionView as SearchView
        mSearchView!!.isIconifiedByDefault = true
        mSearchView!!.setOnQueryTextListener(this)
        mSearchView!!.setOnCloseListener(this)
    }

    override fun searchClose() {
        mSearchView!!.setQuery("", false)
        mSearchListener!!.onVideoSearched("")
        mSearchView!!.onActionViewCollapsed()
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        when (tab.position) {
            0 -> {
                mAppBarLayout.setBackgroundResource(R.color.saved_tab_color)
                mWindow.statusBarColor = mContext.resources.getColor(R.color.saved_statusbar_color)
            }
            1 -> {
                mAppBarLayout.setBackgroundResource(R.color.folder_tab_color)
                mWindow.statusBarColor = mContext.resources.getColor(R.color.folder_statusbar_color)
            }
            2 -> {
                mAppBarLayout.setBackgroundResource(R.color.all_tab_color)
                mWindow.statusBarColor = mContext.resources.getColor(R.color.all_statusbar_color)
            }
            else -> {
                mAppBarLayout.setBackgroundResource(R.color.colorPrimary)
            }
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
        Log.d("Tab", "onTabUnselected!!!!!!")
        showToolbar()
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
        Log.d("Tab", "onTabReselected!!!!!!")
        showToolbar()
    }

    override fun onScrollChanged(scrollY: Int, firstScroll: Boolean, dragging: Boolean) {
        if (dragging) {
            val toolbarHeight: Int = mToolbar.height
            val currentHeaderTranslationY: Float = ViewHelper.getTranslationY(mAppBarLayout)
            if (firstScroll) {
                if (-toolbarHeight < currentHeaderTranslationY) {
                    mBaseTranslationY = scrollY
                }
            }
            val headerTranslationY: Float =
                ScrollUtils.getFloat(
                    (-(scrollY - mBaseTranslationY)).toFloat(),
                    (-toolbarHeight).toFloat(), 0F
                )
            ViewPropertyAnimator.animate(mAppBarLayout).cancel()
            ViewHelper.setTranslationY(mAppBarLayout, headerTranslationY)
        }
    }

    override fun onDownMotionEvent() {}

    override fun onUpOrCancelMotionEvent(scrollState: ScrollState?) {
        Log.d("Tab", "onUpOrCancelMotionEvent!!!!!!")
        mBaseTranslationY = 0
        if (scrollState === ScrollState.DOWN) {
            showToolbar()
        } else if (scrollState === ScrollState.UP) {
            //hideToolbar()
        }
    }

    private fun showToolbar() {
        val headerTranslationY: Float = ViewHelper.getTranslationY(mAppBarLayout)
        if (headerTranslationY != 0f) {
            ViewPropertyAnimator.animate(mAppBarLayout).cancel()
            ViewPropertyAnimator.animate(mAppBarLayout).translationY(0F).setDuration(200).start()
        }
        mAppBarLayout.animate().translationY(0F).interpolator = DecelerateInterpolator(2F)
        //propagateToolbarState(true);
    }

    private fun hideToolbar() {
        val headerTranslationY: Float = ViewHelper.getTranslationY(mAppBarLayout)
        val toolbarHeight: Int = mToolbar.height
        var fabButtonBottomMargin = 16
        fabButtonBottomMargin += 2 //adding 2 dp to make sure the button is hidden completely
        if (headerTranslationY != -toolbarHeight.toFloat()) {
            ViewPropertyAnimator.animate(mAppBarLayout).cancel()
            ViewPropertyAnimator.animate(mAppBarLayout).translationY((-toolbarHeight).toFloat())
                .setDuration(200).start()
        }
    }

    companion object {
        private const val TAG = "VideoListViewImpl"
    }

    init {
        val appCompatActivity: AppCompatActivity = mContext as AppCompatActivity
        val metrics = mContext.resources.displayMetrics
        mPixelDensityFactor = metrics.densityDpi / 160f
        rootView = LayoutInflater.from(mContext).inflate(R.layout.activity_videolist, container)
        mViewPager = rootView.findViewById<View>(R.id.viewpager) as ViewPager
        mViewPager.currentItem = 0

        /*
        val titles = arrayOf<CharSequence>(
            mContext.getResources().getString(R.string.list_tab_name)
        )
        */

        val titles = arrayOf<CharSequence>(
            mContext.getResources().getString(R.string.saved_tab_name),
            mContext.getResources().getString(R.string.folder_tab_name),
            mContext.getResources().getString(R.string.alllist_tab_name)
        )

        val viewPagerAdapter = ViewPagerAdapter(appCompatActivity.supportFragmentManager, titles)
        mViewPager.adapter = viewPagerAdapter
        mViewPager.currentItem = 0

        mWindow = mContext.window

        mTabLayout = rootView.findViewById<View>(R.id.tablayout) as TabLayout
        mAppBarLayout = rootView.findViewById<View>(R.id.appBarLayout) as AppBarLayout

        mTabLayout.setupWithViewPager(mViewPager)
        mTabLayout.addOnTabSelectedListener(this)

        mToolbar = rootView.findViewById<View>(R.id.videolist_toolbar) as Toolbar
        mToolbar.setLogo(R.drawable.main_logo)
        appCompatActivity.setSupportActionBar(mToolbar)
        mAppBarLayout.setBackgroundResource(R.color.saved_tab_color)
        mWindow.statusBarColor = mContext.resources.getColor(R.color.saved_statusbar_color)

    }
}