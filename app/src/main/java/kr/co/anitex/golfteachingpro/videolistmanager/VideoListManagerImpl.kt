@file:Suppress("DEPRECATION")

package kr.co.anitex.golfteachingpro.videolistmanager

import android.app.LoaderManager
import android.content.ContentValues
import android.content.Context
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kr.co.anitex.golfteachingpro.VideoListActivity.Companion.DATE_ASC
import kr.co.anitex.golfteachingpro.VideoListActivity.Companion.DATE_DESC
import kr.co.anitex.golfteachingpro.VideoListActivity.Companion.NAME_ASC
import kr.co.anitex.golfteachingpro.VideoListActivity.Companion.NAME_DESC
import kr.co.anitex.golfteachingpro.VideoListActivity.Companion.SIZE_ASC
import kr.co.anitex.golfteachingpro.VideoListActivity.Companion.SIZE_DESC


/**
 * Created by nitinagarwal on 3/6/17.
 */
class VideoListManagerImpl(private val mContext: Context, private var mSortingPreference: Int) :
    LoaderManager.LoaderCallbacks<Cursor?>, VideoListManager,
        VideoListUpdateManager {
    private val mAppCompatActivity: AppCompatActivity = mContext as AppCompatActivity
    private var mVideoListManagerListerner: VideoListManager.VideoListManagerListener? = null
    private val mVideoListInfo: VideoListInfo

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor?> {
        //val selection = MediaStore.Video.Media.DATA + " like ? "
        //val selectionArgs = arrayOf("%$APP_NAME%")
        val selection = null
        val selectionArgs = null

        return when (mSortingPreference) {
            NAME_ASC -> CursorLoader(
                mContext,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                COLUMNS_OF_INTEREST,
                selection,
                selectionArgs,
                MediaStore.Video.Media.DISPLAY_NAME + " ASC"
            )
            NAME_DESC -> CursorLoader(
                mContext,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                COLUMNS_OF_INTEREST,
                selection,
                selectionArgs,
                MediaStore.Video.Media.DISPLAY_NAME + " DESC"
            )
            DATE_ASC -> CursorLoader(
                mContext,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                COLUMNS_OF_INTEREST,
                selection,
                selectionArgs,
                MediaStore.Video.Media.DATE_ADDED + " ASC"
            )
            DATE_DESC -> CursorLoader(
                mContext,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                COLUMNS_OF_INTEREST,
                selection,
                selectionArgs,
                MediaStore.Video.Media.DATE_ADDED + " DESC"
            )
            SIZE_ASC -> CursorLoader(
                mContext,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                COLUMNS_OF_INTEREST,
                selection,
                selectionArgs,
                MediaStore.Video.Media.SIZE + " ASC"
            )
            SIZE_DESC -> CursorLoader(
                mContext,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                COLUMNS_OF_INTEREST,
                selection,
                selectionArgs,
                MediaStore.Video.Media.SIZE + " DESC"
            )
            else -> CursorLoader(
                mContext,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                COLUMNS_OF_INTEREST,
                selection,
                selectionArgs,
                MediaStore.Video.Media.DATE_ADDED + " DESC"
            )
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor?>?, cursor: Cursor?) {
        if (cursor != null) {
            updateVideoList(cursor)
            FolderListGenerator.generateFolderHashMap(
                mVideoListInfo.videoListBackUp,
                mVideoListInfo.folderListHashMapBackUp
            )
            if (mVideoListManagerListerner != null) mVideoListManagerListerner!!.onVideoListUpdate(
                mVideoListInfo
            )
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor?>) {}
    private fun updateVideoList(cursor: Cursor) {
        mVideoListInfo.clearAll()
        cursor.moveToFirst()
        val coloumnIndexUri = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
        var coloumnIndex: Int
        for (i in 0 until cursor.count) {
            mVideoListInfo.videoListBackUp.add(cursor.getString(coloumnIndexUri))
            coloumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            mVideoListInfo.videoIdHashMap[cursor.getString(coloumnIndexUri)] = cursor.getInt(
                coloumnIndex
            )
            coloumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            mVideoListInfo.videoTitleHashMap[cursor.getString(coloumnIndexUri)] = cursor.getString(
                coloumnIndex
            )
            coloumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            mVideoListInfo.videoHeightHashMap[cursor.getString(coloumnIndexUri)] = cursor.getInt(
                coloumnIndex
            )
            coloumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            mVideoListInfo.videoWidthHashMap[cursor.getString(coloumnIndexUri)] = cursor.getInt(
                coloumnIndex
            )
            coloumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            mVideoListInfo.videoDurationHashMap[cursor.getString(coloumnIndexUri)] = cursor.getInt(
                coloumnIndex
            )
            coloumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            mVideoListInfo.videoSizeHashMap[cursor.getString(coloumnIndexUri)] = cursor.getInt(
                coloumnIndex
            )
            cursor.moveToNext()
        }
    }

    override fun getVideosWithNewSorting(sortType: Int) {
        mSortingPreference = sortType
        mAppCompatActivity.loaderManager.restartLoader(URL_LOADER_EXTERNAL, null, this)
    }

    override fun registerListener(videoListManagerListener: VideoListManager.VideoListManagerListener?) {
        mVideoListManagerListerner = videoListManagerListener
    }

    override fun unRegisterListener() {
        mVideoListManagerListerner = null
    }

    override fun updateForDeleteVideo(id: Int) {
        mContext.contentResolver.delete(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media._ID + "=" + id, null
        )

        mAppCompatActivity.loaderManager.restartLoader(URL_LOADER_EXTERNAL, null, this)
    }

    override fun updateForRenameVideo(id: Int, newFilePath: String?, updatedTitle: String?) {
        val contentValues = ContentValues(2)
        contentValues.put(MediaStore.Video.Media.DATA, newFilePath)
        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, updatedTitle)
        mContext.contentResolver.update(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues,
            MediaStore.Video.Media._ID + "=" + id, null
        )

        mAppCompatActivity.loaderManager.restartLoader(URL_LOADER_EXTERNAL, null, this)
    }

    companion object {
        private const val URL_LOADER_EXTERNAL = 0
        @RequiresApi(Build.VERSION_CODES.Q)
        private val COLUMNS_OF_INTEREST = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED

        )
        private const val TAG = "videolistManagerImpl"
    }

    init {
        mAppCompatActivity.loaderManager.initLoader(URL_LOADER_EXTERNAL, null, this)
        mVideoListInfo = VideoListInfo()
    }
}
