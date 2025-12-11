import { useEffect, useState } from 'react';
import { ActivityIndicator, Platform, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import RouteThumbnail, { RouteData } from '@/components/route-thumbnail';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';

const LOCAL_MACHINE_IP = '192.168.1.60'; 

const API_URL = Platform.select({
  // Android no dispositivo físico precisa do IP real e da porta correta (8080)
  android: `http://${LOCAL_MACHINE_IP}:8080/api/route`,
  ios: `http://${LOCAL_MACHINE_IP}:8080/api/route`,
  default: `http://localhost:8080/api/route`,
});

export default function Routes() {
  const [routes, setRoutes] = useState<RouteData[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fetchRoutes = async () => {
    try {
      console.log("Fetching from:", API_URL);
      const response = await fetch(API_URL!); // Adicionar '!' pois definimos uma string padrão
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const data = await response.json();
      setRoutes(data);
    } catch (error) {
      console.error("Erro ao buscar rotas:", error);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchRoutes();
  }, []);

  const onRefresh = () => {
    setRefreshing(true);
    fetchRoutes();
  };

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.safeArea}>
        <ScrollView
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
          }
        >
          <View style={styles.dropdown}></View>
          <ThemedText type='title' style={styles.title}>My rides</ThemedText>

          {loading ? (
            <ActivityIndicator size="large" style={{ marginTop: 20 }} />
          ) : (
            <>
              {routes.length === 0 ? (
                <ThemedText>No routes found.</ThemedText>
              ) : (
                routes.map((routeItem) => (
                  <RouteThumbnail 
                    key={routeItem.id} 
                    route={routeItem} 
                  />
                ))
              )}
            </>
          )}

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
    paddingBottom: 32,
    justifyContent: 'flex-start',
    alignItems: 'flex-start',
    minHeight: '100%',
  },
  dropdown: {
    height: 40,
    width: '100%',
    marginBottom: 16,
  },
  title: {
    marginVertical: 16,
  }
});