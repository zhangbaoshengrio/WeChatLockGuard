package com.wizpizz.wechatguard.ui.activity

import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.factory.prefs
import com.wizpizz.wechatguard.R
import com.wizpizz.wechatguard.hook.DEFAULT_DELAY_MS
import com.wizpizz.wechatguard.hook.PREF_DELAY_MS
import com.wizpizz.wechatguard.hook.PREF_ENABLED

@Suppress("DEPRECATION")
class MainActivity : BaseActivity() {

    private val prefs by lazy { prefs() }

    override fun onCreate() {
        setContentView(R.layout.activity_main)

        val statusCard = findViewById<android.view.View>(R.id.main_lin_status)
        val statusText = findViewById<TextView>(R.id.main_text_status)
        val enableSwitch = findViewById<Switch>(R.id.enable_switch)
        val seekBar = findViewById<SeekBar>(R.id.delay_seek_bar)
        val delayText = findViewById<TextView>(R.id.delay_value_text)

        // Module status
        val activated = YukiHookAPI.Status.isXposedModuleActive
        statusCard.setBackgroundResource(if (activated) R.drawable.bg_green_round else R.drawable.bg_dark_round)
        statusText.text = getString(if (activated) R.string.module_is_activated else R.string.module_not_activated)

        // Enable toggle
        enableSwitch.isChecked = prefs.getBoolean(PREF_ENABLED, true)
        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.native().edit { putBoolean(PREF_ENABLED, isChecked) }
        }

        // Delay SeekBar: 0ms ~ 3000ms, step 100ms → max = 30
        val currentMs = prefs.getInt(PREF_DELAY_MS, DEFAULT_DELAY_MS)
        seekBar.max = 30
        seekBar.progress = (currentMs / 100).coerceIn(0, 30)
        delayText.text = getString(R.string.delay_value, currentMs)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                delayText.text = getString(R.string.delay_value, progress * 100)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                prefs.native().edit { putInt(PREF_DELAY_MS, sb.progress * 100) }
            }
        })
    }
}
