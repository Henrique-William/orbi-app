import { Ionicons } from '@expo/vector-icons';
import { Tabs } from 'expo-router';
import React from 'react';
import { StyleSheet, TouchableOpacity, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

const COLORS = {
  background: '#18181b',
  primary: '#6366f1',
  inactive: '#d0d0d6ff',  
  white: '#FFFFFF'
};

const CustomTabBar = ({ state, descriptors, navigation }: any) => {
  const insets = useSafeAreaInsets();

  return (
    <View style={[styles.container, { paddingBottom: insets.bottom + 10 }]}>

      {/* Container Principal que segura a barra e o botão */}
      <View style={styles.pillContainer}>
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

          let iconName: keyof typeof Ionicons.glyphMap = 'home-outline';
          if (route.name === 'index') iconName = isFocused ? 'home' : 'home-outline';
          else if (route.name === 'explore') iconName = isFocused ? 'people' : 'people-outline'; // Friends
          else if (route.name === 'calendar') iconName = isFocused ? 'calendar' : 'calendar-outline';
          else if (route.name === 'chat') iconName = isFocused ? 'chatbubble-ellipses' : 'chatbubble-ellipses-outline';

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
                color={isFocused ? COLORS.primary : COLORS.inactive}
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
      <Tabs.Screen name="explore" options={{ title: 'Friends' }} />
      <Tabs.Screen name="calendar" options={{ title: 'Calendar' }} />
      <Tabs.Screen name="chat" options={{ title: 'Chat' }} />
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
    backgroundColor: COLORS.background,
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