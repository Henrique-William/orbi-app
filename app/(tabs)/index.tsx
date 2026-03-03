import { Link } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import {
  Image,
  ScrollView,
  StyleSheet,
  useColorScheme,
  View
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import RouteThumbnail2 from '@/components/route-thumbnail2';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { fetchRoutes } from '@/constants/api';
import { Colors } from '@/constants/theme';
import { RouteData } from '@/constants/types/interfaces';
import { Ionicons } from '@expo/vector-icons';

export default function HomeScreen() {
  const theme = useColorScheme() ?? 'light';
  const [routes, setRoutes] = useState<RouteData[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      setError(null);
      const data = await fetchRoutes();
      setRoutes(data);
    } catch (err) {
      setError("Failed to load routes. Pull to try again.");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const onRefresh = () => {
    setRefreshing(true);
    loadData();
  };

  const renderContent = () => {
    // if (loading && !refreshing) {
    //   return (
    //     <View style={styles.centerContainer}>
    //       <ActivityIndicator size="large" />
    //     </View>
    //   );
    // }

    if (error) {
      return (
        <View style={styles.centerContainer}>
          <ThemedText style={{ color: 'white' }}>{error}</ThemedText>
        </View>
      );
    }

    if (routes.length === 0) {
      return (
        <View style={styles.centerContainer}>
          <ThemedText type="defaultSemiBold">No routes found.</ThemedText>
          <ThemedText type="small" style={{ opacity: 0.6 }}>
            Your scheduled rides will appear here.
          </ThemedText>
        </View>
      );
    }

    return routes.map((routeItem) => (
      <RouteThumbnail2
        key={routeItem.id}
        route={routeItem}
      />
    ));
  };

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.content}>
        {/* Header */}
        <View style={styles.header}>
          <View style={[styles.money, { backgroundColor: Colors[theme].backgroundCard }]}>
            <Image source={require('@/assets/images/money.png')} style={{ width: 32, height: 32 }} />
            <ThemedText type='subtitle'>320.47</ThemedText>
          </View>

          <View style={{ display: 'flex', flexDirection: 'row', gap: 16 }}>
            <Ionicons style={{ backgroundColor: Colors[theme].backgroundCard, borderRadius: 999, padding: 12 }}
              name="search-outline" size={24} color={Colors[theme].icon} />
            <Ionicons style={{ backgroundColor: Colors[theme].backgroundCard, borderRadius: 999, padding: 12 }}
              name="notifications-outline" size={24} color={Colors[theme].icon} />
          </View>
        </View>

        <ThemedText type='title' style={styles.title}>Travel Made Effortless</ThemedText>

        {/* Vehicles */}
        <View style={{ display: 'flex', flexDirection: 'row', gap: 4, width: '100%' }}>
          <View style={[styles.vehicleCards, { backgroundColor: "#CEEDF0" }]}><ThemedText type='thin'>Trains</ThemedText></View>
          <View style={[styles.vehicleCards, { backgroundColor: "#D7E1FB" }]}><ThemedText type='thin'>Flights</ThemedText></View>
          <View style={[styles.vehicleCards, { backgroundColor: "#D3D1FB" }]}><ThemedText type='thin'>Boats</ThemedText></View>
          <View style={[styles.vehicleCards, { backgroundColor: "#F6CFC4" }]}><ThemedText type='thin'>Bus</ThemedText></View>
        </View>

        <View style={styles.upcomingSchedules}>
          {/* Upcoming Schedules Title */}
          <View style={{ display: 'flex', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
            <ThemedText type='subtitle' style={{ marginBottom: 8 }}>Upcoming Schedules</ThemedText>
            <Link href="/rides"><ThemedText type='link' style={{ color: Colors[theme].primary }}>View All</ThemedText></Link>
          </View>

          <ScrollView
            horizontal={true}
            showsHorizontalScrollIndicator={false}
            style={styles.scheduleContent}
          >
            {renderContent()}
          </ScrollView>

        </View>
      </SafeAreaView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 40,
    gap: 8,
  },
  container: {
    flex: 1,
  },
  content: {
    flex: 1,
    padding: 16,
    justifyContent: 'flex-start',
    alignItems: 'flex-start',
  },
  header: {
    width: '100%',
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  money: {
    width: 'auto',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 999,
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  title: {
    display: 'flex',
    width: '60%',
    marginVertical: 32,
  },
  vehicleCards: {
    aspectRatio: 7 / 6,
    width: '24%',
    height: 'auto',
    borderRadius: 16,
    display: 'flex',
    justifyContent: 'flex-start',
    alignItems: 'flex-start',
    padding: 8,
  },
  upcomingSchedules: {
    width: '100%',
    height: 300,
    marginTop: 20,
  },
  scheduleContent: {
    width: '100%',
    maxHeight: '100%',
    paddingVertical: 8,
  },
  scheduleContainer: {
    aspectRatio: 5 / 4,
    width: "70%",
    height: 70,
    backgroundColor: '#fff',
    borderRadius: 24,
    padding: 8,
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
  },
});