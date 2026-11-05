package com.sonoromove.timer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class TimerType(val label: String) {
    COUNTDOWN("Contagem regressiva"),
    STOPWATCH("Cronômetro"),
    ROUNDS("Rounds"),
    TABATA("Tabata")
}

enum class PhaseKind(val label: String) { PREP("PREPARAR"), WORK("TRABALHO"), REST("DESCANSO"), ROUND_REST("DESCANSO DO ROUND") }

data class Phase(val kind: PhaseKind, val seconds: Int, val round: Int, val set: Int = 1)

data class TimerItem(
    val id: Long,
    val name: String,
    val type: TimerType,
    val colorIdx: Int = 0,
    val prep: Int = 0,
    val work: Int = 30,
    val rest: Int = 0,
    val rounds: Int = 1,
    val sets: Int = 1,
    val roundRest: Int = 0
) {
    fun phases(): List<Phase> {
        if (type == TimerType.STOPWATCH) return emptyList()
        val list = mutableListOf<Phase>()
        if (prep > 0) list += Phase(PhaseKind.PREP, prep, 0)
        val n = if (type == TimerType.COUNTDOWN) 1 else rounds
        val total = if (type == TimerType.COUNTDOWN) 1 else sets
        for (set in 1..total) {
            for (r in 1..n) {
                list += Phase(PhaseKind.WORK, work, r, set)
                if (rest > 0 && r < n) list += Phase(PhaseKind.REST, rest, r, set)
            }
            if (roundRest > 0 && set < total) list += Phase(PhaseKind.ROUND_REST, roundRest, n, set)
        }
        return list
    }

    fun totalSeconds(): Int = phases().sumOf { it.seconds }

    fun toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("type", type.name); put("color", colorIdx)
        put("prep", prep); put("work", work); put("rest", rest); put("rounds", rounds); put("sets", sets); put("roundRest", roundRest)
    }

    companion object {
        fun fromJson(o: JSONObject) = TimerItem(
            o.getLong("id"), o.getString("name"), TimerType.valueOf(o.getString("type")),
            o.optInt("color"), o.optInt("prep"), o.optInt("work", 30), o.optInt("rest"), o.optInt("rounds", 1),
            o.optInt("sets", 1), o.optInt("roundRest")
        )
    }
}

val TimerColors = listOf(
    0xFF4CE34C, 0xFFFFE600, 0xFF2196F3, 0xFFFF5722, 0xFFE040FB, 0xFF00E5FF
)

fun formatTime(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

/** Modelos prontos exibidos na aba "Modelos". */
val Templates = listOf(
    TimerItem(0, "Tabata", TimerType.TABATA, 0, prep = 10, work = 20, rest = 10, rounds = 8),
    TimerItem(0, "EMOM 10 min", TimerType.ROUNDS, 2, prep = 5, work = 60, rest = 0, rounds = 10),
    TimerItem(0, "HIIT 40/20", TimerType.ROUNDS, 3, prep = 10, work = 40, rest = 20, rounds = 10),
    TimerItem(0, "Boxe 3 min", TimerType.ROUNDS, 1, prep = 10, work = 180, rest = 60, rounds = 12),
    TimerItem(0, "Descanso 1 min", TimerType.COUNTDOWN, 5, work = 60),
    TimerItem(0, "Cronômetro", TimerType.STOPWATCH, 1)
)

data class AppSettings(
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val countdownBeeps: Boolean = true,
    val soundType: SoundType = SoundType.BEEP,
    val volume: Float = 1f,
    val boostDb: Int = 10,
    val earpiece: Boolean = false
)

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("timerplus", Context.MODE_PRIVATE)

    fun loadTimers(c: Context): List<TimerItem> {
        val raw = prefs(c).getString("timers", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { TimerItem.fromJson(arr.getJSONObject(it)) }
    }

    fun saveTimers(c: Context, list: List<TimerItem>) {
        val arr = JSONArray().also { a -> list.forEach { a.put(it.toJson()) } }
        prefs(c).edit().putString("timers", arr.toString()).apply()
    }

    fun loadSettings(c: Context): AppSettings {
        val p = prefs(c)
        return AppSettings(
            p.getBoolean("sound", true), p.getBoolean("vibration", true), p.getBoolean("beeps", true),
            SoundType.entries.firstOrNull { it.name == p.getString("soundType", null) } ?: SoundType.BEEP,
            p.getFloat("volume", 1f), p.getInt("boostDb", 10), p.getBoolean("earpiece", false)
        )
    }

    fun saveSettings(c: Context, s: AppSettings) {
        prefs(c).edit().putBoolean("sound", s.sound).putBoolean("vibration", s.vibration)
            .putBoolean("beeps", s.countdownBeeps).putString("soundType", s.soundType.name)
            .putFloat("volume", s.volume).putInt("boostDb", s.boostDb)
            .putBoolean("earpiece", s.earpiece).apply()
    }
}
