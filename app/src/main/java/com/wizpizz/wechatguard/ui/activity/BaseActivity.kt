package com.wizpizz.wechatguard.ui.activity

import com.highcapable.yukihookapi.hook.xposed.parasitic.activity.base.ModuleAppCompatActivity

abstract class BaseActivity : ModuleAppCompatActivity() {
    abstract fun onCreate()
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        onCreate()
    }
}
