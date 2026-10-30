import { BebasNeue_400Regular, useFonts } from '@expo-google-fonts/bebas-neue';
import * as Notifications from 'expo-notifications';
import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';
import { BackHandler, Platform, StyleSheet, Text, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import EditScreen from './src/EditScreen';
import HomeScreen from './src/HomeScreen';
import RunScreen from './src/RunScreen';
import { loadSettings, loadTimers, saveSettings, saveTimers } from './src/store';
import { C, FONT } from './src/theme';
import { DEFAULT_SETTINGS, Settings, TimerItem } from './src/types';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

type Screen =
  | { name: 'splash' }
  | { name: 'home' }
  | { name: 'edit'; timer: TimerItem | null }
  | { name: 'run'; timer: TimerItem };

function Splash() {
  return (
    <View style={s.splash}>
      <View style={s.glow}>
        <View style={s.clapper}>
          <Text style={s.clapperTop}>TIMER</Text>
          <Text style={s.clapperTime}>00:30</Text>
        </View>
      </View>
    </View>
  );
}

export default function App() {
  const [fontsLoaded] = useFonts({ BebasNeue_400Regular });
  const [screen, setScreen] = useState<Screen>({ name: 'splash' });
  const [timers, setTimers] = useState<TimerItem[]>([]);
  const [settings, setSettings] = useState<Settings>(DEFAULT_SETTINGS);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    (async () => {
      setTimers(await loadTimers());
      setSettings(await loadSettings());
      setReady(true);
      if (Platform.OS === 'android') {
        await Notifications.setNotificationChannelAsync('default', {
          name: 'Timer',
          importance: Notifications.AndroidImportance.HIGH,
        }).catch(() => {});
      }
      const perm = await Notifications.getPermissionsAsync().catch(() => null);
      if (perm && !perm.granted) await Notifications.requestPermissionsAsync().catch(() => {});
    })();
  }, []);

  useEffect(() => {
    if (fontsLoaded && ready && screen.name === 'splash') {
      const id = setTimeout(() => setScreen({ name: 'home' }), 1500);
      return () => clearTimeout(id);
    }
  }, [fontsLoaded, ready, screen.name]);

  // botão voltar do Android
  useEffect(() => {
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      if (screen.name === 'edit' || screen.name === 'run') {
        setScreen({ name: 'home' });
        return true;
      }
      return false;
    });
    return () => sub.remove();
  }, [screen.name]);

  const persist = (list: TimerItem[]) => {
    setTimers(list);
    saveTimers(list);
  };

  if (!fontsLoaded || screen.name === 'splash') {
    return (
      <SafeAreaProvider>
        <StatusBar style="light" />
        <Splash />
      </SafeAreaProvider>
    );
  }

  return (
    <SafeAreaProvider>
      <StatusBar style="light" />
      {screen.name === 'home' && (
        <HomeScreen
          timers={timers}
          settings={settings}
          onRun={(timer) => setScreen({ name: 'run', timer })}
          onNew={(t) => setScreen({ name: 'edit', timer: t ? { ...t, id: 0 } : null })}
          onEdit={(timer) => setScreen({ name: 'edit', timer })}
          onDuplicate={(t) => persist([...timers, { ...t, id: Date.now(), name: `${t.name} (cópia)` }])}
          onDelete={(t) => persist(timers.filter((x) => x.id !== t.id))}
          onSettings={(st) => {
            setSettings(st);
            saveSettings(st);
          }}
        />
      )}
      {screen.name === 'edit' && (
        <EditScreen
          initial={screen.timer}
          onCancel={() => setScreen({ name: 'home' })}
          onSave={(t) => {
            persist(t.id === 0 ? [...timers, { ...t, id: Date.now() }] : timers.map((x) => (x.id === t.id ? t : x)));
            setScreen({ name: 'home' });
          }}
        />
      )}
      {screen.name === 'run' && (
        <RunScreen timer={screen.timer} settings={settings} onClose={() => setScreen({ name: 'home' })} />
      )}
    </SafeAreaProvider>
  );
}

const s = StyleSheet.create({
  splash: { flex: 1, backgroundColor: C.bg, alignItems: 'center', justifyContent: 'center' },
  glow: { width: 230, height: 230, borderRadius: 115, backgroundColor: 'rgba(139,245,0,0.25)', alignItems: 'center', justifyContent: 'center' },
  clapper: { width: 150, borderRadius: 14, overflow: 'hidden', backgroundColor: C.neon, alignItems: 'center' },
  clapperTop: { width: '100%', textAlign: 'center', backgroundColor: '#2B2B30', color: C.yellow, fontFamily: FONT, fontSize: 24, paddingVertical: 4 },
  clapperTime: { fontFamily: FONT, fontSize: 48, color: C.bg, paddingVertical: 10 },
});
