package kr.co.anitex.golfteachingpro

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.activity_help.*

@Suppress("DEPRECATION")
class HelpActivity : AppCompatActivity() {
    @SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        val helptoolbar = findViewById<Toolbar>(R.id.helptoolbar)
        helptoolbar.setLogo(R.drawable.main_logo)
        setSupportActionBar(helptoolbar)
        window.statusBarColor = resources.getColor(R.color.default_statusbar_color)

        web_view.settings.javaScriptEnabled = true
        web_view.addJavascriptInterface(ConnectInterface(), "app")
        web_view.setBackgroundColor(Color.TRANSPARENT)
        web_view.webViewClient = WebViewClient()
        web_view.loadUrl("file:///android_asset/www/help.html")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode == KeyEvent.KEYCODE_BACK && web_view.canGoBack()) {
            web_view.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    inner class ConnectInterface {
        //@JavascriptInterface
        //fun getResString(name: String): String {
        //    val id = resources.getIdentifier(name, "string", packageName)
        //    return getString(id)
        //}
    }
}