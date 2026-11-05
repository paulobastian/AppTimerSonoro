package com.sonoromove.timer

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Neon = Color(0xFF8BF500)
val Bg = Color(0xFF1B1B22)
val Card = Color(0xFF2A2A31)
val Condensed = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))

private fun cond(size: Int, color: Color = Color.White) =
    TextStyle(fontFamily = Condensed, fontSize = size.sp, color = color)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Neon, onPrimary = Color.Black, background = Bg, surface = Bg,
            onSurface = Color.White, surfaceVariant = Card
        ),
        content = content
    )
}

// ---------------------------------------------------------------- Splash

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(220.dp).clip(CircleShape).background(Neon.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.clip(RoundedCornerShape(14.dp)).background(Neon).width(140.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "TIMER", Modifier.fillMaxWidth().background(Color(0xFF2B2B30)).padding(vertical = 4.dp),
                    style = cond(22, Color(0xFFFFE600)), textAlign = TextAlign.Center
                )
                Text("00:30", Modifier.padding(vertical = 14.dp), style = cond(42, Color(0xFF1B1B22)))
            }
        }
    }
}

// ---------------------------------------------------------------- Home

@Composable
fun HomeScreen(
    timers: List<TimerItem>,
    onRun: (TimerItem) -> Unit,
    onNew: (TimerItem?) -> Unit,
    onEdit: (TimerItem) -> Unit,
    onDuplicate: (TimerItem) -> Unit,
    onDelete: (TimerItem) -> Unit,
    settings: AppSettings,
    onSettings: (AppSettings) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Bg,
        topBar = {
            Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val title = listOf("OS MEUS TIMERS", "MODELOS", "DEFINIÇÕES")[tab]
                    Text(title, style = cond(22))
                    if (tab == 0) {
                        IconButton(
                            onClick = { searching = !searching; if (!searching) query = "" },
                            modifier = Modifier.align(Alignment.CenterEnd).clip(CircleShape).background(Color(0xFF55556A))
                        ) { Icon(Icons.Default.Search, null, tint = Color.White) }
                    }
                }
                if (searching && tab == 0) {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        placeholder = { Text("Pesquisar") }, singleLine = true
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Card) {
                val items = listOf(
                    Triple("Os meus timers", Icons.Default.Favorite, 0),
                    Triple("Modelos", Icons.Default.DirectionsRun, 1),
                    Triple("Definições", Icons.Default.Settings, 2)
                )
                items.forEach { (label, icon, i) ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Icon(icon, null) },
                        label = { Text(label, style = TextStyle(fontSize = 11.sp)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black, indicatorColor = Neon,
                            selectedTextColor = Neon, unselectedIconColor = Color.White,
                            unselectedTextColor = Color.White
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == 0) FloatingActionButton(
                onClick = { onNew(null) }, containerColor = Neon, contentColor = Color.Black, shape = CircleShape
            ) { Icon(Icons.Default.Add, "Novo timer") }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> {
                    val shown = timers.filter { it.name.contains(query, ignoreCase = true) }
                    if (shown.isEmpty()) {
                        Text(
                            if (timers.isEmpty()) "Nenhum timer ainda.\nToque em + para criar um\nou escolha um modelo." else "Nada encontrado.",
                            Modifier.align(Alignment.Center), style = cond(18, Color.Gray), textAlign = TextAlign.Center
                        )
                    }
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(shown, key = { it.id }) { t ->
                            TimerRow(t, { onRun(t) }, { onEdit(t) }, { onDuplicate(t) }, { onDelete(t) })
                        }
                    }
                }
                1 -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(Templates) { t ->
                        TimerRowBase(t, summary(t), onClick = { onNew(t) }) {
                            Icon(Icons.Default.Add, null, tint = Neon)
                        }
                    }
                }
                else -> SettingsTab(settings, onSettings)
            }
        }
    }
}

private fun summary(t: TimerItem): String = when (t.type) {
    TimerType.STOPWATCH -> "Cronômetro"
    TimerType.COUNTDOWN -> formatTime(t.work)
    else -> (if (t.sets > 1) "${t.sets} rounds · " else "") + "${t.rounds}x ${formatTime(t.work)}" +
        (if (t.rest > 0) " / ${formatTime(t.rest)}" else "") +
        (if (t.sets > 1 && t.roundRest > 0) " · desc. round ${formatTime(t.roundRest)}" else "")
}

@Composable
private fun TimerRowBase(t: TimerItem, sub: String, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    val color = Color(TimerColors[t.colorIdx % TimerColors.size])
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Card).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(5.dp).height(64.dp).background(color))
        Spacer(Modifier.width(12.dp))
        if (t.type == TimerType.STOPWATCH) {
            Icon(Icons.Default.Timer, null, tint = color, modifier = Modifier.padding(end = 8.dp))
        } else {
            Text(
                formatTime(t.totalSeconds()), style = cond(13, color), modifier = Modifier.padding(end = 8.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(t.name.uppercase(), style = cond(20), maxLines = 1)
            Text(sub, style = cond(12, Color.Gray))
        }
        trailing()
        Spacer(Modifier.width(6.dp))
    }
}

@Composable
private fun TimerRow(t: TimerItem, onRun: () -> Unit, onEdit: () -> Unit, onDup: () -> Unit, onDel: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    TimerRowBase(t, "${t.type.label} · ${summary(t)}", onRun) {
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreHoriz, null, tint = Color.White) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Editar") }, onClick = { menu = false; onEdit() },
                    leadingIcon = { Icon(Icons.Default.Edit, null) })
                DropdownMenuItem(text = { Text("Duplicar") }, onClick = { menu = false; onDup() },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                DropdownMenuItem(text = { Text("Excluir") }, onClick = { menu = false; onDel() },
                    leadingIcon = { Icon(Icons.Default.Delete, null) })
            }
        }
    }
}

@Composable
private fun SettingsTab(s: AppSettings, onChange: (AppSettings) -> Unit) {
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        SettingSwitch("Som", s.sound) { onChange(s.copy(sound = it)) }
        SettingSwitch("Vibração", s.vibration) { onChange(s.copy(vibration = it)) }
        SettingSwitch("Bipes nos últimos 3 segundos", s.countdownBeeps) { onChange(s.copy(countdownBeeps = it)) }

        Text("SOM", style = cond(14, Color.Gray), modifier = Modifier.padding(top = 12.dp))
        Row(Modifier.horizontalScroll2(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoundType.entries.forEach { t ->
                FilterChip(selected = s.soundType == t,
                    onClick = {
                        val n = s.copy(soundType = t)
                        onChange(n)
                        SoundPlayer.play(n.copy(sound = true), 500)
                    },
                    label = { Text(t.label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon, selectedLabelColor = Color.Black))
            }
        }

        val ctx = androidx.compose.ui.platform.LocalContext.current
        var sysVol by remember { mutableFloatStateOf(SoundPlayer.getSystemVolume()) }
        DisposableEffect(ctx) {
            val r = object : android.content.BroadcastReceiver() {
                override fun onReceive(c: android.content.Context?, i: android.content.Intent?) {
                    sysVol = SoundPlayer.getSystemVolume()
                }
            }
            val f = android.content.IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            if (android.os.Build.VERSION.SDK_INT >= 33)
                ctx.registerReceiver(r, f, android.content.Context.RECEIVER_EXPORTED)
            else ctx.registerReceiver(r, f)
            onDispose { try { ctx.unregisterReceiver(r) } catch (_: Throwable) {} }
        }
        SliderSetting("Volume", "${(sysVol * 100).toInt()}%", sysVol, 0f..1f) {
            sysVol = it
            SoundPlayer.setSystemVolume(it)
        }
        SliderSetting("Aumento extra de volume", if (s.boostDb == 0) "desligado" else "+${s.boostDb} dB",
            s.boostDb / 20f, 0f..1f, steps = 19) { onChange(s.copy(boostDb = (it * 20).toInt())) }

        SettingSwitch("Tocar também no alto-falante da orelha", s.earpiece) { onChange(s.copy(earpiece = it)) }

        Button(
            onClick = { SoundPlayer.play(s.copy(sound = true), 1500, alternate = true) },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Color.Black)
        ) { Text("TESTAR SOM", style = cond(18, Color.Black)) }
        Text(
            "O som toca no volume de alarme do aparelho. Se ainda estiver baixo, aumente o volume de alarme nas configurações do Android e use o aumento extra.",
            style = cond(12, Color.Gray), modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun SliderSetting(
    label: String, value: String, v: Float, range: ClosedFloatingPointRange<Float>,
    steps: Int = 0, onChange: (Float) -> Unit
) {
    Column(Modifier.padding(top = 12.dp)) {
        Row {
            Text(label.uppercase(), Modifier.weight(1f), style = cond(14, Color.Gray))
            Text(value, style = cond(14, Neon))
        }
        Slider(
            value = v, onValueChange = onChange, valueRange = range, steps = steps,
            colors = SliderDefaults.colors(thumbColor = Neon, activeTrackColor = Neon)
        )
    }
}

@Composable
private fun SettingSwitch(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = cond(18))
        Switch(checked = value, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Neon, checkedThumbColor = Color.Black))
    }
}

// ---------------------------------------------------------------- Editor

@Composable
fun EditScreen(initial: TimerItem?, onSave: (TimerItem) -> Unit, onCancel: () -> Unit) {
    val base = initial ?: TimerItem(0, "", TimerType.ROUNDS, 0, 5, 30, 10, 8)
    var name by remember { mutableStateOf(base.name) }
    var type by remember { mutableStateOf(base.type) }
    var color by remember { mutableIntStateOf(base.colorIdx) }
    var prep by remember { mutableIntStateOf(base.prep) }
    var work by remember { mutableIntStateOf(base.work) }
    var rest by remember { mutableIntStateOf(base.rest) }
    var rounds by remember { mutableIntStateOf(base.rounds) }
    var sets by remember { mutableIntStateOf(base.sets) }
    var roundRest by remember { mutableIntStateOf(base.roundRest) }

    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Fechar", tint = Color.White) }
            Text(if (initial != null && initial.id != 0L) "EDITAR TIMER" else "NOVO TIMER", style = cond(22))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nome") }, singleLine = true)

            Text("TIPO", style = cond(14, Color.Gray))
            Row(Modifier.horizontalScroll2(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimerType.entries.forEach { t ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon, selectedLabelColor = Color.Black))
                }
            }

            if (type != TimerType.STOPWATCH) {
                if (type != TimerType.COUNTDOWN) DurationField("Preparação", prep, 0) { prep = it }
                DurationField(if (type == TimerType.COUNTDOWN) "Duração" else "Trabalho", work, 1) { work = it }
                if (type == TimerType.TABATA || type == TimerType.ROUNDS) {
                    DurationField("Descanso", rest, 0) { rest = it }
                    CountField("Trabalhos por round", rounds) { rounds = it }
                    CountField("Rounds", sets) { sets = it }
                    if (sets > 1) DurationField("Descanso entre rounds", roundRest, 0) { roundRest = it }
                }
            }

            Text("COR", style = cond(14, Color.Gray))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimerColors.forEachIndexed { i, c ->
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color(c))
                            .then(if (i == color) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                            .clickable { color = i }
                    )
                }
            }
        }
        Button(
            onClick = {
                val finalName = name.ifBlank { type.label }
                onSave(base.copy(name = finalName, type = type, colorIdx = color, prep = prep, work = work, rest = rest, rounds = rounds, sets = sets, roundRest = roundRest))
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Color.Black)
        ) { Text("SALVAR", style = cond(20, Color.Black)) }
    }
}

@Composable
private fun Modifier.horizontalScroll2(): Modifier =
    this.then(Modifier.horizontalScroll(rememberScrollState()))

@Composable
private fun DurationField(label: String, seconds: Int, min: Int, onChange: (Int) -> Unit) {
    Column {
        Text(label.uppercase(), style = cond(14, Color.Gray))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StepBtn("-1m", 6) { onChange((seconds - 60).coerceAtLeast(min)) }
            StepBtn("-5s", 6) { onChange((seconds - 5).coerceAtLeast(min)) }
            StepBtn("-1s", 6) { onChange((seconds - 1).coerceAtLeast(min)) }
            Text(formatTime(seconds), Modifier.weight(1f), style = cond(32), textAlign = TextAlign.Center)
            StepBtn("+1s", 6) { onChange(seconds + 1) }
            StepBtn("+5s", 6) { onChange(seconds + 5) }
            StepBtn("+1m", 6) { onChange(seconds + 60) }
        }
    }
}

@Composable
private fun CountField(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Text(label.uppercase(), style = cond(14, Color.Gray))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StepBtn("-1") { onChange((value - 1).coerceAtLeast(1)) }
            Text("$value", Modifier.weight(1f), style = cond(32), textAlign = TextAlign.Center)
            StepBtn("+1") { onChange(value + 1) }
        }
    }
}

@Composable
private fun StepBtn(text: String, hPad: Int = 10, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(Card).clickable(onClick = onClick)
            .padding(horizontal = hPad.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, style = cond(15, Neon)) }
}

