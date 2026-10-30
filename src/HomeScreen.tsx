import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { Alert, FlatList, Pressable, StyleSheet, Switch, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { C, FONT } from './theme';
import { Settings, summary, TEMPLATES, TIMER_COLORS, TimerItem, totalSeconds, formatTime, TYPE_LABEL } from './types';

interface Props {
  timers: TimerItem[];
  settings: Settings;
  onRun: (t: TimerItem) => void;
  onNew: (t: TimerItem | null) => void;
  onEdit: (t: TimerItem) => void;
  onDuplicate: (t: TimerItem) => void;
  onDelete: (t: TimerItem) => void;
  onSettings: (s: Settings) => void;
}

function Row({ t, sub, onPress, right }: { t: TimerItem; sub: string; onPress: () => void; right: React.ReactNode }) {
  const color = TIMER_COLORS[t.colorIdx % TIMER_COLORS.length];
  return (
    <Pressable style={s.row} onPress={onPress}>
      <View style={[s.bar, { backgroundColor: color }]} />
      {t.type === 'STOPWATCH' ? (
        <Ionicons name="stopwatch-outline" size={22} color={color} style={{ marginHorizontal: 10 }} />
      ) : (
        <Text style={[s.rowTime, { color }]}>{formatTime(totalSeconds(t))}</Text>
      )}
      <View style={{ flex: 1 }}>
        <Text style={s.rowName} numberOfLines={1}>{t.name.toUpperCase()}</Text>
        <Text style={s.rowSub} numberOfLines={1}>{sub}</Text>
      </View>
      {right}
    </Pressable>
  );
}

export default function HomeScreen(p: Props) {
  const insets = useSafeAreaInsets();
  const [tab, setTab] = useState(0);
  const [searching, setSearching] = useState(false);
  const [query, setQuery] = useState('');
  const titles = ['OS MEUS TIMERS', 'MODELOS', 'DEFINIÇÕES'];
  const shown = p.timers.filter((t) => t.name.toLowerCase().includes(query.toLowerCase()));

  const menu = (t: TimerItem) =>
    Alert.alert(t.name, undefined, [
      { text: 'Editar', onPress: () => p.onEdit(t) },
      { text: 'Duplicar', onPress: () => p.onDuplicate(t) },
      { text: 'Excluir', style: 'destructive', onPress: () => p.onDelete(t) },
      { text: 'Cancelar', style: 'cancel' },
    ]);

  return (
    <View style={[s.screen, { paddingTop: insets.top }]}>
      <View style={s.header}>
        <Text style={s.title}>{titles[tab]}</Text>
        {tab === 0 && (
          <Pressable
            style={s.searchBtn}
            onPress={() => {
              setSearching(!searching);
              if (searching) setQuery('');
            }}
          >
            <Ionicons name="search" size={22} color="#fff" />
          </Pressable>
        )}
      </View>
      {searching && tab === 0 && (
        <TextInput
          style={s.input}
          placeholder="Pesquisar"
          placeholderTextColor={C.gray}
          value={query}
          onChangeText={setQuery}
        />
      )}

      {tab === 0 && (
        <FlatList
          data={shown}
          keyExtractor={(t) => String(t.id)}
          contentContainerStyle={{ padding: 16, paddingBottom: 110, gap: 10, flexGrow: 1 }}
          ListEmptyComponent={
            <Text style={s.empty}>
              {p.timers.length === 0 ? 'Nenhum timer ainda.\nToque em + para criar um\nou escolha um modelo.' : 'Nada encontrado.'}
            </Text>
          }
          renderItem={({ item }) => (
            <Row
              t={item}
              sub={`${TYPE_LABEL[item.type]} · ${summary(item)}`}
              onPress={() => p.onRun(item)}
              right={
                <Pressable hitSlop={10} onPress={() => menu(item)} style={{ padding: 12 }}>
                  <Ionicons name="ellipsis-horizontal" size={22} color="#fff" />
                </Pressable>
              }
            />
          )}
        />
      )}

      {tab === 1 && (
        <FlatList
          data={TEMPLATES}
          keyExtractor={(t) => t.name}
          contentContainerStyle={{ padding: 16, gap: 10 }}
          renderItem={({ item }) => (
            <Row
              t={item}
              sub={summary(item)}
              onPress={() => p.onNew(item)}
              right={<Ionicons name="add" size={26} color={C.neon} style={{ padding: 12 }} />}
            />
          )}
        />
      )}

      {tab === 2 && (
        <View style={{ padding: 16 }}>
          {(
            [
              ['sound', 'Som'],
              ['vibration', 'Vibração'],
              ['countdownBeeps', 'Bipes nos últimos 3 segundos'],
            ] as const
          ).map(([key, label]) => (
            <View key={key} style={s.setting}>
              <Text style={s.settingLabel}>{label}</Text>
              <Switch
                value={p.settings[key]}
                onValueChange={(v) => p.onSettings({ ...p.settings, [key]: v })}
                trackColor={{ true: C.neon, false: '#555' }}
                thumbColor="#fff"
              />
            </View>
          ))}
        </View>
      )}

      {tab === 0 && (
        <Pressable style={[s.fab, { bottom: 90 + insets.bottom }]} onPress={() => p.onNew(null)}>
          <Ionicons name="add" size={32} color="#000" />
        </Pressable>
      )}

      <View style={[s.tabs, { paddingBottom: insets.bottom + 6 }]}>
        {(
          [
            ['Os meus timers', 'heart'],
            ['Modelos', 'walk'],
            ['Definições', 'settings'],
          ] as const
        ).map(([label, icon], i) => (
          <Pressable key={label} style={s.tab} onPress={() => setTab(i)}>
            <View style={[s.tabIcon, tab === i && { backgroundColor: C.neon }]}>
              <Ionicons name={icon} size={22} color={tab === i ? '#000' : '#fff'} />
            </View>
            <Text style={[s.tabLabel, tab === i && { color: C.neon }]}>{label}</Text>
          </Pressable>
        ))}
      </View>
    </View>
  );
}

const s = StyleSheet.create({
  screen: { flex: 1, backgroundColor: C.bg },
  header: { alignItems: 'center', justifyContent: 'center', paddingVertical: 14, paddingHorizontal: 16 },
  title: { fontFamily: FONT, fontSize: 28, color: '#fff', letterSpacing: 0.5 },
  searchBtn: {
    position: 'absolute', right: 16, width: 40, height: 40, borderRadius: 20,
    backgroundColor: '#55556A', alignItems: 'center', justifyContent: 'center',
  },
  input: {
    marginHorizontal: 16, marginBottom: 4, padding: 12, borderRadius: 10,
    backgroundColor: C.card, color: '#fff', fontSize: 16,
  },
  empty: { color: C.gray, fontFamily: FONT, fontSize: 22, textAlign: 'center', marginTop: 140, lineHeight: 28 },
  row: {
    flexDirection: 'row', alignItems: 'center', backgroundColor: C.card, borderRadius: 12,
    overflow: 'hidden', minHeight: 64,
  },
  bar: { width: 5, alignSelf: 'stretch' },
  rowTime: { fontFamily: FONT, fontSize: 17, marginHorizontal: 10 },
  rowName: { fontFamily: FONT, fontSize: 24, color: '#fff' },
  rowSub: { color: C.gray, fontSize: 12 },
  setting: { flexDirection: 'row', alignItems: 'center', paddingVertical: 10 },
  settingLabel: { flex: 1, color: '#fff', fontFamily: FONT, fontSize: 22 },
  fab: {
    position: 'absolute', right: 20, width: 60, height: 60, borderRadius: 30,
    backgroundColor: C.neon, alignItems: 'center', justifyContent: 'center', elevation: 6,
  },
  tabs: { flexDirection: 'row', backgroundColor: C.card, paddingTop: 8 },
  tab: { flex: 1, alignItems: 'center', gap: 2 },
  tabIcon: { width: 64, height: 32, borderRadius: 16, alignItems: 'center', justifyContent: 'center' },
  tabLabel: { color: '#fff', fontSize: 11 },
});
