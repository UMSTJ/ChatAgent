package com.aiagent.assistant.speech

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.aiagent.assistant.R
import com.iflytek.cloud.*
import java.util.*

class XunfeiSpeechManager(private val context: Context) {
    private var mIat: SpeechRecognizer? = null
    private var mTts: SpeechSynthesizer? = null
    private var listener: SpeechListener? = null

    companion object {
        private const val TAG = "XunfeiSpeechManager"
    }

    interface SpeechListener {
        fun onSpeechResult(text: String)
        fun onSpeechError(error: String)
        fun onTtsCompleted()
    }

    init {
        // 初始化即创建语音配置
        val appId = context.getString(R.string.xunfei_app_id)
        SpeechUtility.createUtility(context, SpeechConstant.APPID + "=" + appId)
        initRecognizer()
        initSynthesizer()
    }

    private fun initRecognizer() {
        mIat = SpeechRecognizer.createRecognizer(context) { code ->
            if (code != ErrorCode.SUCCESS) {
                Log.e(TAG, "语音识别器初始化失败: ${code}")
            }
        }

        // 设置语音识别参数
        mIat?.setParameter(SpeechConstant.DOMAIN, "iat")
        mIat?.setParameter(SpeechConstant.LANGUAGE, "zh_cn")
        mIat?.setParameter(SpeechConstant.ACCENT, "mandarin")
        mIat?.setParameter(SpeechConstant.VAD_BOS, "4000")
        mIat?.setParameter(SpeechConstant.VAD_EOS, "1000")
        mIat?.setParameter(SpeechConstant.ASR_PTT, "1")
    }

    private fun initSynthesizer() {
        mTts = SpeechSynthesizer.createSynthesizer(context) { code ->
            if (code != ErrorCode.SUCCESS) {
                Log.e(TAG, "语音合成器初始化失败: ${code}")
            }
        }

        // 设置语音合成参数
        mTts?.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan")
        mTts?.setParameter(SpeechConstant.SPEED, "50")
        mTts?.setParameter(SpeechConstant.VOLUME, "80")
        mTts?.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD)
        mTts?.setParameter(SpeechConstant.TTS_AUDIO_PATH, null)
    }

    fun startListening() {
        if (mIat == null) {
            listener?.onSpeechError("语音识别器未初始化")
            return
        }

        mIat?.startListening(object : RecognizerListener {
            private val stringBuilder = StringBuilder()

            override fun onVolumeChanged(volume: Int, bytes: ByteArray) {}

            override fun onBeginOfSpeech() {}

            override fun onEndOfSpeech() {}

            override fun onResult(results: RecognizerResult, isLast: Boolean) {
                val text = parseIatResult(results.resultString)
                stringBuilder.append(text)
                
                if (isLast) {
                    val finalResult = stringBuilder.toString()
                    stringBuilder.clear()
                    listener?.onSpeechResult(finalResult)
                }
            }

            override fun onError(error: SpeechError) {
                listener?.onSpeechError(error.getPlainDescription(true))
            }

            override fun onEvent(eventType: Int, arg1: Int, arg2: Int, obj: Bundle?) {}
        })
    }

    fun stopListening() {
        mIat?.stopListening()
    }

    fun speak(text: String) {
        mTts?.startSpeaking(text, object : SynthesizerListener {
            override fun onSpeakBegin() {}

            override fun onSpeakPaused() {}

            override fun onSpeakResumed() {}

            override fun onBufferProgress(percent: Int, beginPos: Int, endPos: Int, info: String?) {}

            override fun onSpeakProgress(percent: Int, beginPos: Int, endPos: Int) {}

            override fun onCompleted(error: SpeechError?) {
                if (error == null) {
                    listener?.onTtsCompleted()
                } else {
                    listener?.onSpeechError(error.getPlainDescription(true))
                }
            }

            override fun onEvent(eventType: Int, arg1: Int, arg2: Int, obj: Bundle?) {}
        })
    }

    private fun parseIatResult(json: String): String {
        val ret = StringBuilder()
        try {
            org.json.JSONTokener(json).nextValue()?.let { result ->
                if (result is org.json.JSONObject) {
                    val ws = result.getJSONArray("ws")
                    for (i in 0 until ws.length()) {
                        val words = ws.getJSONObject(i)
                        val cw = words.getJSONArray("cw")
                        for (j in 0 until cw.length()) {
                            val obj = cw.getJSONObject(j)
                            ret.append(obj.getString("w"))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析语音识别结果失败", e)
        }
        return ret.toString()
    }

    fun setSpeechListener(listener: SpeechListener) {
        this.listener = listener
    }

    fun destroy() {
        mIat?.cancel()
        mIat?.destroy()
        mTts?.stopSpeaking()
        mTts?.destroy()
    }
}
