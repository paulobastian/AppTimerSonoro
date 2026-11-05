import AsyncStorage from '@react-native-async-storage/async-storage';
import { DEFAULT_SETTINGS, Settings, TimerItem } from './types';

const TIMERS_KEY = 'timers';
const SETTINGS_KEY = 'settings';

export async function loadTimers(): Promise<TimerItem[]> {
  try {
    const raw = await AsyncStorage.getItem(TIMERS_KEY);
    return raw ? (JSON.parse(raw) as TimerItem[]) : [];
  } catch {
    return [];
  }
}

export async function saveTimers(list: TimerItem[]) {
  await AsyncStorage.setItem(TIMERS_KEY, JSON.stringify(list));
}

export async function loadSettings(): Promise<Settings> {
  try {
    const raw = await AsyncStorage.getItem(SETTINGS_KEY);
    return raw ? { ...DEFAULT_SETTINGS, ...JSON.parse(raw) } : DEFAULT_SETTINGS;
  } catch {
    return DEFAULT_SETTINGS;
  }
}

export async function saveSettings(s: Settings) {
  await AsyncStorage.setItem(SETTINGS_KEY, JSON.stringify(s));
}
