package com.wizpizz.wechatguard.hook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import com.highcapable.yukihookapi.hook.param.PackageParam

private const val TAG = "WeChatLockGuard"

const val PREF_ENABLED = "lock_guard_enabled"
const val PREF_DELAY_MS = "screen_on_delay_ms"
const val DEFAULT_DELAY_MS = 0

object LockGuardHook {

    fun apply(packageParam: PackageParam) {
        packageParam.apply {
            val enabled = prefs.getBoolean(PREF_ENABLED, true)
            if (!enabled) {
                Log.d(TAG, "LockGuardHook is disabled, skipping")
                return
            }

            val delayMs = prefs.getInt(PREF_DELAY_MS, DEFAULT_DELAY_MS).toLong()
            Log.d(TAG, "LockGuardHook loaded in SystemUI, delayMs=$delayMs")

            // Hook SystemUI Application onCreate directly
            "com.android.systemui.SystemUIApplication".toClass().hook {
                injectMember {
                    method { name = "onCreate" }
                    afterHook {
                        val context = instance as? Context ?: return@afterHook
                        Log.d(TAG, "SystemUIApplication.onCreate hooked, setting up proximity guard")
                        setupProximityGuard(context, delayMs)
                    }
                }
            }
        }
    }

    private fun setupProximityGuard(context: Context, delayMs: Long) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        if (proximitySensor == null) {
            Log.e(TAG, "No proximity sensor found")
            return
        }

        val handler = Handler(context.mainLooper)

        val proximityListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val covered = event.values[0] < (proximitySensor.maximumRange / 2f + 0.1f)
                Log.d(TAG, "Proximity: ${event.values[0]}, covered=$covered, interactive=${powerManager?.isInteractive}")
                if (covered && powerManager?.isInteractive == true) {
                    Log.i(TAG, "In pocket while screen on, turning screen off")
                    turnScreenOff(powerManager)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        Log.d(TAG, "Screen ON, registering proximity listener after ${delayMs}ms")
                        handler.postDelayed({
                            if (powerManager?.isInteractive == true) {
                                sensorManager.registerListener(
                                    proximityListener,
                                    proximitySensor,
                                    SensorManager.SENSOR_DELAY_NORMAL,
                                    handler
                                )
                                Log.d(TAG, "Proximity listener registered")
                            }
                        }, delayMs)
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        handler.removeCallbacksAndMessages(null)
                        sensorManager.unregisterListener(proximityListener)
                        Log.d(TAG, "Screen OFF, proximity listener unregistered")
                    }
                }
            }
        }

        context.registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })

        Log.d(TAG, "Proximity guard initialized successfully")
    }

    private fun turnScreenOff(powerManager: PowerManager) {
        // Method 1: goToSleep with 3 args
        try {
            val m = PowerManager::class.java.getDeclaredMethod(
                "goToSleep", Long::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
            )
            m.isAccessible = true
            m.invoke(powerManager, System.currentTimeMillis(), 2, 0)
            Log.d(TAG, "goToSleep(3-arg) succeeded")
            return
        } catch (e: Exception) { Log.d(TAG, "goToSleep(3-arg) failed: ${e.message}") }

        // Method 2: goToSleep with 1 arg
        try {
            val m = PowerManager::class.java.getDeclaredMethod("goToSleep", Long::class.javaPrimitiveType)
            m.isAccessible = true
            m.invoke(powerManager, System.currentTimeMillis())
            Log.d(TAG, "goToSleep(1-arg) succeeded")
            return
        } catch (e: Exception) { Log.d(TAG, "goToSleep(1-arg) failed: ${e.message}") }

        // Method 3: IWindowManager.lockNow
        try {
            val sm = Class.forName("android.os.ServiceManager")
            val binder = sm.getDeclaredMethod("getService", String::class.java).invoke(null, "window")
            val stub = Class.forName("android.view.IWindowManager\$Stub")
            val wm = stub.getDeclaredMethod("asInterface", android.os.IBinder::class.java).invoke(null, binder)
            wm?.javaClass?.getDeclaredMethod("lockNow", android.os.Bundle::class.java)?.also {
                it.isAccessible = true
                it.invoke(wm, null)
            }
            Log.d(TAG, "lockNow succeeded")
            return
        } catch (e: Exception) { Log.d(TAG, "lockNow failed: ${e.message}") }

        Log.e(TAG, "All screen-off methods failed")
    }
}
