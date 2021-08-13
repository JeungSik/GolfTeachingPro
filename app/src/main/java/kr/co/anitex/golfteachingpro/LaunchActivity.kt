package kr.co.anitex.golfteachingpro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat

@Suppress("DEPRECATION", "NAME_SHADOWING")
class LaunchActivity : AppCompatActivity() {
    // 퍼미션 응답 처리 코드
    private val multiplePermissionsCode = 100

    //필요한 퍼미션 리스트
    private val requiredPermissions = arrayOf(
        //Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.CAMERA,
    )
    //Manifest.permission.ACCESS_MEDIA_LOCATION

    private fun hasPermission(): Boolean {
        // 각 퍼미션들의 허가 상태여부 확인
        for( permission in requiredPermissions ) {
            // 미허가된 퍼미션이 있는 경우 false 리턴
            if( ContextCompat.checkSelfPermission(this, permission ) == PackageManager.PERMISSION_DENIED ) {
                return false
            }
        }
        // 모든 퍼미션이 허가된 경우 true 리턴
        return true
    }

    private fun exitAlertDialog() {
        val builder = AlertDialog.Builder(this )
        builder.setTitle("경고")
        builder.setMessage("이 앱은 카라메 및 쓰기 권한이 필요합니다.")
        builder.setCancelable(false)
        builder.setPositiveButton("확인") { _, _ ->  finish() }
        builder.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //overridePendingTransition(R.transition.fadein, R.transition.fadeout)
        setContentView(R.layout.activity_launch)

        val launchtoolbar = findViewById<Toolbar>(R.id.launchtoolbar)
        launchtoolbar.setLogo(R.drawable.main_robo)
        setSupportActionBar(launchtoolbar)
        window.statusBarColor = resources.getColor(R.color.default_statusbar_color)

        // 퍼미션 상태확인
        if (!hasPermission()) {
            // 퍼미션 허가 안되어 있으면 사용자에게 요청
            requestPermissions(requiredPermissions, multiplePermissionsCode)
        } else {
            // 모든 퍼미션이 허가된 후 실행되어야 할 코드
            onMainActivity()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var all_grented = true

        when( requestCode ){
            multiplePermissionsCode -> {
                if(grantResults.isNotEmpty()) {
                    for((i, permissions) in permissions.withIndex()) {
                        if(grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            // 권한획득 실패
                            Log.i("PERMISSION", "The user has denied to $permissions permission!")
                            all_grented = false
                        }
                    }
                }
            }
        }

        if(all_grented){
            // 모든 퍼미션이 허가된 후 실행되어야 할 코드
            onMainActivity()
        }
        else {
            exitAlertDialog()
        }
    }

    private fun onMainActivity() {
        Handler().postDelayed({
            val intent = Intent(this, MainActivity::class.java)
//            intent.setAction(Intent.ACTION_MAIN)
//            intent.addCategory(Intent.CATEGORY_HOME)
//            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
//            System.exit(0)
        }, 1500)
    }
}