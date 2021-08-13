package kr.co.anitex.golfteachingpro

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var mAdView : AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the AdMob app
        MobileAds.initialize(this) {}

        //mAdView = findViewById(R.id.adView)
        //val adRequest = AdRequest.Builder().build()
        //mAdView.loadAd(adRequest)

        val maintoolbar = findViewById<Toolbar>(R.id.main_toolbar)
        maintoolbar.setLogo(R.drawable.main_robo)
        setSupportActionBar(maintoolbar)
        window.statusBarColor = resources.getColor(R.color.default_statusbar_color)

        findViewById<ImageView>(R.id.cameraImage).setOnClickListener { view ->
            Snackbar.make(view, "Video recording and Motion analysis", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            onCameraActivity()
        }

        findViewById<ImageView>(R.id.folderImage).setOnClickListener { view ->
            Snackbar.make(view, "View saved video", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            onVideoListActivity()
        }

        /*
        findViewById<ImageView>(R.id.referenceImage).setOnClickListener { view ->
            Snackbar.make(view, "Pro golfers' action reference", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
        */

        findViewById<ImageView>(R.id.helpImage).setOnClickListener { view ->
            Snackbar.make(view, "Get help", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            onHelpActivity()
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            /*
            R.id.menu_settings -> {
                return true
            }
            */

            R.id.menu_exit -> {
                //moveTaskToBack(true)
                //exitProcess(-1)
                finishAffinity()
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onCameraActivity() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }

    private fun onVideoListActivity() {
        val intent = Intent(this, VideoListActivity::class.java)
        startActivity(intent)
    }

    private fun onHelpActivity() {
        val intent = Intent(this, HelpActivity::class.java)
        startActivity(intent)
    }
}