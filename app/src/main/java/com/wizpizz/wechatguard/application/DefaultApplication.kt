package com.wizpizz.wechatguard.application

import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.xposed.application.ModuleApplication

class DefaultApplication : ModuleApplication() {
    override fun onCreate() {
        super.onCreate()
        YukiHookAPI.encase(this)
    }
}
