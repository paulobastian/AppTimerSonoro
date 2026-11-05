export type TimerType = 'COUNTDOWN' | 'STOPWATCH' | 'ROUNDS' | 'TABATA';
export type PhaseKind = 'PREP' | 'WORK' | 'REST' | 'ROUND_REST';

export const TYPE_LABEL: Record<TimerType, string> = {
  COUNTDOWN: 'Contagem regressiva',
  STOPWATCH: 'Cronômetro',
  ROUNDS: 'Rounds',
  TABATA: 'Tabata',
};

export const PHASE_LABEL: Record<PhaseKind, string> = {
  PREP: 'PREPARAR',
  WORK: 'EXERCÍCIO',
  REST: 'DESCANSO',
  ROUND_REST: 'DESCANSO DO ROUND',
};

export interface Phase {
  kind: PhaseKind;
  seconds: number;
  round: number; // trabalho dentro do round (0 = preparação)
  set: number; // round atual
}

export interface TimerItem {
  id: number;
  name: string;
  type: TimerType;
  colorIdx: number;
  prep: number;
  work: number;
  rest: number;
  rounds: number; // trabalhos por round
  sets: number; // quantidade de rounds
  roundRest: number; // descanso ao fim de cada round
}

export interface Settings {
  sound: boolean;
  vibration: boolean;
  countdownBeeps: boolean;
}

export const DEFAULT_SETTINGS: Settings = { sound: true, vibration: true, countdownBeeps: true };

export const TIMER_COLORS = ['#4CE34C', '#FFE600', '#2196F3', '#FF5722', '#E040FB', '#00E5FF'];

export const grouped = (t: TimerItem) => t.type === 'ROUNDS' || t.type === 'TABATA';

export function buildPhases(t: TimerItem): Phase[] {
  if (t.type === 'STOPWATCH') return [];
  const list: Phase[] = [];
  if (t.prep > 0 && t.type !== 'COUNTDOWN') list.push({ kind: 'PREP', seconds: t.prep, round: 0, set: 1 });
  const n = grouped(t) ? t.rounds : 1;
  const total = grouped(t) ? t.sets : 1;
  for (let set = 1; set <= total; set++) {
    for (let r = 1; r <= n; r++) {
      list.push({ kind: 'WORK', seconds: t.work, round: r, set });
      if (t.rest > 0 && r < n) list.push({ kind: 'REST', seconds: t.rest, round: r, set });
    }
    if (t.roundRest > 0 && set < total) list.push({ kind: 'ROUND_REST', seconds: t.roundRest, round: n, set });
  }
  return list;
}

export const totalSeconds = (t: TimerItem) => buildPhases(t).reduce((a, p) => a + p.seconds, 0);

export function formatTime(totalSec: number): string {
  const s = Math.max(0, Math.floor(totalSec));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  const pad = (x: number) => String(x).padStart(2, '0');
  return h > 0 ? `${h}:${pad(m)}:${pad(sec)}` : `${pad(m)}:${pad(sec)}`;
}

export function summary(t: TimerItem): string {
  if (t.type === 'STOPWATCH') return 'Cronômetro';
  if (t.type === 'COUNTDOWN') return formatTime(t.work);
  return (
    (t.sets > 1 ? `${t.sets} rounds · ` : '') +
    `${t.rounds}x ${formatTime(t.work)}` +
    (t.rest > 0 ? ` / ${formatTime(t.rest)}` : '') +
    (t.sets > 1 && t.roundRest > 0 ? ` · desc. round ${formatTime(t.roundRest)}` : '')
  );
}

const base = { prep: 0, work: 30, rest: 0, rounds: 1, sets: 1, roundRest: 0, id: 0 };

export const TEMPLATES: TimerItem[] = [
  { ...base, name: 'Tabata', type: 'TABATA', colorIdx: 0, prep: 10, work: 20, rest: 10, rounds: 8 },
  { ...base, name: 'EMOM 10 min', type: 'ROUNDS', colorIdx: 2, prep: 5, work: 60, rounds: 10 },
  { ...base, name: 'HIIT 40/20', type: 'ROUNDS', colorIdx: 3, prep: 10, work: 40, rest: 20, rounds: 10 },
  { ...base, name: 'Boxe 3 min', type: 'ROUNDS', colorIdx: 1, prep: 10, work: 180, rest: 60, rounds: 12 },
  { ...base, name: 'Descanso 1 min', type: 'COUNTDOWN', colorIdx: 5, work: 60 },
  { ...base, name: 'Cronômetro', type: 'STOPWATCH', colorIdx: 1 },
];
