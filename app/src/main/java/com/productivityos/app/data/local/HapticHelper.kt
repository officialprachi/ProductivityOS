package com.productivityos.app.data.local

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Short tick — button clicks, toggles
    fun click() = vibrate(50L, VibrationEffect.EFFECT_CLICK)

    // Double pulse — focus session start
    fun focusStart() = vibratePattern(longArrayOf(0, 80, 60, 80))

    // Long pulse + fade — focus session end / schedule applied
    fun success() = vibratePattern(longArrayOf(0, 60, 40, 120))

    // Sharp buzz — alert / warning
    fun warning() = vibrate(120L, VibrationEffect.EFFECT_HEAVY_CLICK)

    // ── Internals ─────────────────────────────────────────────

    private fun vibrate(ms: Long, effectId: Int) {
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(
            VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    private fun vibratePattern(pattern: LongArray) {
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(
            VibrationEffect.createWaveform(pattern, -1)
        )
    }
}
