import { Colors } from '@/constants/theme';
import { RouteData } from '@/constants/types/interfaces';
import React from 'react';
import { StyleSheet, TouchableOpacity, useColorScheme, View } from 'react-native';
import { ThemedText } from './themed-text';

interface RouteThumbnailProps {
  route: RouteData;
}

export default function RouteThumbnailHome({ route }: RouteThumbnailProps) {
  const theme = useColorScheme() ?? 'light';

  const colors = {
    background: Colors[theme].background,
    backgroundCard: Colors[theme].backgroundCard,
    iconPrimary: Colors[theme].primary,
    inactive: Colors[theme].gray,
    textSecondary: Colors[theme].icon,
  };

  return (
    <TouchableOpacity style={[styles.scheduleContainer]}>

      {/* Card Header */}

      <View style={[styles.cardInfo, { backgroundColor: colors.backgroundCard }]}>
        <ThemedText type="defaultSemiBold">Rota #{route.id}</ThemedText>
        <ThemedText type="small">{route.driverName}</ThemedText>
      </View>

    </TouchableOpacity>
  )
}

const styles = StyleSheet.create({
  scheduleContainer: {
    width: 180,
    aspectRatio: 9 / 10,
    height: 'auto',
    borderRadius: 40,
    marginRight: 8,
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'flex-end',
    gap: 12,
    backgroundColor: '#9090ff',
  },
  cardInfo: {
    width: '100%',
    height: '50%',
    display: 'flex',
    flexDirection: 'column',
    backgroundColor: '#fff',
    borderRadius: 40,
    paddingHorizontal: 24,
    paddingVertical: 16,
  }
});
