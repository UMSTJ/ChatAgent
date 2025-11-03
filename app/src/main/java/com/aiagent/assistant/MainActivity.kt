package com.aiagent.assistant

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aiagent.assistant.service.AssistantService

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (android.provider.Settings.canDrawOverlays(this)) {
            checkAndRequestPermissions()
        } else {
            statusText.text = "需要悬浮窗权限才能显示助手界面"
        }
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // 保存权限结果以供服务使用
            getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE).edit()
                .putInt("media_projection_result_code", result.resultCode)
                .putString("media_projection_result_data", result.data?.toUri(Intent.URI_ALLOW_UNSAFE))
                .apply()
            startAssistantService()
        } else {
            statusText.text = "需要屏幕录制权限才能分析屏幕内容"
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        startButton.setOnClickListener {
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        // 检查悬浮窗权限
        if (!android.provider.Settings.canDrawOverlays(this)) {
            statusText.text = "请在设置中授予悬浮窗权限"
            // 跳转到悬浮窗权限设置页面
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
            return
        }

        // 检查其他运行时权限
        if (!checkRecordPermission()) {
            requestRecordPermission()
        } else {
            startAssistantService()
        }
    }

    private fun checkRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == 
            PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            RECORD_PERMISSION_REQUEST_CODE
        )
    }

    private fun startAssistantService() {
        val intent = Intent(this, AssistantService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        statusText.text = "助手服务已启动"
        moveTaskToBack(true)
    }

    private fun requestMediaProjectionPermission() {
        mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAndRequestPermissions() // 重新检查所有权限
            } else {
                statusText.text = "需要录音权限才能启动语音助手"
            }
        }
    }

    companion object {
        private const val RECORD_PERMISSION_REQUEST_CODE = 123
        private const val SPEECH_REQUEST_CODE = 789
    }
}
