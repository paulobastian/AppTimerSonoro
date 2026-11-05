package com.sonoromove.timer

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RunState(
    val timer: TimerItem? = null,
    val phases: List<Phase> = emptyList(),
    val phaseIndex: Int = 0,
    val remainingMs: Long = 0,
    val elapsedMs: Long = 0,
    val running: Boolean = false,
    val finished: Boolean = false
) {
    val phase: Phase? get() = phases.getOrNull(phaseIndex)
    val isStopwatch get() = timer?.type == TimerType.STOPWATCH
    val active get() = timer != null && !finished
}

/** Motor único do timer: roda independente da tela e é mantido vivo pelo TimerService. */
object TimerEngine {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null
    private lateinit var app: Context
    private var settings = AppSettings()

    private val _state = MutableStateFlow(RunState())
    val state: StateFlow<RunState> = _state

    fun init(context: Context) {
        app = context.applicationContext
        SoundPlayer.init(app)
        settings = Store.loadSettings(app)
    }

    fun refreshSettings() { settings = Store.loadSettings(app) }

    fun load(timer: TimerItem) {
        job?.cancel()
        val phases = timer.phases()
        _state.value = RunState(
            timer = timer, phases = phases,
            remainingMs = (phases.firstOrNull()?.seconds ?: 0) * 1000L
        )
    }

    fun toggle() { if (_state.value.running) pause() else play() }

    fun play() {
        val s = _state.value
        if (s.timer == null || s.finished) return
        _state.value = s.copy(running = true)
        job?.cancel()
        job = scope.launch {
            var last = SystemClock.elapsedRealtime()
            while (true) {
                delay(50)
                val now = SystemClock.elapsedRealtime()
                val dt = now - last
                last = now
                tick(dt)
                if (!_state.value.running) break
            }
        }
    }

    fun pause() {
        job?.cancel()
        _state.value = _state.value.copy(running = false)
    }

    fun reset() { _state.value.timer?.let { load(it) } }

    fun stop() {
        job?.cancel()
        _state.value = RunState()
    }

    private fun tick(dt: Long) {
        val s = _state.value
        if (s.isStopwatch) {
            _state.value = s.copy(elapsedMs = s.elapsedMs + dt)
            return
        }
        val before = s.remainingMs
        var remaining = before - dt
        val elapsed = s.elapsedMs + dt
        if (remaining > 0) {
            if (settings.countdownBeeps) {
                val secBefore = (before + 999) / 1000
                val secNow = (remaining + 999) / 1000
                if (secNow != secBefore && secNow in 1..3) beep(short = true)
            }
            _state.value = s.copy(remainingMs = remaining, elapsedMs = elapsed)
            return
        }
        val next = s.phaseIndex + 1
        if (next >= s.phases.size) {
            job?.cancel()
            _state.value = s.copy(remainingMs = 0, elapsedMs = elapsed, running = false, finished = true)
            finishAlert()
        } else {
            remaining = s.phases[next].seconds * 1000L + remaining
            _state.value = s.copy(phaseIndex = next, remainingMs = remaining, elapsedMs = elapsed)
            beep(short = false)
        }
    }

    private fun beep(short: Boolean) {
        SoundPlayer.play(settings, if (short) 150 else 500)
        vibrate(if (short) 60 else 300)
    }

    private fun finishAlert() {
        SoundPlayer.play(settings, 1500, alternate = true)
        vibrate(1000)
    }

    private fun vibrate(ms: Long) {
        if (!settings.vibration) return
        try {
            val v: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            else @Suppress("DEPRECATION") app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            v?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Throwable) {
        }
    }
}
