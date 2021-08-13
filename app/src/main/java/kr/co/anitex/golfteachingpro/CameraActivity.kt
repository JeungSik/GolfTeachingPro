package kr.co.anitex.golfteachingpro

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

@Suppress("DEPRECATION")
class CameraActivity : AppCompatActivity() {
/*
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tfe_pn_activity_camera)
        savedInstanceState ?: supportFragmentManager.beginTransaction()
            .replace(R.id.container, PosenetActivity())
            .commit()
    }
*/
    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_camera)
        container = findViewById(R.id.camera_fragment_container)
        window.statusBarColor = resources.getColor(R.color.default_statusbar_color)
    }

    override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        //container.systemUiVisibility = FLAGS_FULLSCREEN

        container.postDelayed({
          //container.systemUiVisibility = FLAGS_FULLSCREEN
        }, IMMERSIVE_FLAG_TIMEOUT)

    }

    companion object {
        /** Combination of all flags required to put activity into immersive mode */
        //const val FLAGS_FULLSCREEN =
        //            View.SYSTEM_UI_FLAG_LOW_PROFILE or
        //            View.SYSTEM_UI_FLAG_FULLSCREEN or
        //            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        //            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        /** Milliseconds used for UI animations */
        //const val ANIMATION_FAST_MILLIS = 50L
        //const val ANIMATION_SLOW_MILLIS = 100L
        const val IMMERSIVE_FLAG_TIMEOUT = 500L
    }
}