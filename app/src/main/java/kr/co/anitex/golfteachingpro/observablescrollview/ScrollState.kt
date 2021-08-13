package kr.co.anitex.golfteachingpro.observablescrollview

/**
 * Constants that indicates the scroll state of the Scrollable widgets.
 */
enum class ScrollState {
    /**
     * Widget is stopped.
     * This state does not always mean that this widget have never been scrolled.
     */
    STOP,

    /**
     * Widget is scrolled up by swiping it down.
     */
    UP,

    /**
     * Widget is scrolled down by swiping it up.
     */
    DOWN
}
