import { Ionicons } from '@expo/vector-icons';
import { useKeepAwake } from 'expo-keep-awake';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import Svg, { Circle } from 'react-native-svg';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { C, FONT } from './theme';
import { formatTime, grouped, PHASE_LABEL, PhaseKind, Settings, TIMER_COLORS, TimerItem, totalSeconds } from './types';
import { useRunner } from './useRunner';

const phaseColor = (kind: PhaseKind | undefined, work: string) =>
  kind === 'PREP' ? C.prep : kind === 'REST' ? C.rest : kind === 'ROUND_REST' ? C.roundRest : work;

function Ring({ progress, size = 84, stroke = 7 }: { progress: number; size?: number; stroke?: number }) {
  const r = (size - stroke) / 2;
  const circ = 2 * Math.PI * r;
  return (
    <Svg width={size} height={size} style={StyleSheet.absoluteFill}>
      <Circle cx={size / 2} cy={size / 2} r={r} stroke="#2A2A31" strokeWidth={stroke} fill="none" />
      <Circle
        cx={size / 2} cy={size / 2} r={r} stroke={C.yellow} strokeWidth={stroke} fill="none"
        strokeDasharray={`${circ}`} strokeDashoffset={circ * (1 - Math.min(1, Math.max(0, progress)))}
        strokeLinecap="round" rotation={-90} origin={`${size / 2}, ${size / 2}`}
      />
    </Svg>
  );
}

function Stat({ value, label, color }: { value: number; label: string; color: string }) {
  return (
    <View style={{ alignItems: 'center' }}>
      <Text style={[s.statValue, { color }]}>{value}</Text>
      <Text style={s.statLabel}>{label}</Text>
    </View>
  );
}

export default function RunScreen({ timer, settings, onClose }: { timer: TimerItem; settings: Settings; onClose: () => void }) {
  useKeepAwake();
  const insets = useSafeAreaInsets();
  const { state, phases, toggle, reset, stopwatch } = useRunner(timer, settings);
  const workColor = TIMER_COLORS[timer.colorIdx % TIMER_COLORS.length];
  const phase = phases[state.phaseIndex];
  const next = phases[state.phaseIndex + 1];

  const mainColor = state.finished ? workColor : phaseColor(phase?.kind, workColor);
  const seconds = stopwatch ? Math.floor(state.elapsedMs / 1000) : Math.ceil(state.remainingMs / 1000);
  const mainLabel = state.finished ? 'CONCLUÍDO' : stopwatch ? 'CRONÔMETRO' : phase ? PHASE_LABEL[phase.kind] : '';

  const totalRemaining = stopwatch
    ? seconds
    : state.finished
      ? 0
      : seconds + phases.slice(state.phaseIndex + 1).reduce((a, p) => a + p.seconds, 0);
  const total = Math.max(1, totalSeconds(timer));
  const overall = stopwatch ? 0 : 1 - totalRemaining / total;

  const isGroup = grouped(timer);
  const totalSets = isGroup ? timer.sets : 1;
  const currentWork = phase && phase.round > 0 ? phase.round : 1;
  const currentSet = phase?.set ?? 1;
  const showWorks = isGroup && timer.rounds > 1;
  const showSets = isGroup && totalSets > 1;
  const worksLeft = state.finished ? 0 : phase?.kind === 'REST' ? timer.rounds - currentWork : phase?.kind === 'WORK' ? timer.rounds - currentWork + 1 : timer.rounds;
  const setsLeft = state.finished ? 0 : phase?.kind === 'ROUND_REST' ? totalSets - currentSet : phase?.kind === 'PREP' ? totalSets : totalSets - currentSet + 1;

  const header = [
    showSets ? `ROUND ${currentSet} / ${totalSets}` : null,
    showWorks && phase?.kind !== 'ROUND_REST' ? `TRABALHO ${currentWork} / ${timer.rounds}` : null,
  ].filter(Boolean).join('  ·  ');

  const started = state.elapsedMs > 0;

  return (
    <View style={[s.screen, { backgroundColor: C.black }]}>
      <View style={[s.top, { paddingTop: insets.top + 2 }]}>
        <Pressable onPress={onClose} hitSlop={12} style={s.topBtn}>
          <Ionicons name="close" size={28} color={C.yellow} />
        </Pressable>
        <View style={{ alignItems: 'center', flex: 1 }}>
          <Text style={s.topName} numberOfLines={1}>{timer.name.toUpperCase()}</Text>
          <Text style={s.topTime}>{formatTime(totalRemaining)}</Text>
        </View>
        <Pressable onPress={reset} hitSlop={12} style={s.topBtn}>
          <Ionicons name="refresh" size={26} color={C.yellow} />
        </Pressable>
      </View>

      <View style={[s.main, { backgroundColor: mainColor }]}>
        {!state.finished && phase && phase.kind !== 'PREP' && header !== '' && <Text style={s.header}>{header}</Text>}
        <Text style={[s.mainLabel, mainLabel.length > 12 && { fontSize: 52 }]} numberOfLines={1} adjustsFontSizeToFit>
          {mainLabel}
        </Text>
        <Text style={s.mainTime} numberOfLines={1} adjustsFontSizeToFit>{formatTime(seconds)}</Text>
      </View>

      {next && !state.finished && (
        <View style={[s.next, { backgroundColor: phaseColor(next.kind, workColor) }]}>
          <Text style={s.nextLabel}>{PHASE_LABEL[next.kind]}:</Text>
          <Text style={s.nextTime}>{formatTime(next.seconds)}</Text>
        </View>
      )}

      <View style={[s.bottom, { paddingBottom: insets.bottom + 10 }]}>
        <View style={{ flex: 1, alignItems: 'center' }}>
          {showWorks && <Stat value={worksLeft} label="TRABALHOS PARA TERMINAR" color={C.blue} />}
        </View>
        <View style={{ alignItems: 'center' }}>
          <Pressable onPress={toggle} disabled={state.finished} style={s.playWrap}>
            <Ring progress={overall} />
            <Ionicons name={state.running ? 'pause' : 'play'} size={40} color={C.yellow} />
          </Pressable>
          <Text style={s.playLabel}>{state.running ? 'PAUSAR' : started ? 'RETOMAR' : 'INICIAR'}</Text>
        </View>
        <View style={{ flex: 1, alignItems: 'center' }}>
          {showSets ? (
            <Stat value={setsLeft} label="ROUNDS PARA TERMINAR" color={C.yellow} />
          ) : showWorks ? (
            <Stat value={currentWork} label="TRABALHO ATUAL" color={C.yellow} />
          ) : null}
        </View>
      </View>
    </View>
  );
}

const s = StyleSheet.create({
  screen: { flex: 1 },
  top: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 8, paddingBottom: 6 },
  topBtn: { padding: 10 },
  topName: { fontFamily: FONT, fontSize: 16, color: C.yellow },
  topTime: { fontFamily: FONT, fontSize: 30, color: '#fff' },
  main: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 12 },
  header: { fontFamily: FONT, fontSize: 22, color: '#000' },
  mainLabel: { fontFamily: FONT, fontSize: 72, color: '#000' },
  mainTime: { fontFamily: FONT, fontSize: 190, color: '#000', lineHeight: 200, width: '100%', textAlign: 'center' },
  next: { alignItems: 'center', paddingVertical: 10 },
  nextLabel: { fontFamily: FONT, fontSize: 26, color: '#000' },
  nextTime: { fontFamily: FONT, fontSize: 56, color: '#000', lineHeight: 60 },
  bottom: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, paddingTop: 10 },
  playWrap: { width: 84, height: 84, alignItems: 'center', justifyContent: 'center' },
  playLabel: { fontFamily: FONT, fontSize: 16, color: C.yellow, marginTop: 2 },
  statValue: { fontFamily: FONT, fontSize: 60 },
  statLabel: { fontFamily: FONT, fontSize: 12, color: '#fff', textAlign: 'center' },
});
