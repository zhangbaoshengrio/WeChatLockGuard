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
const val DEFAULT_DELAY_MS = 1000  // 1 second delay before proximity monitoring starts

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

            onAppLifecycle {
                onCreate {
                    val context: Context = this
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                    val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

                    if (proximitySensor == null) {
                        Log.e(TAG, "No proximity sensor found")
                        return@onCreate
                    }

                    val handler = Handler(context.mainLooper)

                    val proximityListener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            val covered = event.values[0] < (proximitySensor.maximumRange / 2f + 0.1f)
                            Log.d(TAG, "Proximity: ${event.values[0]}, covered=$covered")
                            if (covered && powerManager?.isInteractive == true) {
                                Log.i(TAG, "In pocket while screen on, turning screen off")
                                try {
                                    val goToSleep = PowerManager::class.java
                                        .getDeclaredMethod("goToSleep", Long::class.javaPrimitiveType)
                                    goToSleep.isAccessible = true
                                    goToSleep.invoke(powerManager, System.currentTimeMillis())
                                } catch (e: Exception) {
                                    Log.e(TAG, "goToSleep failed: ${e.message}")
                                }
                            }
                        }
                        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
                    }

                    val screenReceiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context, intent: Intent) {
                            when (intent.action) {
                                Intent.ACTION_SCREEN_ON -> {
                                    // Delay start so intentional unlocks don't immediately re-lock
                                    handler.postDelayed({
                                        if (powerManager?.isInteractive == true) {
                                            sensorManager.registerListener(
                                                proximityListener,
                                                proximitySensor,
                                                SensorManager.SENSOR_DELAY_NORMAL,
                                                handler
                                            )
                                            Log.d(TAG, "Screen on, proximity monitoring started")
                                        }
                                    }, delayMs)
                                }
                                Intent.ACTION_SCREEN_OFF -> {
                                    handler.removeCallbacksAndMessages(null)
                                    sensorManager.unregisterListener(proximityListener)
                                    Log.d(TAG, "Screen off, proximity monitoring stopped")
                                }
                            }
                        }
                    }

                    context.registerReceiver(screenReceiver, IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_ON)
                        addAction(Intent.ACTION_SCREEN_OFF)
                    })

                    Log.d(TAG, "Proximity-based screen guard initialized")
                }
            }
        }
    }
}
