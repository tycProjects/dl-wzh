package com.example.infinitestairsauto

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    private val captureRequest = 701
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(28,36,28,24) }
        root.addView(TextView(this).apply { text="Infinite Stairs AUTO Vision"; textSize=24f })
        root.addView(TextView(this).apply {
            text="실험적 화면 인식 버전\n할로윈 스킨의 주황색 계단 타일을 영상에서 찾아 다음 진행 방향을 추정합니다."
            textSize=15f; setPadding(0,18,0,18)
        })
        root.addView(Button(this).apply {
            text="1. 접근성 서비스 설정"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text="2. 화면 인식 권한 허용 및 시작"
            setOnClickListener {
                val mgr=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(mgr.createScreenCaptureIntent(),captureRequest)
            }
        })
        root.addView(Button(this).apply {
            text="자동 입력 정지"
            setOnClickListener {
                getSharedPreferences("vision",MODE_PRIVATE).edit().putBoolean("running",false).apply()
                stopService(Intent(this@MainActivity,CaptureService::class.java))
                Toast.makeText(this@MainActivity,"정지 요청 완료",Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(TextView(this).apply {
            text="주의: 색상 기반 컴퓨터 비전 실험판이며 학습된 AI 모델은 아닙니다. 현재 영상에서 보이는 주황색 타일을 기준으로 추정하므로 다른 스킨·배경·이펙트에서는 오판할 수 있습니다. 먼저 낮은 속도에서 테스트하세요. 화면 인식 권한과 접근성 권한은 사용자가 직접 승인해야 합니다."
            textSize=13f; setPadding(0,22,0,0)
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }
    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==captureRequest && resultCode==RESULT_OK && data!=null) {
            val i=Intent(this,CaptureService::class.java).apply {
                putExtra("resultCode",resultCode); putExtra("data",data)
            }
            startForegroundService(i)
            getSharedPreferences("vision",MODE_PRIVATE).edit().putBoolean("running",true).apply()
            Toast.makeText(this,"화면 인식 시작됨. 게임으로 이동하세요.",Toast.LENGTH_LONG).show()
        }
    }
}
