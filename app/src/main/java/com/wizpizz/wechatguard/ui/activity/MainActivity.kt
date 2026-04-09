package com.wizpizz.wechatguard.ui.activity

import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.factory.prefs
import com.wizpizz.wechatguard.R
import com.wizpizz.wechatguard.hook.DEFAULT_GUARD_DURATION_MS
import com.wizpizz.wechatguard.hook.PREF_ENABLED
import com.wizpizz.wechatguard.hook.PREF_GUARD_DURATION

@Suppress("DEPRECATION")
class MainActivity : BaseActivity() {

    private val prefs by lazy { prefs() }

    override fun onCreate() {
        setContentView(R.layout.activity_main)

        val statusCard = findViewById<android.view.View>(R.id.main_lin_status)
        val statusText = findViewById<TextView>(R.id.main_text_status)
        val enableSwitch = findViewById<Switch>(R.id.enable_switch)
        val seekBar = findViewById<SeekBar>(R.id.duration_seek_bar)
        val durationText = findViewById<TextView>(R.id.duration_value_text)

        // Module status
        val activated = YukiHookAPI.Status.isXposedModuleActive
        statusCard.setBackgroundResource(if (activated) R.drawable.bg_green_round else R.drawable.bg_dark_round)
        statusText.text = getString(if (activated) R.string.module_is_activated else R.string.module_not_activated)

        // Enable toggle
        enableSwitch.isChecked = prefs.getBoolean(PREF_ENABLED, true)
        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.native().edit { putBoolean(PREF_ENABLED, isChecked) }
        }

        // Guard duration SeekBar: 100ms ~ 2000ms, step 100ms → max = 19
        val currentMs = prefs.getInt(PREF_GUARD_DURATION, DEFAULT_GUARD_DURATION_MS)
        seekBar.max = 19
        seekBar.progress = ((currentMs / 100) - 1).coerceIn(0, 19)
        durationText.text = getString(R.string.duration_value, currentMs)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                durationText.text = getString(R.string.duration_value, (progress + 1) * 100)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                prefs.native().edit { putInt(PREF_GUARD_DURATION, (sb.progress + 1) * 100) }
            }
        })
    }
}
