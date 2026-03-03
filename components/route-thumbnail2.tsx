import { Colors } from '@/constants/theme';
import { RouteData } from '@/constants/types/interfaces';
import { format } from 'date-fns';
import { Link } from 'expo-router';
import React from 'react';
import { StyleSheet, TouchableOpacity, useColorScheme, View } from 'react-native';
import { ThemedText } from './themed-text';

interface RouteThumbnailProps {
  route: RouteData;
}

export default function RouteThumbnail2({ route }: RouteThumbnailProps) {
  const theme = useColorScheme() ?? 'light';

  const colors = {
    background: Colors[theme].background,
    backgroundCard: Colors[theme].backgroundCard,
    iconPrimary: Colors[theme].primary,
    inactive: Colors[theme].gray,
    textSecondary: Colors[theme].icon,
  };

  return (
    <View style={[styles.scheduleContainer, { backgroundColor: colors.backgroundCard }]}>

      {/* Card Header */}
      <View style={styles.cardHeader}>
        <View style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', gap: 8 }}>
          <View style={[styles.cardIcon, { backgroundColor: Colors[theme].primary }]}></View>
          <View style={styles.cardHeaderText}>
            <ThemedText type='small' style={{ color: colors.inactive, marginBottom: 4 }}>One Way</ThemedText>
            <ThemedText type='defaultSemiBold' style={{ marginBottom: 8 }}>ROUTE # {route.id}</ThemedText>
          </View>
        </View>
        <View style={styles.scheduleDate}>
          <ThemedText type='small' style={{ color: Colors[theme].green }}>{format(new Date(route.createdAt || new Date()), 'd MMM yyyy')}</ThemedText>
        </View>
      </View>

      {/* Card Content */}
      <View style={styles.cardContent}>
        <View style={styles.cardLocation}>
          <ThemedText type='defaultSemiBold'>CGK</ThemedText>
          <ThemedText type='small' style={{ color: Colors[theme].gray }}>06:40</ThemedText>
        </View>

        <View style={{ flex: 1, display: 'flex', flexDirection: 'row', justifyContent: 'center', alignItems: 'center' }}></View>

        <View style={styles.cardLocation}>
          <ThemedText type='defaultSemiBold'>DPS</ThemedText>
          <ThemedText type='small' style={{ color: Colors[theme].gray }}>09:25</ThemedText>
        </View>

      </View>

      {/* Card Footer */}
      <View style={styles.cardFooter}>
        <TouchableOpacity style={[styles.cardButton, { backgroundColor: Colors[theme].backgroundDark }]} activeOpacity={0.9}>
          <Link href={`../ride/${route.id}`} asChild>
            <ThemedText style={{ color: Colors[theme === 'light' ? 'dark' : 'light'].text }}>See Details</ThemedText>
          </Link>
        </TouchableOpacity>
        <View style={styles.cardFooterInfo}>
          <ThemedText type='thin' style={{ color: colors.inactive }}>Baggage</ThemedText>
          <ThemedText type='defaultSemiBold' style={{ color: colors.textSecondary }}>5 kg</ThemedText>
        </View>
      </View>

    </View>
  )
}

const styles = StyleSheet.create({
  scheduleContainer: {
    flex: 1,
    aspectRatio: 16 / 14,
    width: '100%',
    height: 'auto',
    borderRadius: 24,
    padding: 16,
    marginRight: 16,
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'space-between',
    gap: 12,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  cardIcon: {
    aspectRatio: 1 / 1,
    height: 50,
    borderRadius: 999,
  },
  cardHeaderText: {
    display: 'flex',
    flexDirection: 'column',
  },
  scheduleDate: {
    padding: 8,
    borderRadius: 999,
    backgroundColor: '#38cb8925',
  },
  cardContent: {
    backgroundColor: '#f2f2ff',
    height: "33%",
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    padding: 0,
    width: '100%',
    borderRadius: 16,
  },
  cardLocation: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
    alignItems: 'center',
    gap: 4,
  },

  cardFooter: {
    height: "25%",
    display: 'flex',
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  cardButton: {
    borderRadius: 999,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 12,
  },
  cardFooterInfo: {
    display: 'flex',
    flexDirection: 'column',
    gap: 4,
  }
});
