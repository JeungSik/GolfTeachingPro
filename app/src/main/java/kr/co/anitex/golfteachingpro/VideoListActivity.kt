@file:Suppress("DEPRECATION")

package kr.co.anitex.golfteachingpro

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import kr.co.anitex.golfteachingpro.videolistmanager.*


@Suppress("UNCHECKED_CAST")
class VideoListActivity : AppCompatActivity(), VideoListManager.VideoListManagerListener,
        VideoUserInteraction {
    private var mSortingType = 0
    private var mVideoListingViewImpl: VideoListViewImpl? = null
    private var mVideoListManagerImpl: VideoListManagerImpl? = null
    private var mViewPager: ViewPager? = null
    private var mVideoListFragment: VideoListFragment? = null
    private var mFolderListFragment: FolderListFragment? = null
    private var mSavedListFragment: SavedListFragment? = null
    private var mVideoListInfo: VideoListInfo? = null
    private var mIsInSearchMode = false
    private var mSearchText: String? = null
    private var mShouldExecutedOnResume = false

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mVideoListingViewImpl = VideoListViewImpl(this, null)
        setContentView(mVideoListingViewImpl!!.rootView)
        mViewPager = mVideoListingViewImpl!!.viewPager
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        mSortingType = settings.getInt(SORT_TYPE_PREFERENCE_KEY, 3)
        mVideoListManagerImpl = VideoListManagerImpl(this, mSortingType)
        mVideoListManagerImpl!!.registerListener(this)
    }

    override fun onResume() {
        super.onResume()
        if(mShouldExecutedOnResume) {
            updateSharedPreferenceAndGetNewList(mSortingType)
        } else {
            mShouldExecutedOnResume = true
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_sort, menu)
        mVideoListingViewImpl!!.AddSearchBar(menu.findItem(R.id.action_search))
        val searchVideoListener: ViewMvpSearch.SearchVideo = object : ViewMvpSearch.SearchVideo {
            override fun onVideoSearched(seachText: String?) {
                mSearchText = seachText
                if (seachText != null) {
                    if (seachText.trim { it <= ' ' } == "") {
                        mIsInSearchMode = false
                        mVideoListInfo!!.videosList.clear()
                        mVideoListInfo!!.videosList.addAll(mVideoListInfo!!.videoListBackUp)
                    } else {
                        mIsInSearchMode = true
                        mVideoListInfo!!.videosList =
                            VideoSearch.SearchResult(
                                seachText,
                                mVideoListInfo!!.videoListBackUp
                            )

                        mVideoListInfo!!.folderListHashMap =
                            VideoSearch.SearchResult(
                                seachText,
                                mVideoListInfo!!.folderListHashMapBackUp
                            )

                    }
                }
                mVideoListInfo!!.savedVideoList =
                    FolderListGenerator.getSavedVideoListFromFolderHashMap(
                        mVideoListInfo!!.folderListHashMap
                    ) as List<String>?

                if (mVideoListFragment != null) mVideoListFragment!!.bindVideoList(mVideoListInfo)
            }
        }
        mVideoListingViewImpl!!.SetSearchListener(searchVideoListener)
        setSortingOptionChecked(menu)
        return true
    }

    private fun setSortingOptionChecked(menu: Menu) {
        when (mSortingType) {
            NAME_ASC -> menu.findItem(R.id.sort_name_asc).isChecked =
                true
            NAME_DESC -> menu.findItem(R.id.sort_name_dsc).isChecked =
                true
            DATE_ASC -> menu.findItem(R.id.sort_date_asc).isChecked =
                true
            DATE_DESC -> menu.findItem(R.id.sort_date_dsc).isChecked =
                true
            SIZE_ASC -> menu.findItem(R.id.sort_size_asc).isChecked =
                true
            SIZE_DESC -> menu.findItem(R.id.sort_size_dsc).isChecked =
                true
            else -> menu.findItem(R.id.sort_date_dsc).isChecked = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.sort_name_asc -> {
                updateSharedPreferenceAndGetNewList(NAME_ASC)
                item.isChecked = true
            }
            R.id.sort_name_dsc -> {
                updateSharedPreferenceAndGetNewList(NAME_DESC)
                item.isChecked = true
            }
            R.id.sort_date_asc -> {
                updateSharedPreferenceAndGetNewList(DATE_ASC)
                item.isChecked = true
            }
            R.id.sort_date_dsc -> {
                updateSharedPreferenceAndGetNewList(DATE_DESC)
                item.isChecked = true
            }
            R.id.sort_size_asc -> {
                updateSharedPreferenceAndGetNewList(SIZE_ASC)
                item.isChecked = true
            }
            R.id.sort_size_dsc -> {
                updateSharedPreferenceAndGetNewList(SIZE_DESC)
                item.isChecked = true
            }
            else -> {
                updateSharedPreferenceAndGetNewList(DATE_DESC)
                item.isChecked = true
            }
        }
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }

    private fun updateSharedPreferenceAndGetNewList(sortType: Int) {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = settings.edit()
        editor.putInt(SORT_TYPE_PREFERENCE_KEY, sortType)
        editor.apply()
        mVideoListManagerImpl?.getVideosWithNewSorting(sortType)
    }

    override fun onVideoListUpdate(videoListInfo: VideoListInfo?) {
        mVideoListInfo = videoListInfo
        if (mIsInSearchMode) {
            mVideoListInfo?.videosList =
                VideoSearch.SearchResult(
                    mSearchText!!,
                    mVideoListInfo!!.videoListBackUp
                )

            mVideoListInfo?.folderListHashMap =
                VideoSearch.SearchResult(
                    mSearchText!!,
                    mVideoListInfo!!.folderListHashMapBackUp
                )

        } else {
            if (mVideoListInfo?.videosList != null) mVideoListInfo!!.videosList.clear()
            mVideoListInfo?.videosList?.addAll(mVideoListInfo!!.videoListBackUp)
            if (mVideoListInfo?.folderListHashMap != null) mVideoListInfo!!.folderListHashMap
                .clear()
            mVideoListInfo?.folderListHashMap
                ?.putAll(mVideoListInfo!!.folderListHashMapBackUp)
        }
        mVideoListInfo?.savedVideoList =
            FolderListGenerator.getSavedVideoListFromFolderHashMap(
                mVideoListInfo!!.folderListHashMap
            ) as List<String>?

        fetchVideoList()
        fetchFolderList()
        fetchSavedList()
    }

    fun fetchVideoList() {
        if (mVideoListFragment != null && mVideoListInfo != null) {
            mVideoListFragment?.bindVideoList(mVideoListInfo)
        }
    }

    fun fetchFolderList() {
        if (mFolderListFragment != null && mVideoListInfo != null) {
            mFolderListFragment?.bindVideoList(mVideoListInfo)
        }
    }

    fun fetchSavedList() {
        if (mSavedListFragment != null && mVideoListInfo != null) {
            mSavedListFragment?.bindVideoList(mVideoListInfo)
        }
    }

    fun registerListener(videoListFragment: VideoListFragment) {
        mVideoListFragment = videoListFragment
    }


    fun registerListener(folderListFragment: FolderListFragment) {
        mFolderListFragment = folderListFragment
    }


    fun registerListener(savedListFragment: SavedListFragment) {
        mSavedListFragment = savedListFragment
    }


    override fun onVideoSelected(videoPath: String?) {
        Toast.makeText(this, videoPath + "clicked", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, ReviewActivity::class.java)
        val args = Bundle()
        args.putString("fileUri", videoPath)
        intent.putExtras(args)
        startActivity(intent)
    }

    val videoListActivityView: VideoListViewImpl?
        get() = mVideoListingViewImpl

    override fun onVideoLongPressed(videoPath: String?, itemId: Int) {
        when (itemId) {
            R.id.long_press_menu_share -> VideoLongPressOptions.shareFile(this, videoPath!!)
            R.id.long_press_menu_delete -> {
                val deleteVideoId: Int? = mVideoListInfo!!.videoIdHashMap[videoPath]
                VideoLongPressOptions.deleteFile(
                    this,
                    videoPath!!, deleteVideoId!!, mVideoListManagerImpl!!
                )
            }
            R.id.long_press_menu_rename -> {
                val selectedVideoTitleWithExtension: String? =
                    mVideoListInfo!!.videoTitleHashMap[videoPath]
                val index = selectedVideoTitleWithExtension!!.lastIndexOf('.')
                val selectedVideoTitleForRename: String
                val extensionValue: String
                if (index > 0) {
                    selectedVideoTitleForRename =
                        selectedVideoTitleWithExtension.substring(0, index)
                    extensionValue = selectedVideoTitleWithExtension.substring(
                        index,
                        selectedVideoTitleWithExtension.length
                    )
                } else {
                    selectedVideoTitleForRename = selectedVideoTitleWithExtension
                    extensionValue = ""
                }
                val renameVideoId: Int? = mVideoListInfo!!.videoIdHashMap[videoPath]
                VideoLongPressOptions.renameFile(
                    this, selectedVideoTitleForRename, videoPath!!,
                    extensionValue, renameVideoId!!, mVideoListManagerImpl!!
                )
            }
        }
    }

    companion object {
        private const val SORT_TYPE_PREFERENCE_KEY = "sort_type"
        const val NAME_ASC = 0
        const val NAME_DESC = 1
        const val DATE_ASC = 2
        const val DATE_DESC = 3
        const val SIZE_ASC = 4
        const val SIZE_DESC = 5
        //const val SHARE_VIDEO = 0
        //const val DELETE_VIDEO = 1
        //const val RENAME_VIDEO = 2
    }
}
