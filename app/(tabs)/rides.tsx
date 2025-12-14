import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  RefreshControl,
  ScrollView,
  StyleSheet,
  View
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import RouteThumbnail from '@/components/route-thumbnail';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { fetchRoutes } from '@/constants/api';
import { RouteData } from '@/constants/types/interfaces';


export default function RoutesScreen() {
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
    if (loading && !refreshing) {
      return (
        <View style={styles.centerContainer}>
          <ActivityIndicator size="small" />
        </View>
      );
    }

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
      <RouteThumbnail 
        key={routeItem.id} 
        route={routeItem} 
      />
    ));
  };

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.safeArea} edges={['top']}> 
        <ScrollView
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
          }
        >
          <ThemedText type='title' style={styles.title}>My rides</ThemedText>
          
          {renderContent()}

        </ScrollView>
      </SafeAreaView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  safeArea: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: 16,
    paddingBottom: 100, // Espaço extra para o TabBar não cobrir o último item
    minHeight: '100%',
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 40,
    gap: 8,
  },
  title: {
    marginTop: 24,
    marginBottom: 20,
  }
});