import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { C, FONT } from './theme';
import { formatTime, TIMER_COLORS, TimerItem, TimerType, TYPE_LABEL } from './types';

const TYPES: TimerType[] = ['COUNTDOWN', 'STOPWATCH', 'ROUNDS', 'TABATA'];

function Step({ label, onPress }: { label: string; onPress: () => void }) {
  return (
    <Pressable style={s.step} onPress={onPress}>
      <Text style={s.stepText}>{label}</Text>
    </Pressable>
  );
}

function Duration({ label, value, min, onChange }: { label: string; value: number; min: number; onChange: (v: number) => void }) {
  return (
    <View>
      <Text style={s.label}>{label.toUpperCase()}</Text>
      <View style={s.fieldRow}>
        <Step label="-1m" onPress={() => onChange(Math.max(min, value - 60))} />
        <Step label="-5s" onPress={() => onChange(Math.max(min, value - 5))} />
        <Text style={s.value}>{formatTime(value)}</Text>
        <Step label="+5s" onPress={() => onChange(value + 5)} />
        <Step label="+1m" onPress={() => onChange(value + 60)} />
      </View>
    </View>
  );
}

function Count({ label, value, onChange }: { label: string; value: number; onChange: (v: number) => void }) {
  return (
    <View>
      <Text style={s.label}>{label.toUpperCase()}</Text>
      <View style={s.fieldRow}>
        <Step label="-1" onPress={() => onChange(Math.max(1, value - 1))} />
        <Text style={s.value}>{value}</Text>
        <Step label="+1" onPress={() => onChange(value + 1)} />
      </View>
    </View>
  );
}

export default function EditScreen({ initial, onSave, onCancel }: { initial: TimerItem | null; onSave: (t: TimerItem) => void; onCancel: () => void }) {
  const insets = useSafeAreaInsets();
  const base: TimerItem = initial ?? {
    id: 0, name: '', type: 'ROUNDS', colorIdx: 0, prep: 5, work: 30, rest: 10, rounds: 8, sets: 1, roundRest: 0,
  };
  const [t, setT] = useState<TimerItem>(base);
  const set = <K extends keyof TimerItem>(k: K, v: TimerItem[K]) => setT((p) => ({ ...p, [k]: v }));
  const isGroup = t.type === 'ROUNDS' || t.type === 'TABATA';

  return (
    <View style={[s.screen, { paddingTop: insets.top, paddingBottom: insets.bottom }]}>
      <View style={s.header}>
        <Pressable onPress={onCancel} hitSlop={12} style={{ padding: 8 }}>
          <Ionicons name="close" size={28} color="#fff" />
        </Pressable>
        <Text style={s.title}>{initial && initial.id !== 0 ? 'EDITAR TIMER' : 'NOVO TIMER'}</Text>
      </View>

      <ScrollView contentContainerStyle={{ padding: 16, gap: 18 }} keyboardShouldPersistTaps="handled">
        <TextInput
          style={s.input}
          placeholder="Nome"
          placeholderTextColor={C.gray}
          value={t.name}
          onChangeText={(v) => set('name', v)}
        />

        <View>
          <Text style={s.label}>TIPO</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8 }}>
            {TYPES.map((ty) => (
              <Pressable key={ty} onPress={() => set('type', ty)} style={[s.chip, t.type === ty && { backgroundColor: C.neon }]}>
                <Text style={[s.chipText, t.type === ty && { color: '#000' }]}>{TYPE_LABEL[ty]}</Text>
              </Pressable>
            ))}
          </ScrollView>
        </View>

        {t.type !== 'STOPWATCH' && (
          <>
            {t.type !== 'COUNTDOWN' && <Duration label="Preparação" value={t.prep} min={0} onChange={(v) => set('prep', v)} />}
            <Duration label={t.type === 'COUNTDOWN' ? 'Duração' : 'Trabalho'} value={t.work} min={1} onChange={(v) => set('work', v)} />
            {isGroup && (
              <>
                <Duration label="Descanso" value={t.rest} min={0} onChange={(v) => set('rest', v)} />
                <Count label="Trabalhos por round" value={t.rounds} onChange={(v) => set('rounds', v)} />
                <Count label="Rounds" value={t.sets} onChange={(v) => set('sets', v)} />
                {t.sets > 1 && (
                  <Duration label="Descanso entre rounds" value={t.roundRest} min={0} onChange={(v) => set('roundRest', v)} />
                )}
              </>
            )}
          </>
        )}

        <View>
          <Text style={s.label}>COR</Text>
          <View style={{ flexDirection: 'row', gap: 12 }}>
            {TIMER_COLORS.map((c, i) => (
              <Pressable
                key={c}
                onPress={() => set('colorIdx', i)}
                style={[s.dot, { backgroundColor: c }, t.colorIdx === i && { borderWidth: 3, borderColor: '#fff' }]}
              />
            ))}
          </View>
        </View>
      </ScrollView>

      <Pressable
        style={s.save}
        onPress={() => onSave({ ...t, name: t.name.trim() || TYPE_LABEL[t.type] })}
      >
        <Text style={s.saveText}>SALVAR</Text>
      </Pressable>
    </View>
  );
}

const s = StyleSheet.create({
  screen: { flex: 1, backgroundColor: C.bg },
  header: { flexDirection: 'row', alignItems: 'center', padding: 8, gap: 8 },
  title: { fontFamily: FONT, fontSize: 28, color: '#fff' },
  input: { borderWidth: 1, borderColor: '#777', borderRadius: 8, padding: 14, color: '#fff', fontSize: 18 },
  label: { fontFamily: FONT, fontSize: 18, color: C.gray, marginBottom: 6 },
  fieldRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  value: { flex: 1, textAlign: 'center', fontFamily: FONT, fontSize: 44, color: '#fff' },
  step: { backgroundColor: C.card, borderRadius: 8, paddingHorizontal: 11, paddingVertical: 12 },
  stepText: { fontFamily: FONT, fontSize: 20, color: C.neon },
  chip: { borderWidth: 1, borderColor: '#777', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 10 },
  chipText: { color: '#fff', fontSize: 15 },
  dot: { width: 36, height: 36, borderRadius: 18 },
  save: { backgroundColor: C.neon, margin: 16, height: 52, borderRadius: 26, alignItems: 'center', justifyContent: 'center' },
  saveText: { fontFamily: FONT, fontSize: 26, color: '#000' },
});
