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
const val DEFAULT_DELAY_MS = 0  // no delay, start monitoring immediately on screen on

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
                                // Try multiple methods in order of preference
                                var success = false

                                // Method 1: goToSleep with reason flag (Android 10+)
                                if (!success) try {
                                    val goToSleep = PowerManager::class.java.getDeclaredMethod(
                                        "goToSleep", Long::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
                                    )
                                    goToSleep.isAccessible = true
                                    goToSleep.invoke(powerManager, System.currentTimeMillis(), 2 /* GO_TO_SLEEP_REASON_TIMEOUT */, 0)
                                    success = true
                                    Log.d(TAG, "goToSleep(3-arg) succeeded")
                                } catch (e: Exception) {
                                    Log.d(TAG, "goToSleep(3-arg) failed: ${e.message}")
                                }

                                // Method 2: goToSleep simple
                                if (!success) try {
                                    val goToSleep = PowerManager::class.java.getDeclaredMethod(
                                        "goToSleep", Long::class.javaPrimitiveType
                                    )
                                    goToSleep.isAccessible = true
                                    goToSleep.invoke(powerManager, System.currentTimeMillis())
                                    success = true
                                    Log.d(TAG, "goToSleep(1-arg) succeeded")
                                } catch (e: Exception) {
                                    Log.d(TAG, "goToSleep(1-arg) failed: ${e.message}")
                                }

                                // Method 3: IWindowManager.lockNow via service
                                if (!success) try {
                                    val serviceManager = Class.forName("android.os.ServiceManager")
                                    val getService = serviceManager.getDeclaredMethod("getService", String::class.java)
                                    val wmBinder = getService.invoke(null, "window")
                                    val stub = Class.forName("android.view.IWindowManager\$Stub")
                                    val asInterface = stub.getDeclaredMethod("asInterface", android.os.IBinder::class.java)
                                    val wm = asInterface.invoke(null, wmBinder)
                                    val lockNow = wm?.javaClass?.getDeclaredMethod("lockNow", android.os.Bundle::class.java)
                                    lockNow?.isAccessible = true
                                    lockNow?.invoke(wm, null)
                                    success = true
                                    Log.d(TAG, "lockNow succeeded")
                                } catch (e: Exception) {
                                    Log.d(TAG, "lockNow failed: ${e.message}")
                                }

                                if (!success) Log.e(TAG, "All screen-off methods failed")
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
