package kr.co.anitex.golfteachingpro.observablescrollview

import android.view.ViewGroup

/**
 * Provides common API for observable and scrollable widgets.
 */
interface Scrollable {
    /**
     * Sets a callback listener.
     *
     * @param listener listener to set
     */
    fun setScrollViewCallbacks(listener: ObservableScrollViewCallbacks?)

    /**
     * Scrolls vertically to the absolute Y.
     * Implemented classes are expected to scroll to the exact Y pixels from the top,
     * but it depends on the type of the widget.
     *
     * @param y vertical position to scroll to
     */
    fun scrollVerticallyTo(y: Int)

    /**
     * Returns the current Y of the scrollable view.
     *
     * @return current Y pixel
     */
    val currentScrollY: Int

    /**
     * Sets a touch motion event delegation ViewGroup.
     * This is used to pass motion events back to parent view.
     * It's up to the implementation classes whether or not it works.
     *
     * @param viewGroup ViewGroup object to dispatch motion events
     */
    fun setTouchInterceptionViewGroup(viewGroup: ViewGroup?)
}