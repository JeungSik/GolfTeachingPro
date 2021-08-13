package kr.co.anitex.golfteachingpro.videolistmanager

import android.os.Bundle
import android.view.View

/**
 * Created by nitinagarwal on 4/9/17.
 */
interface ViewMvp {
    val rootView: View?
    val viewState: Bundle?
}