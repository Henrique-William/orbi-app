import { Colors } from '@/constants/theme'; // Importe daqui
import { useColorScheme } from '@/hooks/use-color-scheme';
import { Ionicons } from '@expo/vector-icons';
import { Tabs } from 'expo-router';
import { StyleSheet, TouchableOpacity, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

const CustomTabBar = ({ state, descriptors, navigation }: any) => {
  const insets = useSafeAreaInsets();
  const theme = useColorScheme() ?? 'light';
  
  const activeColor = Colors[theme].tabIconSelected;
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
          else if (route.name === 'routes') iconName = isFocused ? 'analytics' : 'analytics-outline';
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

                color={isFocused ? Colors.dark.tint : Colors.dark.tabIconDefault} 
              />
            </TouchableOpacity>
          );
        })}
      </View>
    </View>
  );
};

export default function TabLayout() {
  return (
    <Tabs
      tabBar={(props) => <CustomTabBar {...props} />}
      screenOptions={{
        headerShown: false,
        tabBarHideOnKeyboard: true,
      }}
    >
      <Tabs.Screen name="index" options={{ title: 'Home' }} />
      <Tabs.Screen name="routes" options={{ title: 'Routes' }} />
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
    backgroundColor: '#18181b',
    height: 60,
    borderRadius: 30,
    alignItems: 'center',
    width: '70%',
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