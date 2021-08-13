package kr.co.anitex.golfteachingpro

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController

@Suppress("DEPRECATION")
class ReviewActivity : AppCompatActivity() {
    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        val args = intent.extras
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.review_fragment_container)
        val navController = navHostFragment!!.findNavController()
        if(args != null) {
            navController.setGraph(R.navigation.nav_graph_review, args)
        }
        container = findViewById(R.id.review_fragment_container)
        window.statusBarColor = resources.getColor(R.color.default_statusbar_color)
    }

    override fun onResume() {
        super.onResume()
        container.postDelayed({
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    companion object {
        /** Milliseconds used for UI animations */
        const val IMMERSIVE_FLAG_TIMEOUT = 500L
    }
}