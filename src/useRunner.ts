import { createAudioPlayer, setAudioModeAsync } from 'expo-audio';
import * as Haptics from 'expo-haptics';
import * as Notifications from 'expo-notifications';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { buildPhases, formatTime, PHASE_LABEL, Phase, Settings, TimerItem } from './types';

export interface RunState {
  phaseIndex: number;
  remainingMs: number;
  elapsedMs: number;
  running: boolean;
  finished: boolean;
}

const players = {
  short: createAudioPlayer(require('../assets/beep-short.wav')),
  long: createAudioPlayer(require('../assets/beep-long.wav')),
  end: createAudioPlayer(require('../assets/beep-end.wav')),
};

async function play(name: keyof typeof players) {
  try {
    await players[name].seekTo(0);
    players[name].play();
  } catch {}
}

const initial = (phases: Phase[]): RunState => ({
  phaseIndex: 0,
  remainingMs: (phases[0]?.seconds ?? 0) * 1000,
  elapsedMs: 0,
  running: false,
  finished: false,
});

export function useRunner(timer: TimerItem, settings: Settings) {
  const phases = useMemo(() => buildPhases(timer), [timer]);
  const stopwatch = timer.type === 'STOPWATCH';
  const [state, setState] = useState<RunState>(() => initial(phases));
  const ref = useRef(state);
  const settingsRef = useRef(settings);
  settingsRef.current = settings;

  const commit = (s: RunState) => {
    ref.current = s;
    setState(s);
  };

  useEffect(() => {
    setAudioModeAsync({ playsInSilentMode: true }).catch(() => {});
    return () => {
      Notifications.cancelAllScheduledNotificationsAsync().catch(() => {});
    };
  }, []);

  const buzz = (kind: 'short' | 'long' | 'end') => {
    const st = settingsRef.current;
    if (st.sound) play(kind);
    if (st.vibration) {
      if (kind === 'short') Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light).catch(() => {});
      else Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success).catch(() => {});
    }
  };

  const tick = useCallback(
    (dt: number) => {
      const s = ref.current;
      if (!s.running) return;
      if (stopwatch) {
        commit({ ...s, elapsedMs: s.elapsedMs + dt });
        return;
      }
      const before = s.remainingMs;
      let remaining = before - dt;
      const elapsed = s.elapsedMs + dt;
      if (remaining > 0) {
        if (settingsRef.current.countdownBeeps) {
          const a = Math.ceil(before / 1000);
          const b = Math.ceil(remaining / 1000);
          if (a !== b && b >= 1 && b <= 3) buzz('short');
        }
        commit({ ...s, remainingMs: remaining, elapsedMs: elapsed });
        return;
      }
      // fase(s) concluída(s) — também cobre o retorno de segundo plano
      let idx = s.phaseIndex;
      let changed = false;
      while (remaining <= 0 && idx + 1 < phases.length) {
        idx += 1;
        remaining += phases[idx].seconds * 1000;
        changed = true;
      }
      if (remaining <= 0) {
        commit({ ...s, remainingMs: 0, elapsedMs: elapsed, running: false, finished: true });
        buzz('end');
        Notifications.cancelAllScheduledNotificationsAsync().catch(() => {});
      } else {
        commit({ ...s, phaseIndex: idx, remainingMs: remaining, elapsedMs: elapsed });
        if (changed) buzz('long');
      }
    },
    [phases, stopwatch],
  );

  // laço de tempo baseado no relógio do sistema
  useEffect(() => {
    if (!state.running) return;
    let last = Date.now();
    const id = setInterval(() => {
      const now = Date.now();
      const dt = now - last;
      last = now;
      tick(dt);
    }, 100);
    return () => clearInterval(id);
  }, [state.running, tick]);

  // avisos agendados para as trocas de fase quando o app estiver em segundo plano
  const scheduleNotifications = async (s: RunState) => {
    await Notifications.cancelAllScheduledNotificationsAsync().catch(() => {});
    if (stopwatch) return;
    let acc = s.remainingMs / 1000;
    for (let i = s.phaseIndex + 1; i <= phases.length && i < s.phaseIndex + 60; i++) {
      const next = phases[i];
      const body = next ? `${PHASE_LABEL[next.kind]} · ${formatTime(next.seconds)}` : 'Treino concluído!';
      await Notifications.scheduleNotificationAsync({
        content: { title: timer.name, body, sound: true },
        trigger: { type: Notifications.SchedulableTriggerInputTypes.TIME_INTERVAL, seconds: Math.max(1, Math.round(acc)) },
      }).catch(() => {});
      if (next) acc += next.seconds;
    }
  };

  const playRun = () => {
    const s = ref.current;
    if (s.finished) return;
    commit({ ...s, running: true });
    scheduleNotifications(s);
  };

  const pause = () => {
    commit({ ...ref.current, running: false });
    Notifications.cancelAllScheduledNotificationsAsync().catch(() => {});
  };

  const toggle = () => (ref.current.running ? pause() : playRun());

  const reset = () => {
    commit(initial(phases));
    Notifications.cancelAllScheduledNotificationsAsync().catch(() => {});
  };

  return { state, phases, toggle, reset, stopwatch };
}
