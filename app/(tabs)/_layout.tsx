import { Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';
import { Ionicons } from '@expo/vector-icons';
import { Tabs } from 'expo-router';
import { StyleSheet, TouchableOpacity, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

const CustomTabBar = ({ state, descriptors, navigation }: any) => {
  const insets = useSafeAreaInsets();
  const theme = useColorScheme() ?? 'light';
  
  const activeColor = Colors[theme].primary;
  const inactiveColor = Colors[theme].tabIconDefault;
  const backgroundColor = Colors[theme].background;

  return (
    <View style={[styles.container, { paddingBottom: insets.bottom + 10 }]}>
      <View style={[styles.pillContainer, { backgroundColor: '#18181b' }]}> 
        
        {state.routes.slice(0, 4).map((route: any, index: number) => {
          const { options } = descriptors[route.key];
          const isFocused = state.index === index;

          const onPress = () => {
            const event = navigation.emit({
              type: 'tabPress',
              target: route.key,
              canPreventDefault: true,
            });

            if (!isFocused && !event.defaultPrevented) {
              navigation.navigate(route.name);
            }
          };

          let iconName: any = 'home-outline';
          if (route.name === 'index') iconName = isFocused ? 'home' : 'home-outline';
          else if (route.name === 'rides') iconName = isFocused ? 'analytics' : 'analytics-outline';
          else if (route.name === 'statistics') iconName = isFocused ? 'stats-chart' : 'stats-chart-outline';
          else if (route.name === 'notifications') iconName = isFocused ? 'notifications' : 'notifications-outline';

          return (
            <TouchableOpacity
              key={index}
              onPress={onPress}
              style={styles.tabItem}
              activeOpacity={0.7}
            >
              <Ionicons
                name={iconName}
                size={24}

                color={isFocused ? Colors[theme].primary : Colors[theme].tabIconDefault} 
              />
            </TouchableOpacity>
          );
        })}
      </View>
    </View>
  );
};

import { API_URLS } from '@/constants/api';
import { useRouter } from 'expo-router';
import { useEffect, useState } from 'react'; // will be fixed in text
import { ActivityIndicator } from 'react-native';

export default function TabLayout() {
  const router = useRouter();
  const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);

  useEffect(() => {
    fetch(API_URLS.VALIDATE, {
      method: 'GET',
      credentials: 'include',
    })
      .then((res) => {
        if (res.ok) setIsAuthenticated(true);
        else throw new Error('Não autenticado');
      })
      .catch(() => {
        setIsAuthenticated(false);
        router.replace('/login');
      });
  }, []);

  if (isAuthenticated === null) {
    return (
      <View style={[ styles.container, { flex: 1, justifyContent: 'center', backgroundColor: '#f8f8f8' } ]}>
        <ActivityIndicator size="large" color="#450693" />
      </View>
    );
  }

  if (!isAuthenticated) return null;

  return (
    <Tabs
      tabBar={(props) => <CustomTabBar {...props} />}
      screenOptions={{
        headerShown: false,
        tabBarHideOnKeyboard: true,
      }}
    >
      <Tabs.Screen name="index" options={{ title: 'Home' }} />
      <Tabs.Screen name="rides" options={{ title: 'Rides' }} />
      <Tabs.Screen name="statistics" options={{ title: 'Statistics' }} />
      <Tabs.Screen name="notifications" options={{ title: 'Notifications' }} />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    alignItems: 'center',
    backgroundColor: 'transparent',
    width: '100%'
  },
  pillContainer: {
    flexDirection: 'row',
    height: 60,
    borderRadius: 30,
    alignItems: 'center',
    width: '80%',
    justifyContent: 'space-between',
    paddingHorizontal: '5%'
  },
  tabItem: {
    minWidth: 50,
    minHeight: 60,
    alignItems: 'center',
    justifyContent: 'center',
  },
});