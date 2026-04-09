package com.wizpizz.wechatguard.hook

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {

    override fun onHook() = encase {
        configs {
            debugLog {
                tag = "WeChatLockGuard"
            }
        }

        loadApp(name = "com.tencent.mm") {
            LockGuardHook.apply(this)
        }
    }
}
