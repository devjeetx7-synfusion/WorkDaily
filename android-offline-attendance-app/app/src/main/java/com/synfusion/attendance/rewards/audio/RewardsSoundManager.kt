package com.synfusion.attendance.rewards.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.synfusion.attendance.R
import java.util.concurrent.ConcurrentHashMap

enum class RewardsSoundEvent {
    SPIN_START,
    SPIN_TICK,
    SPIN_STOP,
    COIN_REWARD,
    BONUS_SPIN,
    DAILY_CHECKIN_SUCCESS,
    SEVEN_DAY_STREAK,
    MILESTONE_CLAIM,
    BADGE_UNLOCK,
    CONGRATULATIONS,
    BUTTON_TAP,
    ACTION_ERROR,
    REFERRAL_COPY
}

data class RewardsAudioSettings(
    val isSoundEnabled: Boolean = true,
    val isMusicEnabled: Boolean = false,
    val isHapticEnabled: Boolean = true
)

class RewardsSoundManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("rewards_audio_prefs", Context.MODE_PRIVATE)

    var settings: RewardsAudioSettings = loadSettings()
        private set

    private val soundPool: SoundPool
    private val soundMap = ConcurrentHashMap<RewardsSoundEvent, Int>()
    private var lastTickTimestamp: Long = 0L

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        preloadSounds()
    }

    private fun preloadSounds() {
        soundMap[RewardsSoundEvent.SPIN_START] = soundPool.load(appContext, R.raw.reward_spin_start, 1)
        soundMap[RewardsSoundEvent.SPIN_TICK] = soundPool.load(appContext, R.raw.reward_spin_tick, 1)
        soundMap[RewardsSoundEvent.SPIN_STOP] = soundPool.load(appContext, R.raw.reward_spin_stop, 1)
        soundMap[RewardsSoundEvent.COIN_REWARD] = soundPool.load(appContext, R.raw.reward_coin_collect, 1)
        soundMap[RewardsSoundEvent.BONUS_SPIN] = soundPool.load(appContext, R.raw.reward_bonus_spin, 1)
        soundMap[RewardsSoundEvent.DAILY_CHECKIN_SUCCESS] = soundPool.load(appContext, R.raw.reward_checkin_success, 1)
        soundMap[RewardsSoundEvent.SEVEN_DAY_STREAK] = soundPool.load(appContext, R.raw.reward_streak_bonus, 1)
        soundMap[RewardsSoundEvent.MILESTONE_CLAIM] = soundPool.load(appContext, R.raw.reward_milestone_unlock, 1)
        soundMap[RewardsSoundEvent.BADGE_UNLOCK] = soundPool.load(appContext, R.raw.reward_badge_unlock, 1)
        soundMap[RewardsSoundEvent.CONGRATULATIONS] = soundPool.load(appContext, R.raw.reward_congratulations, 1)
        soundMap[RewardsSoundEvent.BUTTON_TAP] = soundPool.load(appContext, R.raw.reward_button_tap, 1)
        soundMap[RewardsSoundEvent.ACTION_ERROR] = soundPool.load(appContext, R.raw.reward_error, 1)
        soundMap[RewardsSoundEvent.REFERRAL_COPY] = soundPool.load(appContext, R.raw.reward_referral_copy, 1)
    }

    private fun loadSettings(): RewardsAudioSettings {
        return RewardsAudioSettings(
            isSoundEnabled = prefs.getBoolean("sound_enabled", true),
            isMusicEnabled = prefs.getBoolean("music_enabled", false),
            isHapticEnabled = prefs.getBoolean("haptic_enabled", true)
        )
    }

    fun updateSettings(newSettings: RewardsAudioSettings) {
        settings = newSettings
        prefs.edit()
            .putBoolean("sound_enabled", newSettings.isSoundEnabled)
            .putBoolean("music_enabled", newSettings.isMusicEnabled)
            .putBoolean("haptic_enabled", newSettings.isHapticEnabled)
            .apply()
    }

    fun playSound(event: RewardsSoundEvent) {
        if (!settings.isSoundEnabled) return

        // Throttle high-frequency ticks to avoid audio clutter (min 35ms apart)
        if (event == RewardsSoundEvent.SPIN_TICK) {
            val now = System.currentTimeMillis()
            if (now - lastTickTimestamp < 35) return
            lastTickTimestamp = now
        }

        val soundId = soundMap[event] ?: return
        if (soundId != 0) {
            val volume = when (event) {
                RewardsSoundEvent.SPIN_TICK -> 0.4f
                RewardsSoundEvent.BUTTON_TAP -> 0.3f
                else -> 0.85f
            }
            soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
        }

        performHaptic(event)
    }

    fun performHaptic(event: RewardsSoundEvent) {
        if (!settings.isHapticEnabled || vibrator == null || !vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (event) {
                    RewardsSoundEvent.SPIN_TICK -> VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE)
                    RewardsSoundEvent.SPIN_STOP -> VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
                    RewardsSoundEvent.BUTTON_TAP, RewardsSoundEvent.REFERRAL_COPY -> VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE)
                    RewardsSoundEvent.COIN_REWARD, RewardsSoundEvent.DAILY_CHECKIN_SUCCESS -> VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                    RewardsSoundEvent.MILESTONE_CLAIM, RewardsSoundEvent.CONGRATULATIONS, RewardsSoundEvent.SEVEN_DAY_STREAK ->
                        VibrationEffect.createWaveform(longArrayOf(0, 30, 50, 40), -1)
                    RewardsSoundEvent.ACTION_ERROR -> VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                    else -> VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
            }
        } catch (_: Exception) {
            // Ignore haptic failures gracefully
        }
    }

    fun release() {
        soundPool.release()
        instance = null
    }

    companion object {
        @Volatile
        private var instance: RewardsSoundManager? = null

        fun getInstance(context: Context): RewardsSoundManager {
            return instance ?: synchronized(this) {
                instance ?: RewardsSoundManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
