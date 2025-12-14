import { Ionicons } from '@expo/vector-icons';
import { format, isToday } from 'date-fns'; // Importe isToday
import { ptBR } from 'date-fns/locale';
import { Link, Stack, useLocalSearchParams, useRouter } from 'expo-router'; // Adicione useRouter
import React, { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, TouchableOpacity, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { fetchRouteDetails } from '@/constants/api';
import { Colors } from '@/constants/theme';
import { RouteData } from '@/constants/types/interfaces';
import { useColorScheme } from '@/hooks/use-color-scheme';

export default function RideDetailsScreen() {
  const { id } = useLocalSearchParams();
  const router = useRouter();
  const [route, setRoute] = useState<RouteData | null>(null);
  const [loading, setLoading] = useState(true);
  const theme = useColorScheme() ?? 'light';

  useEffect(() => {
    if (id) {
      loadRouteDetails(Number(id));
    }
  }, [id]);

  const loadRouteDetails = async (routeId: number) => {
    try {
      const data = await fetchRouteDetails(routeId);
      setRoute(data);
    } catch (error) {
      console.error("Erro ao carregar rota", error);
    } finally {
      setLoading(false);
    }
  };

  const formattedDate = useMemo(() => {
    if (!route?.createdAt) return '';

    const dateToUse = new Date(route.createdAt);
    if (isToday(dateToUse)) {
      return `Today, ${format(dateToUse, 'MMMM d', { locale: ptBR })}`;
    }
    return format(dateToUse, 'MMMM d, yyyy', { locale: ptBR });
  }, [route?.createdAt]);

  // 3. Só AGORA podemos ter os retornos condicionais (Early returns)
  if (loading) {
    return (
      <ThemedView style={styles.centerContainer}>
        <ActivityIndicator size="large" />
      </ThemedView>
    );
  }

  if (!route) {
    return (
      <ThemedView style={styles.centerContainer}>
        <ThemedText>Route not found.</ThemedText>
      </ThemedView>
    );
  }

  return (
    <ThemedView style={styles.container}>
      <Stack.Screen options={{
        title: `Route #${route.id}`,
        headerBackTitle: 'Back', 
        headerShown: false
      }} />

      <ScrollView contentContainerStyle={styles.scrollContent}>

        <View style={styles.headerSection}>
          <Link href="../rides" asChild>
            <TouchableOpacity onPress={() => router.back()}>
              <Ionicons name="arrow-back-outline" size={24} color={Colors[theme].text} />
            </TouchableOpacity>
          </Link>

          <ThemedText type="thin" style={{ opacity: 0.7 }}>
            {formattedDate}
          </ThemedText>

          <Ionicons name="document-text" size={32} color={Colors[theme].text} />
        </View>

        <View style={{ marginTop: 20 }}>
          <ThemedText type="title">Route #{route.id}</ThemedText>
        </View>

      </ScrollView>
    </ThemedView>
  );
}
const styles = StyleSheet.create({
  container: { flex: 1 },
  centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  scrollContent: { padding: 16, paddingBottom: 40 },
  headerSection: {
    marginBottom: 24,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 8
  },
  driverBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    borderRadius: 12,
    borderWidth: 1,
    gap: 8,
    marginTop: 8,
  },
  sectionTitle: { marginBottom: 12 },
  deliveriesList: { gap: 16 },
  deliveryCard: {
    borderRadius: 12,
    borderWidth: 1,
    padding: 16,
    gap: 12,
  },
  deliveryHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  badge: {
    backgroundColor: '#0a7ea4',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
  },
  row: {
    flexDirection: 'row',
    gap: 8,
    alignItems: 'center', // Corrigido para alinhar ícone com texto
  },
  addressText: {
    flex: 1, // Permite quebra de linha se o endereço for longo
  },
  detailsContainer: {
    flexDirection: 'row',
    backgroundColor: 'rgba(150, 150, 150, 0.1)',
    padding: 10,
    borderRadius: 8,
    justifyContent: 'space-between',
  },
  detailItem: {
    flex: 1,
    gap: 4,
  },
  label: { opacity: 0.6 },
  footerTime: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginTop: 4,
  }
});