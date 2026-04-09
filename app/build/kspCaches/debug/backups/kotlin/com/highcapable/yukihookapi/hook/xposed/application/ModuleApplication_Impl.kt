@file:Suppress("ClassName")

package com.highcapable.yukihookapi.hook.xposed.application

import com.wizpizz.wechatguard.hook.HookEntry

/**
 * ModuleApplication_Impl Class
 *
 * Compiled from YukiHookXposedProcessor
 *
 * Generate Date: Apr 9, 2026, 9:21:18 AM
 *
 * Powered by YukiHookAPI (C) HighCapable 2019-2024
 *
 * Project URL: [YukiHookAPI](https://github.com/HighCapable/YukiHookAPI)
 */
object ModuleApplication_Impl {

    fun callHookEntryInit() = try {
        HookEntry.onInit()
    } catch (_: Throwable) {
    }
}