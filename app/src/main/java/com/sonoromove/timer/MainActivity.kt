package com.sonoromove.timer

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

private sealed interface Screen {
    data object Splash : Screen
    data object Home : Screen
    data class Edit(val timer: TimerItem?) : Screen
    data object Run : Screen
}

class MainActivity : ComponentActivity() {
    // Garante que os botões físicos sempre mexam no volume de alarme (o canal do som do timer)
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            SoundPlayer.adjust(keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        TimerEngine.init(this)
        // botões físicos de volume passam a controlar o volume de alarme (o canal em que o som toca)
        volumeControlStream = android.media.AudioManager.STREAM_ALARM
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            AppTheme {
                val ctx = LocalContext.current
                var screen by remember {
                    mutableStateOf<Screen>(if (TimerEngine.state.value.active) Screen.Run else Screen.Splash)
                }
                var timers by remember { mutableStateOf(Store.loadTimers(ctx)) }
                var settings by remember { mutableStateOf(Store.loadSettings(ctx)) }
                val run by TimerEngine.state.collectAsState()

                val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                LaunchedEffect(Unit) {
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    if (screen == Screen.Splash) {
                        delay(1500)
                        screen = Screen.Home
                    }
                }

                fun persist(list: List<TimerItem>) {
                    timers = list
                    Store.saveTimers(ctx, list)
                }

                when (val s = screen) {
                    Screen.Splash -> SplashScreen()
                    Screen.Home -> HomeScreen(
                        timers = timers,
                        onRun = {
                            TimerEngine.load(it)
                            TimerService.start(ctx)
                            screen = Screen.Run
                        },
                        onNew = { screen = Screen.Edit(it?.copy(id = 0)) },
                        onEdit = { screen = Screen.Edit(it) },
                        onDuplicate = { t ->
                            persist(timers + t.copy(id = System.currentTimeMillis(), name = t.name + " (cópia)"))
                        },
                        onDelete = { t -> persist(timers.filter { it.id != t.id }) },
                        settings = settings,
                        onSettings = {
                            settings = it
                            Store.saveSettings(ctx, it)
                            TimerEngine.refreshSettings()
                        }
                    )
                    is Screen.Edit -> {
                        BackHandler { screen = Screen.Home }
                        EditScreen(
                            initial = s.timer,
                            onSave = { t ->
                                val list = if (t.id == 0L) timers + t.copy(id = System.currentTimeMillis())
                                else timers.map { if (it.id == t.id) t else it }
                                persist(list)
                                screen = Screen.Home
                            },
                            onCancel = { screen = Screen.Home }
                        )
                    }
                    Screen.Run -> {
                        BackHandler {
                            TimerEngine.stop()
                            screen = Screen.Home
                        }
                        RunScreen(
                            state = run,
                            onToggle = { TimerEngine.toggle() },
                            onReset = { TimerEngine.reset() },
                            onClose = {
                                TimerEngine.stop()
                                screen = Screen.Home
                            }
                        )
                    }
                }
            }
        }
    }
}
