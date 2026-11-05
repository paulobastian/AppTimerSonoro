package com.sonoromove.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

enum class SoundType(val label: String, val f1: Double, val f2: Double, val square: Boolean, val decay: Boolean = false) {
    BEEP("Bipe", 1000.0, 1000.0, true),
    DIGITAL("Digital", 1200.0, 1600.0, true),
    ALARM("Alarme", 800.0, 1100.0, true),
    WHISTLE("Apito", 2600.0, 3000.0, false),
    BELL("Sino", 880.0, 880.0, false, decay = true)
}

/**
 * Gera os sons por síntese e toca via AudioTrack, permitindo ajustar o volume e
 * aplicar um ganho extra digital. Opcionalmente toca também no alto-falante da orelha.
 */
object SoundPlayer {
    private const val RATE = 44100
    private val handler = Handler(Looper.getMainLooper())

    private var audio: AudioManager? = null

    fun init(c: Context) { audio = c.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    private fun minVol(a: AudioManager): Int =
        if (android.os.Build.VERSION.SDK_INT >= 28) a.getStreamMinVolume(AudioManager.STREAM_ALARM) else 0

    /** Volume do canal de alarme do sistema (0..1). O passo mais baixo do Android conta como 0 (mudo). */
    fun getSystemVolume(): Float {
        val a = audio ?: return 1f
        val min = minVol(a)
        val max = a.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        return if (max > min) (a.getStreamVolume(AudioManager.STREAM_ALARM) - min).toFloat() / (max - min) else 1f
    }

    /** Ajusta o volume do canal de alarme do sistema (0..1), igual aos botões físicos. */
    fun setSystemVolume(v: Float) {
        val a = audio ?: return
        val min = minVol(a)
        val max = a.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val t = (min + Math.round(v * (max - min))).coerceIn(min, max)
        try { a.setStreamVolume(AudioManager.STREAM_ALARM, t, 0) } catch (_: Throwable) {}
    }

    /** Botões físicos: sobe/desce um passo no canal de alarme. */
    fun adjust(up: Boolean) {
        val a = audio ?: return
        try {
            a.adjustStreamVolume(
                AudioManager.STREAM_ALARM,
                if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
        } catch (_: Throwable) {}
    }

    private fun muted(): Boolean {
        val a = audio ?: return false
        return a.getStreamVolume(AudioManager.STREAM_ALARM) <= minVol(a)
    }

    fun play(s: AppSettings, ms: Int, alternate: Boolean = false) {
        if (!s.sound || muted()) return
        try {
            // o volume vem do canal de alarme do sistema; aqui só aplicamos o ganho extra
            val pcm = synth(s.soundType, ms, alternate, 1f, s.boostDb)
            // alto-falante principal
            start(pcm, ms, AudioAttributes.USAGE_ALARM, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
        } catch (_: Throwable) {
        }
        if (s.earpiece) {
            try {
                val pcm = synth(s.soundType, ms, alternate, 1f, s.boostDb)
                // alto-falante da orelha (só roteia com uso de chamada de voz)
                start(pcm, ms, AudioAttributes.USAGE_VOICE_COMMUNICATION, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
            } catch (_: Throwable) {
            }
        }
    }

    private fun start(pcm: ShortArray, ms: Int, usage: Int, device: Int) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        audio?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.firstOrNull { it.type == device }
            ?.let { track.preferredDevice = it }
        track.write(pcm, 0, pcm.size)
        track.play()
        handler.postDelayed({
            try { track.release() } catch (_: Throwable) {}
        }, ms + 300L)
    }

    private fun synth(type: SoundType, ms: Int, alternate: Boolean, volume: Float, boostDb: Int): ShortArray {
        val gain = 10.0.pow(boostDb / 20.0)
        val n = RATE * ms / 1000
        val out = ShortArray(n)
        val fade = RATE * 5 / 1000
        val seg = RATE * 150 / 1000
        var phase = 0.0
        for (i in 0 until n) {
            val f = if (alternate && (i / seg) % 2 == 1) type.f2 else type.f1
            phase += 2 * PI * f / RATE
            var v = sin(phase)
            if (type.square) v = sign(v) * 0.9 + v * 0.1
            var env = 1.0
            if (type.decay) env = exp(-3.0 * i / n)
            if (i < fade) env *= i / fade.toDouble()
            if (i > n - fade) env *= (n - i) / fade.toDouble()
            // ganho extra com saturação: deixa a onda mais "cheia" e portanto mais alta
            val x = (v * env * gain).coerceIn(-1.0, 1.0)
            out[i] = (x * volume * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }
}
