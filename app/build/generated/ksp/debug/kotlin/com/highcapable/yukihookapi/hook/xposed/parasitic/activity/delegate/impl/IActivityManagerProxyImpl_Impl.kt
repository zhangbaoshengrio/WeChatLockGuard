@file:Suppress("ClassName", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.highcapable.yukihookapi.hook.xposed.parasitic.activity.delegate.impl

import android.os.Handler
import com.highcapable.yukihookapi.hook.xposed.parasitic.activity.delegate.caller.IActivityManagerProxyCaller
import com.highcapable.yukihookapi.hook.xposed.parasitic.activity.delegate.IActivityManagerProxy_com_wizpizz_wechatguard
import java.lang.reflect.Proxy

/**
 * IActivityManagerProxyImpl_Impl Class
 *
 * Compiled from YukiHookXposedProcessor
 *
 * Generate Date: Apr 9, 2026, 9:21:18 AM
 *
 * Powered by YukiHookAPI (C) HighCapable 2019-2024
 *
 * Project URL: [YukiHookAPI](https://github.com/HighCapable/YukiHookAPI)
 */
object IActivityManagerProxyImpl_Impl {

    fun createWrapper(clazz: Class<*>?, instance: Any) = 
        Proxy.newProxyInstance(IActivityManagerProxyCaller.currentClassLoader, arrayOf(clazz), IActivityManagerProxy_com_wizpizz_wechatguard(instance))
}