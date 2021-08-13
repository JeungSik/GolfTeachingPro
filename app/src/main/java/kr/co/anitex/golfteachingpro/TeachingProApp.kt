package kr.co.anitex.golfteachingpro

import android.app.Application
import android.content.Context

/**
 * Created by nitinagarwal on 3/19/17.
 */
class TeachingProApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        private var instance: TeachingProApp? = null

        /**
         * @return the main context of the Application
         */
        val appContext: Context?
            get() = instance

        /**
         * @return the main resources from the Application
         */
        //val appResources: Resources
        //    get() = instance!!.resources
    }
}