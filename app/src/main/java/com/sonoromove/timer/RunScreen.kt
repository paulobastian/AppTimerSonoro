package com.sonoromove.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PrepColor = Color(0xFFF4FF3B)
private val RestColor = Color(0xFF4FC3F7)
private val RoundRestColor = Color(0xFFFFB74D)
private val Black = Color(0xFF0B0B0D)
private val RoundsBlue = Color(0xFF2196F3)
private val CyclesYellow = Color(0xFFFFE600)

private fun big(size: Int, color: Color = Color.Black) =
    TextStyle(fontFamily = Condensed, fontSize = size.sp, color = color)

private fun phaseColor(kind: PhaseKind?, workColor: Color) = when (kind) {
    PhaseKind.PREP -> PrepColor
    PhaseKind.REST -> RestColor
    PhaseKind.ROUND_REST -> RoundRestColor
    else -> workColor
}

private fun phaseTitle(kind: PhaseKind) = when (kind) {
    PhaseKind.PREP -> "PREPARAR"
    PhaseKind.WORK -> "EXERCÍCIO"
    PhaseKind.REST -> "DESCANSO"
    PhaseKind.ROUND_REST -> "DESCANSO DO ROUND"
}

@Composable
fun RunScreen(state: RunState, onToggle: () -> Unit, onReset: () -> Unit, onClose: () -> Unit) {
    val t = state.timer ?: return
    val workColor = Color(TimerColors[t.colorIdx % TimerColors.size])
    val phase = state.phase
    val next = state.phases.getOrNull(state.phaseIndex + 1)

    val mainColor = if (state.finished) workColor else phaseColor(phase?.kind, workColor)
    val mainLabel = when {
        state.finished -> "CONCLUÍDO"
        state.isStopwatch -> "CRONÔMETRO"
        else -> phase?.kind?.let { phaseTitle(it) } ?: ""
    }
    val seconds = if (state.isStopwatch) (state.elapsedMs / 1000).toInt()
    else ((state.remainingMs + 999) / 1000).toInt()

    val totalRemaining = when {
        state.isStopwatch -> seconds
        state.finished -> 0
        else -> seconds + state.phases.drop(state.phaseIndex + 1).sumOf { it.seconds }
    }
    val totalSeconds = t.totalSeconds().coerceAtLeast(1)
    val overall = if (state.isStopwatch) 0f else 1f - totalRemaining / totalSeconds.toFloat()

    val grouped = t.type == TimerType.ROUNDS || t.type == TimerType.TABATA
    val worksPerRound = t.rounds
    val totalSets = if (grouped) t.sets else 1
    val currentWork = phase?.round?.takeIf { it > 0 } ?: 1
    val currentSet = phase?.set ?: 1
    val showWorks = grouped && worksPerRound > 1
    val showSets = grouped && totalSets > 1
    val worksLeft = when {
        state.finished -> 0
        phase?.kind == PhaseKind.REST -> worksPerRound - currentWork
        phase?.kind == PhaseKind.WORK -> worksPerRound - currentWork + 1
        else -> worksPerRound
    }.coerceAtLeast(0)
    val setsLeft = when {
        state.finished -> 0
        phase?.kind == PhaseKind.ROUND_REST -> totalSets - currentSet
        phase?.kind == PhaseKind.PREP -> totalSets
        else -> totalSets - currentSet + 1
    }.coerceAtLeast(0)

    Column(Modifier.fillMaxSize().background(Black)) {
        // Barra superior
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Fechar", tint = CyclesYellow) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(t.name.uppercase(), style = big(14, CyclesYellow), maxLines = 1)
                Text(formatTime(totalRemaining), style = big(26, Color.White))
            }
            IconButton(onClick = onReset) { Icon(Icons.Default.Replay, "Reiniciar", tint = CyclesYellow) }
        }

        // Fase atual
        Column(
            Modifier.weight(1f).fillMaxWidth().background(mainColor),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            if (!state.finished && phase != null && phase.kind != PhaseKind.PREP) {
                val parts = listOfNotNull(
                    if (showSets) "ROUND ${currentSet} / $totalSets" else null,
                    if (showWorks && phase.kind != PhaseKind.ROUND_REST) "TRABALHO $currentWork / $worksPerRound" else null
                )
                if (parts.isNotEmpty()) Text(parts.joinToString("  ·  "), style = big(20))
            }
            Text(mainLabel, style = big(if (mainLabel.length > 12) 44 else 60), maxLines = 1)
            Text(formatTime(seconds), style = big(150), maxLines = 1, textAlign = TextAlign.Center)
        }

        // Próxima fase
        if (next != null && !state.finished) {
            Column(
                Modifier.fillMaxWidth().background(phaseColor(next.kind, workColor)).padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${phaseTitle(next.kind)}:", style = big(24))
                Text(formatTime(next.seconds), style = big(52))
            }
        }

        // Barra inferior
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (showWorks) Stat("$worksLeft", "TRABALHOS PARA TERMINAR", RoundsBlue)
            }
            val started = state.elapsedMs > 0
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(84.dp).clip(CircleShape).clickable(enabled = !state.finished, onClick = onToggle),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { overall.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxSize(),
                        color = CyclesYellow, trackColor = Color(0xFF2A2A31), strokeWidth = 7.dp
                    )
                    Icon(
                        if (state.running) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Iniciar/Pausar", tint = CyclesYellow, modifier = Modifier.size(40.dp)
                    )
                }
                Text(
                    when {
                        state.running -> "PAUSAR"
                        started -> "RETOMAR"
                        else -> "INICIAR"
                    },
                    style = big(14, CyclesYellow), modifier = Modifier.padding(top = 2.dp)
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (showSets) Stat("$setsLeft", "ROUNDS PARA TERMINAR", CyclesYellow)
                else if (showWorks) Stat("$currentWork", "TRABALHO ATUAL", CyclesYellow)
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = big(56, color))
        Text(label, style = big(10, Color.White), textAlign = TextAlign.Center, maxLines = 2)
    }
}
