package com.wizpizz.wechatguard.hook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.MotionEvent
import com.highcapable.yukihookapi.hook.param.PackageParam

private const val TAG = "WeChatLockGuard"

const val PREF_ENABLED = "lock_guard_enabled"
const val PREF_GUARD_DURATION = "guard_duration_ms"
const val DEFAULT_GUARD_DURATION_MS = 500

object LockGuardHook {

    @Volatile
    private var screenOnTimeMs = 0L

    fun apply(packageParam: PackageParam) {
        packageParam.apply {
            val enabled = prefs.getBoolean(PREF_ENABLED, true)
            if (!enabled) {
                Log.d(TAG, "LockGuardHook is disabled, skipping")
                return
            }

            val guardDurationMs = prefs.getInt(PREF_GUARD_DURATION, DEFAULT_GUARD_DURATION_MS).toLong()
            Log.d(TAG, "LockGuardHook loaded, guardDuration=${guardDurationMs}ms")

            onAppLifecycle {
                onCreate {
                    val context: Context = this

                    // Listen for screen on event to record timestamp
                    val screenReceiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context, intent: Intent) {
                            if (intent.action == Intent.ACTION_SCREEN_ON) {
                                screenOnTimeMs = System.currentTimeMillis()
                                Log.d(TAG, "Screen on detected, blocking touches for ${guardDurationMs}ms")
                            }
                        }
                    }
                    context.registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
                    Log.d(TAG, "Screen receiver registered")
                }
            }

            // Hook dispatchTouchEvent on all Activity subclasses in WeChat
            "android.app.Activity".toClass().hook {
                injectMember {
                    method {
                        name = "dispatchTouchEvent"
                        param(MotionEvent::class.java)
                    }
                    beforeHook {
                        // Block touch within guard duration after screen turned on
                        val timeSinceScreenOn = System.currentTimeMillis() - screenOnTimeMs
                        if (screenOnTimeMs > 0 && timeSinceScreenOn < guardDurationMs) {
                            Log.d(TAG, "Within guard window (${timeSinceScreenOn}ms < ${guardDurationMs}ms), blocking touch")
                            result = true
                        }
                    }
                }
            }
        }
    }
}
