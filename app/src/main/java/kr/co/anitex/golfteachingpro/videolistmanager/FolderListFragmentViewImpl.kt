package kr.co.anitex.golfteachingpro.videolistmanager

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kr.co.anitex.golfteachingpro.R
import java.util.*

/**
 * Created by nitinagarwal on 3/15/17.
 */
class FolderListFragmentViewImpl(
    context: Context?,
    container: ViewGroup?,
    inflater: LayoutInflater
) :
    FolderListFragmentView {
    private var mFragmentFolderListView: View =
        inflater.inflate(R.layout.tab_folderlist, container, false)
    private var mFolderListAdapter: FolderListAdapter = FolderListAdapter(context)
    private var mExpandableListView: ObservableExpandableListView

    override val expandableListView: ObservableExpandableListView
        get() = mExpandableListView

    override val rootView: View
        get() = mFragmentFolderListView

    override fun bindVideoList(
        folderListHashMap: HashMap<String, List<String>>,
        folderNames: ArrayList<String>?,
        videoListInfo: VideoListInfo?
    ) {
        mFolderListAdapter.bindVideoList(folderListHashMap, folderNames, videoListInfo)
        mFolderListAdapter.notifyDataSetChanged()
    }

    override val viewState: Bundle?
        get() = null

    init {
        mExpandableListView =
            rootView.findViewById<View>(R.id.expandablelistview) as ObservableExpandableListView
        mExpandableListView.setAdapter(mFolderListAdapter)
    }
}
