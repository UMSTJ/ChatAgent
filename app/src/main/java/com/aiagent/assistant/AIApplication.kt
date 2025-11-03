package com.aiagent.assistant

import android.app.Application
import com.iflytek.cloud.SpeechUtility

class AIApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化科大讯飞SDK
        SpeechUtility.createUtility(this, "appid=" + getString(R.string.xunfei_app_id))
    }
}
