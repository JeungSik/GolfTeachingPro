package kr.co.anitex.golfteachingpro.observablescrollview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseIntArray
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ListView

/**
 * ListView that its scroll position can be observed.
 */
class ObservableListView : ListView, Scrollable {
    // Fields that should be saved onSaveInstanceState
    private var mPrevFirstVisiblePosition = 0
    private var mPrevFirstVisibleChildHeight = -1
    private var mPrevScrolledChildrenHeight = 0
    private var mPrevScrollY = 0
    private var mScrollY = 0
    private var mChildrenHeights: SparseIntArray? = null

    // Fields that don't need to be saved onSaveInstanceState
    private var mCallbacks: ObservableScrollViewCallbacks? = null
    private var mScrollState: ScrollState? = null
    private var mFirstScroll = false
    private var mDragging = false
    private var mIntercepted = false
    private var mPrevMoveEvent: MotionEvent? = null
    private var mTouchInterceptionViewGroup: ViewGroup? = null
    private var mOriginalScrollListener: OnScrollListener? = null
    private val mScrollListener: OnScrollListener = object : OnScrollListener {
        override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
            if (mOriginalScrollListener != null) {
                mOriginalScrollListener!!.onScrollStateChanged(view, scrollState)
            }
        }

        override fun onScroll(
            view: AbsListView,
            firstVisibleItem: Int,
            visibleItemCount: Int,
            totalItemCount: Int
        ) {
            if (mOriginalScrollListener != null) {
                mOriginalScrollListener!!.onScroll(
                    view,
                    firstVisibleItem,
                    visibleItemCount,
                    totalItemCount
                )
            }
            // AbsListView#invokeOnItemScrollListener calls onScrollChanged(0, 0, 0, 0)
            // on Android 4.0+, but Android 2.3 is not. (Android 3.0 is unknown)
            // So call it with onScrollListener.
            onScrollChanged()
        }
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        mPrevFirstVisiblePosition = ss.prevFirstVisiblePosition
        mPrevFirstVisibleChildHeight = ss.prevFirstVisibleChildHeight
        mPrevScrolledChildrenHeight = ss.prevScrolledChildrenHeight
        mPrevScrollY = ss.prevScrollY
        mScrollY = ss.scrollY
        mChildrenHeights = ss.childrenHeights
        super.onRestoreInstanceState(ss.superState)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.prevFirstVisiblePosition = mPrevFirstVisiblePosition
        ss.prevFirstVisibleChildHeight = mPrevFirstVisibleChildHeight
        ss.prevScrolledChildrenHeight = mPrevScrolledChildrenHeight
        ss.prevScrollY = mPrevScrollY
        ss.scrollY = mScrollY
        ss.childrenHeights = mChildrenHeights
        return ss
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (mCallbacks != null) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    run {
                        mDragging = true
                        mFirstScroll = mDragging
                    }
                    mCallbacks!!.onDownMotionEvent()
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (mCallbacks != null) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mIntercepted = false
                    mDragging = false
                    mCallbacks!!.onUpOrCancelMotionEvent(mScrollState)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mPrevMoveEvent == null) {
                        mPrevMoveEvent = ev
                    }
                    val diffY = ev.y - mPrevMoveEvent!!.y
                    mPrevMoveEvent = MotionEvent.obtainNoHistory(ev)
                    if (mScrollY - diffY <= 0) {
                        // Can't scroll anymore.
                        if (mIntercepted) {
                            // Already dispatched ACTION_DOWN event to parents, so stop here.
                            return false
                        }

                        // Apps can set the interception target other than the direct parent.
                        val parent: ViewGroup = if (mTouchInterceptionViewGroup == null) {
                            parent as ViewGroup
                        } else {
                            mTouchInterceptionViewGroup as ViewGroup
                        }

                        // Get offset to parents. If the parent is not the direct parent,
                        // we should aggregate offsets from all of the parents.
                        var offsetX = 0f
                        var offsetY = 0f
                        var v: View = this
                        while (v !== parent) {
                            offsetX += v.left - v.scrollX.toFloat()
                            offsetY += v.top - v.scrollY.toFloat()
                            v = try {
                                v.parent as View
                            } catch (ex: ClassCastException) {
                                break
                            }
                        }
                        val event = MotionEvent.obtainNoHistory(ev)
                        event.offsetLocation(offsetX, offsetY)
                        if (parent.onInterceptTouchEvent(event)) {
                            mIntercepted = true

                            // If the parent wants to intercept ACTION_MOVE events,
                            // we pass ACTION_DOWN event to the parent
                            // as if these touch events just have began now.
                            event.action = MotionEvent.ACTION_DOWN

                            // Return this onTouchEvent() first and set ACTION_DOWN event for parent
                            // to the queue, to keep events sequence.
                            post { parent.dispatchTouchEvent(event) }
                            return false
                        }
                        // Even when this can't be scrolled anymore,
                        // simply returning false here may cause subView's click,
                        // so delegate it to super.
                        return super.onTouchEvent(ev)
                    }
                }
            }
        }
        return super.onTouchEvent(ev)
    }

    override fun setOnScrollListener(l: OnScrollListener) {
        // Don't set l to super.setOnScrollListener().
        // l receives all events through mScrollListener.
        mOriginalScrollListener = l
    }

    override fun setScrollViewCallbacks(listener: ObservableScrollViewCallbacks?) {
        mCallbacks = listener
    }

    override fun setTouchInterceptionViewGroup(viewGroup: ViewGroup?) {
        mTouchInterceptionViewGroup = viewGroup
    }

    override fun scrollVerticallyTo(y: Int) {
        val firstVisibleChild = getChildAt(0)
        if (firstVisibleChild != null) {
            val baseHeight = firstVisibleChild.height
            val position = y / baseHeight
            setSelection(position)
        }
    }

    override val currentScrollY: Int
        get() = mScrollY

    private fun init() {
        mChildrenHeights = SparseIntArray()
        super.setOnScrollListener(mScrollListener)
    }

    @Suppress("NAME_SHADOWING")
    private fun onScrollChanged() {
        if (mCallbacks != null) {
            if (childCount > 0) {
                val firstVisiblePosition = firstVisiblePosition
                var i = getFirstVisiblePosition()
                var j = 0
                while (i <= lastVisiblePosition) {
                    if (mChildrenHeights!!.indexOfKey(i) < 0 || getChildAt(j).height != mChildrenHeights!![i]) {
                        mChildrenHeights!!.put(i, getChildAt(j).height)
                    }
                    i++
                    j++
                }
                val firstVisibleChild = getChildAt(0)
                if (firstVisibleChild != null) {
                    when {
                        mPrevFirstVisiblePosition < firstVisiblePosition -> {
                            // scroll down
                            var skippedChildrenHeight = 0
                            if (firstVisiblePosition - mPrevFirstVisiblePosition != 1) {
                                for (i in firstVisiblePosition - 1 downTo mPrevFirstVisiblePosition + 1) {
                                    skippedChildrenHeight += if (0 < mChildrenHeights!!.indexOfKey(i)) {
                                        mChildrenHeights!![i]
                                    } else {
                                        // Approximate each item's height to the first visible child.
                                        // It may be incorrect, but without this, scrollY will be broken
                                        // when scrolling from the bottom.
                                        firstVisibleChild.height
                                    }
                                }
                            }
                            mPrevScrolledChildrenHeight += mPrevFirstVisibleChildHeight + skippedChildrenHeight
                            mPrevFirstVisibleChildHeight = firstVisibleChild.height
                        }
                        firstVisiblePosition < mPrevFirstVisiblePosition -> {
                            // scroll up
                            var skippedChildrenHeight = 0
                            if (mPrevFirstVisiblePosition - firstVisiblePosition != 1) {
                                for (i in mPrevFirstVisiblePosition - 1 downTo firstVisiblePosition + 1) {
                                    skippedChildrenHeight += if (0 < mChildrenHeights!!.indexOfKey(i)) {
                                        mChildrenHeights!![i]
                                    } else {
                                        // Approximate each item's height to the first visible child.
                                        // It may be incorrect, but without this, scrollY will be broken
                                        // when scrolling from the bottom.
                                        firstVisibleChild.height
                                    }
                                }
                            }
                            mPrevScrolledChildrenHeight -= firstVisibleChild.height + skippedChildrenHeight
                            mPrevFirstVisibleChildHeight = firstVisibleChild.height
                        }
                        firstVisiblePosition == 0 -> {
                            mPrevFirstVisibleChildHeight = firstVisibleChild.height
                        }
                    }
                    if (mPrevFirstVisibleChildHeight < 0) {
                        mPrevFirstVisibleChildHeight = 0
                    }
                    mScrollY = mPrevScrolledChildrenHeight - firstVisibleChild.top
                    mPrevFirstVisiblePosition = firstVisiblePosition
                    mCallbacks!!.onScrollChanged(mScrollY, mFirstScroll, mDragging)
                    if (mFirstScroll) {
                        mFirstScroll = false
                    }
                    mScrollState = when {
                        mPrevScrollY < mScrollY -> {
                            ScrollState.UP
                        }
                        mScrollY < mPrevScrollY -> {
                            ScrollState.DOWN
                        }
                        else -> {
                            ScrollState.STOP
                        }
                    }
                    mPrevScrollY = mScrollY
                }
            }
        }
    }

    internal class SavedState : BaseSavedState {
        var prevFirstVisiblePosition = 0
        var prevFirstVisibleChildHeight = -1
        var prevScrolledChildrenHeight = 0
        var prevScrollY = 0
        var scrollY = 0
        var childrenHeights: SparseIntArray? = null

        /**
         * Called by onSaveInstanceState.
         */
        constructor(superState: Parcelable?) : super(superState)

        /**
         * Called by CREATOR.
         */
        private constructor(`in`: Parcel) : super(`in`) {
            prevFirstVisiblePosition = `in`.readInt()
            prevFirstVisibleChildHeight = `in`.readInt()
            prevScrolledChildrenHeight = `in`.readInt()
            prevScrollY = `in`.readInt()
            scrollY = `in`.readInt()
            childrenHeights = SparseIntArray()
            val numOfChildren = `in`.readInt()
            if (0 < numOfChildren) {
                for (i in 0 until numOfChildren) {
                    val key = `in`.readInt()
                    val value = `in`.readInt()
                    childrenHeights!!.put(key, value)
                }
            }
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(prevFirstVisiblePosition)
            out.writeInt(prevFirstVisibleChildHeight)
            out.writeInt(prevScrolledChildrenHeight)
            out.writeInt(prevScrollY)
            out.writeInt(scrollY)
            val numOfChildren = if (childrenHeights == null) 0 else childrenHeights!!.size()
            out.writeInt(numOfChildren)
            if (0 < numOfChildren) {
                for (i in 0 until numOfChildren) {
                    out.writeInt(childrenHeights!!.keyAt(i))
                    out.writeInt(childrenHeights!!.valueAt(i))
                }
            }
        }

        companion object {
            @JvmField val CREATOR: Parcelable.Creator<SavedState?> = object : Parcelable.Creator<SavedState?> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}
