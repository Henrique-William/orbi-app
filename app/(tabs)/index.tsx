import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  useColorScheme,
  View
} from 'react-native';
import MapView from 'react-native-maps';
import { SafeAreaView } from 'react-native-safe-area-context';

import RouteThumbnailHome from '@/components/route-thumbnail-home';
import { ThemedText } from '@/components/themed-text';
import { fetchRoutes } from '@/constants/api';
import mapStyle from '@/constants/map/mapStyle.json';
import { RouteData } from '@/constants/types/interfaces';
import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import * as Location from 'expo-location';
import { ScrollView } from 'react-native';

export default function HomeScreen() {
  const theme = useColorScheme() ?? 'light';
  const [routes, setRoutes] = useState<RouteData[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [location, setLocation] = useState<Location.LocationObject | null>(null);
  const [isLocationReady, setIsLocationReady] = useState(false);

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

  useEffect(() => {
    let locationSubscription: Location.LocationSubscription | null = null;

    (async () => {
      let { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        setIsLocationReady(true);
        return;
      }

      // 1. Pega a primeira localização rápido para carregar o mapa imediatamente
      let initialLoc = await Location.getCurrentPositionAsync({});
      setLocation(initialLoc);
      setIsLocationReady(true);

      // 2. Inicia o rastreamento em tempo real
      locationSubscription = await Location.watchPositionAsync(
        {
          accuracy: Location.Accuracy.High,
          timeInterval: 200, // Atualiza a cada 1 segundo
          distanceInterval: 5, // Ou a cada 5 metros de movimento
        },
        (newLocation) => {
          setLocation(newLocation);
        }
      );
    })();

    // 3. Limpa o listener quando o componente for desmontado para evitar vazamento de memória
    return () => {
      if (locationSubscription) {
        locationSubscription.remove();
      }
    };
  }, []);

  const onRefresh = () => {
    setRefreshing(true);
    loadData();
  };

  const renderContent = () => {
    // ... (mantido igual ao seu código original)
    if (loading && !refreshing) {
      return (
        <View style={styles.centerContainer}>
          <ActivityIndicator size="large" />
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
      <RouteThumbnailHome
        key={routeItem.id}
        route={routeItem}
      />
    ));
  };

  const [search, setSearch] = useState('');
  const filterOptions = ['All', 'Parking', 'Vehicle', 'Motorbike', 'Bicycle', 'Bus'];

  return (
    <View style={styles.container}>

      {/* 1. MAPA NO FUNDO DE TUDO */}
      <View style={styles.mapContainer}>
        {isLocationReady && (
          <MapView
            style={styles.map}
            customMapStyle={mapStyle}
            initialRegion={{
              latitude: location?.coords.latitude ?? -22.8250,
              longitude: location?.coords.longitude ?? -47.2650,
              latitudeDelta: 0.01,
              longitudeDelta: 0.01,
            }}
            showsUserLocation={true}
            showsMyLocationButton={true}
            pitchEnabled={false}
            rotateEnabled={false}
          />
        )}
      </View>

      <LinearGradient
        colors={['transparent', '#F4F5FC', '#F4F5FC']}
        style={styles.gradientOverlayBottom}
        pointerEvents="none"
      />

      {/* 2. INTERFACE FLUTUANDO NA FRENTE */}
      <SafeAreaView style={styles.content} pointerEvents="box-none">

        <View style={styles.header} pointerEvents="box-none">
          {/* Gradiente cobrindo o header e "sumindo" para revelar o mapa */}
          <LinearGradient
            colors={['#F4F5FC', '#F4F5FC', '#F4F5FC', '#F4F5FC', 'transparent']}
            style={styles.gradientOverlayTop}
            pointerEvents="none"
          />

          <ThemedText type='titleBold' style={styles.title}>Comece Otimizando Sua Nova Rota</ThemedText>

          <View style={styles.searchBarContainer}>
            <TextInput
              style={styles.searchBar}
              placeholder="Digite o nome de uma cidade..."
              value={search}
              onChangeText={(text) => setSearch(text)}
            />
            <TouchableOpacity style={styles.searchButton}>
              <Ionicons name="locate-outline" size={28} color="white" />
            </TouchableOpacity>
          </View>

          <FlatList
            horizontal={true}
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={{ gap: 4, paddingHorizontal: 8 }}
            style={styles.filterSection}
            data={filterOptions}
            renderItem={({ item }) => (
              <TouchableOpacity style={styles.filters}>
                <ThemedText type='thin' style={{ color: 'white' }}>
                  {item}
                </ThemedText>
              </TouchableOpacity>
            )}
            keyExtractor={(item) => item}
          />
        </View>

        {/*Uppcomming schedule*/}
        <View style={styles.upcomingSchedules}>
          <ScrollView
            horizontal={true}
            showsHorizontalScrollIndicator={false}
            style={styles.scheduleContent}
          >
            {renderContent()}
          </ScrollView>

        </View>

      </SafeAreaView>
    </View>
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
    backgroundColor: 'transparent', // Garante que o fundo geral não bloqueie o mapa
  },
  content: {
    flex: 1,
  },
  mapContainer: {
    ...StyleSheet.absoluteFillObject, // Faz o container do mapa pegar a tela INTEIRA
    zIndex: 0,
  },
  map: {
    ...StyleSheet.absoluteFillObject, // Faz o mapa seguir o container
  },
  header: {
    width: '100%',
    paddingHorizontal: 16,
    paddingBottom: 60,
    position: 'relative',
  },
  gradientOverlayTop: {
    position: 'absolute',
    left: 0,
    right: 0,
    top: -50, // Começa um pouco acima do header para cobrir a status bar
    bottom: 0, // Desce até o fim do header (onde tem o paddingBottom)
    zIndex: -1, // Fica atrás dos textos/inputs, mas na frente do mapa
  },
  title: {
    display: 'flex',
    textAlign: 'center',
    marginVertical: 32,
  },
  searchBarContainer: {
    width: '100%',
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    gap: 16,
    zIndex: 2,
  },
  searchBar: {
    flex: 1,
    height: 60,
    borderRadius: 32,
    paddingHorizontal: 20,
    backgroundColor: '#fff',
    elevation: 5, // Dá uma sombra no Android para destacar do mapa
    shadowColor: '#000', // Sombra no iOS
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  searchButton: {
    width: 50,
    height: 50,
    borderRadius: 60,
    backgroundColor: '#000',
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
  },
  filterSection: {
    marginTop: 24,
    width: '100%',
    zIndex: 2,
  },
  filters: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 32,
    backgroundColor: '#000',
  },
  upcomingSchedules: {
    position: 'absolute',
    bottom: 0,
    width: '100%',
    height: '30%',
  },
  scheduleContent: {
    width: '100%',
    maxHeight: '100%',
    paddingVertical: 8,
    position: 'absolute',
    zIndex: 1,
    bottom: '40%',
    left: 16,
    marginRight: 16,
  },
  gradientOverlayBottom: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    height: '20%',
  }
});