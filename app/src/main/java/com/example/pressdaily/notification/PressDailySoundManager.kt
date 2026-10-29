package com.example.pressdaily.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.pressdaily.data.model.AlarmSoundOption
import com.example.pressdaily.data.model.SoundType
import kotlin.concurrent.thread
import kotlin.math.sin

object PressDailySoundManager {
    private var currentRingtone: Ringtone? = null
    private var currentAudioTrack: AudioTrack? = null
    private var isPlayingLoop = false
    private val handler = Handler(Looper.getMainLooper())
    private var vibrator: Vibrator? = null

    private fun getVibrator(context: Context): Vibrator? {
        if (vibrator == null) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
        return vibrator
    }

    /**
     * Preview a ringtone for 3-4 seconds
     */
    fun playPreview(context: Context, sound: AlarmSoundOption) {
        stopSound()

        if (sound == AlarmSoundOption.SILENT) {
            return
        }

        if (sound == AlarmSoundOption.VIBRATE_ONLY) {
            vibrateAlert(context, repeat = false)
            return
        }

        vibrateAlert(context, repeat = false)

        when (sound.soundType) {
            SoundType.SYSTEM_ALARM -> playSystemSound(context, RingtoneManager.TYPE_ALARM, isPreview = true)
            SoundType.SYSTEM_RINGTONE -> playSystemSound(context, RingtoneManager.TYPE_RINGTONE, isPreview = true)
            SoundType.SYSTEM_NOTIFICATION -> playSystemSound(context, RingtoneManager.TYPE_NOTIFICATION, isPreview = true)
            SoundType.MELODY_ESPRESSO -> playSynthesizedMelody(listOf(523.25, 659.25, 783.99, 987.77, 1046.50), isPreview = true)
            SoundType.MELODY_SUNRISE -> playSynthesizedMelody(listOf(440.0, 554.37, 659.25, 880.0, 1108.73), isPreview = true)
            SoundType.MELODY_ZEN -> playSynthesizedMelody(listOf(261.63, 392.00, 523.25), isPreview = true)
            SoundType.MELODY_HARP -> playSynthesizedMelody(listOf(349.23, 440.0, 523.25, 698.46, 880.0), isPreview = true)
            SoundType.MELODY_ALARM -> playSynthesizedMelody(listOf(880.0, 1046.5, 880.0, 1046.5, 880.0), isPreview = true)
            SoundType.VIBRATE, SoundType.SILENT -> {}
        }
    }

    /**
     * Start continuous looping alarm for full-screen alarm screen
     */
    fun startAlarmLoop(context: Context, sound: AlarmSoundOption) {
        stopSound()
        isPlayingLoop = true

        if (sound == AlarmSoundOption.SILENT) return

        vibrateAlert(context, repeat = true)

        if (sound == AlarmSoundOption.VIBRATE_ONLY) return

        when (sound.soundType) {
            SoundType.SYSTEM_ALARM -> playSystemSound(context, RingtoneManager.TYPE_ALARM, isPreview = false)
            SoundType.SYSTEM_RINGTONE -> playSystemSound(context, RingtoneManager.TYPE_RINGTONE, isPreview = false)
            SoundType.SYSTEM_NOTIFICATION -> playSystemSound(context, RingtoneManager.TYPE_NOTIFICATION, isPreview = false)
            SoundType.MELODY_ESPRESSO -> playSynthesizedMelody(listOf(523.25, 659.25, 783.99, 987.77, 1046.50), isPreview = false)
            SoundType.MELODY_SUNRISE -> playSynthesizedMelody(listOf(440.0, 554.37, 659.25, 880.0, 1108.73), isPreview = false)
            SoundType.MELODY_ZEN -> playSynthesizedMelody(listOf(261.63, 392.00, 523.25), isPreview = false)
            SoundType.MELODY_HARP -> playSynthesizedMelody(listOf(349.23, 440.0, 523.25, 698.46, 880.0), isPreview = false)
            SoundType.MELODY_ALARM -> playSynthesizedMelody(listOf(880.0, 1046.5, 880.0, 1046.5, 880.0), isPreview = false)
            SoundType.VIBRATE, SoundType.SILENT -> {}
        }
    }

    private fun playSystemSound(context: Context, ringtoneType: Int, isPreview: Boolean) {
        try {
            val uri: Uri = RingtoneManager.getDefaultUri(ringtoneType)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return

            val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = !isPreview
            }

            ringtone.play()
            currentRingtone = ringtone

            if (isPreview) {
                handler.postDelayed({
                    stopSound()
                }, 3500)
            }
        } catch (_: Exception) {}
    }

    private fun playSynthesizedMelody(frequencies: List<Double>, isPreview: Boolean) {
        thread {
            try {
                val sampleRate = 44100
                val noteDurationMs = 280
                val totalSamplesPerNote = (sampleRate * noteDurationMs) / 1000

                val buffer = ArrayList<Short>()
                frequencies.forEach { freq ->
                    for (i in 0 until totalSamplesPerNote) {
                        val time = i.toDouble() / sampleRate
                        val envelope = (1.0 - (i.toDouble() / totalSamplesPerNote)) // Linear decay
                        val sampleValue = (sin(2.0 * Math.PI * freq * time) * 28000 * envelope).toInt()
                        buffer.add(sampleValue.toShort())
                    }
                }

                val pcmData = ShortArray(buffer.size)
                for (i in buffer.indices) {
                    pcmData[i] = buffer[i]
                }

                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcmData.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(pcmData, 0, pcmData.size)
                currentAudioTrack = audioTrack

                if (isPreview) {
                    audioTrack.play()
                    Thread.sleep(noteDurationMs * frequencies.size.toLong() + 300)
                    stopSound()
                } else {
                    audioTrack.setLoopPoints(0, pcmData.size, AudioTrack.MODE_STATIC)
                    audioTrack.play()
                }
            } catch (_: Exception) {}
        }
    }

    private fun vibrateAlert(context: Context, repeat: Boolean) {
        try {
            val vib = getVibrator(context) ?: return
            val pattern = longArrayOf(0, 400, 200, 400, 200, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pattern, if (repeat) 0 else -1))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, if (repeat) 0 else -1)
            }
        } catch (_: Exception) {}
    }

    fun stopAlarm() {
        stopSound()
    }

    fun stopSound() {
        isPlayingLoop = false
        handler.removeCallbacksAndMessages(null)

        try {
            currentRingtone?.stop()
            currentRingtone = null
        } catch (_: Exception) {}

        try {
            currentAudioTrack?.stop()
            currentAudioTrack?.release()
            currentAudioTrack = null
        } catch (_: Exception) {}

        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }
}
