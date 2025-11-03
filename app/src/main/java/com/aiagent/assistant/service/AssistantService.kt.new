package com.aiagent.assistant.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.aiagent.assistant.R
import com.aiagent.assistant.overlay.AssistantOverlay
import com.aiagent.assistant.speech.XunfeiSpeechManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import com.google.android.gms.tasks.Task

suspend fun <TResult> Task<TResult>.await(): TResult = suspendCoroutine { continuation ->
    addOnSuccessListener { result -> continuation.resume(result) }
    addOnFailureListener { exception -> continuation.resumeWithException(exception) }
}

class AssistantService : Service() {
    private val TAG = "AssistantService"
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private lateinit var speechManager: XunfeiSpeechManager
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var overlay: AssistantOverlay

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var screenDensity: Int = 0
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private val virtualDisplayName = "AIAssistant-ScreenCapture"
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Starting service initialization")
        initializeScreenMetrics()
        initializeOverlay()
        startForeground()
        initializeSpeech()
        initializeMediaProjection()
    }

    private fun initializeScreenMetrics() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenDensity = metrics.densityDpi
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
    }

    private fun initializeOverlay() {
        Log.d(TAG, "initializeOverlay: Creating overlay")
        overlay = AssistantOverlay(this)
        overlay.show()
        overlay.updateStatus("正在初始化AI助手...")
    }

    private fun startForeground() {
        val channelId = "assistant_service_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AI助手服务",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AI助手")
            .setContentText("助手服务正在运行")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // 先只启动带麦克风权限的服务
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun initializeSpeech() {
        speechManager = XunfeiSpeechManager(this)
        speechManager.setSpeechListener(object : XunfeiSpeechManager.SpeechListener {
            override fun onSpeechResult(text: String) {
                processCommand(text)
                speechManager.startListening() // 继续监听
            }

            override fun onSpeechError(error: String) {
                Log.e(TAG, "Speech error: $error")
                overlay.updateStatus("语音识别出错：$error")
                Handler(Looper.getMainLooper()).postDelayed({
                    speechManager.startListening()
                }, 3000)
            }

            override fun onTtsCompleted() {
                // TTS完成后自动开始监听
                speechManager.startListening()
            }
        })
        // 开始语音识别
        Handler(Looper.getMainLooper()).postDelayed({
            speak("你好，我是AI助手")
        }, 1000)
    }

    private fun initializeMediaProjection() {
        val prefs = getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE)
        val resultCode = prefs.getInt("media_projection_result_code", Activity.RESULT_CANCELED)
        val resultData = prefs.getString("media_projection_result_data", null)?.let {
            Intent.parseUri(it, Intent.URI_ALLOW_UNSAFE)
        }

        if (resultCode == Activity.RESULT_OK && resultData != null) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
        } else {
            Log.e(TAG, "No media projection permission")
        }
    }

    private fun processCommand(command: String) {
        overlay.updateStatus("收到命令：$command")
        when {
            command.contains("你好") -> {
                speak("你好，我是AI助手，有什么可以帮你的吗？")
            }
            command.contains("读取屏幕") -> {
                speak("正在分析屏幕内容")
                scope.launch {
                    analyzeScreen()
                }
            }
            command.contains("退出") || command.contains("关闭") -> {
                speak("正在关闭助手")
                stopSelf()
            }
            else -> {
                speak("抱歉，我还不知道怎么处理这个命令")
            }
        }
    }

    private fun speak(text: String) {
        Log.d(TAG, "Speaking: $text")
        overlay.updateStatus("正在说：$text")
        speechManager.speak(text)
    }

    private suspend fun analyzeScreen() {
        withContext(Dispatchers.IO) {
            try {
                val bitmap = captureScreen()
                if (bitmap != null) {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val result = textRecognizer.process(image).await()
                    val text = result.text
                    if (text.isNotEmpty()) {
                        speak("我找到了以下文字内容：$text")
                    } else {
                        speak("当前屏幕上没有找到文字内容")
                    }
                    bitmap.recycle()
                } else {
                    speak("无法获取屏幕内容，请检查相关权限")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing screen", e)
                speak("分析屏幕内容时出错")
            } finally {
                stopScreenCapture()
            }
        }
    }

    private fun captureScreen(): Bitmap? {
        try {
            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection not initialized")
                return null
            }

            imageReader = ImageReader.newInstance(
                screenWidth, screenHeight,
                PixelFormat.RGBA_8888, 2
            )

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                virtualDisplayName,
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, handler
            )

            // 等待一帧图像
            Thread.sleep(100)

            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * screenWidth

                val bitmap = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride,
                    screenHeight, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()
                return bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screen", e)
        }
        return null
    }

    private fun stopScreenCapture() {
        try {
            virtualDisplay?.release()
            imageReader?.close()
            virtualDisplay = null
            imageReader = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping screen capture", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlay.hide()
        speechManager.destroy()
        mediaProjection?.stop()
        virtualDisplay?.release()
        imageReader?.close()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
