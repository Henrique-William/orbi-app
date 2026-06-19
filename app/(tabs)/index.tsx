import { useCallback, useEffect, useState } from 'react';
import {
  FlatList,
  ScrollView,
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
import { ThemedView } from '@/components/themed-view';
import { fetchRoutes } from '@/constants/api';
import mapStyle from '@/constants/map/mapStyle.json';
import { RouteData } from '@/constants/types/interfaces';
import { Ionicons } from '@expo/vector-icons';
import * as Location from 'expo-location';

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
    (async () => {
      let { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        setIsLocationReady(true); // Permissão negada, usa o padrão mesmo
        return;
      }

      let loc = await Location.getCurrentPositionAsync({});
      setLocation(loc);
      setIsLocationReady(true); // Agora temos a real localização
    })();
  }, []);

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
      <RouteThumbnailHome
        key={routeItem.id}
        route={routeItem}
      />
    ));
  };

  const [search, setSearch] = useState('');

  const filterOptions = ['All', 'Parking', 'Vehicle', 'Motorbike', 'Bicycle', 'Bus'];

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.content}>
        {/* Header */}
        <View style={styles.header}>
          <ThemedText type='titleBold' style={styles.title}>Começe Otmizando Sua Nova Rota</ThemedText>

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
            contentContainerStyle={{ gap: 4, paddingHorizontal: 8 }} // Adicione padding se necessário
            style={styles.filterSection} // Garanta que este estilo não tenha altura fixa que conflite
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

          {/* <View style={{styles.}}></View> */}

        </View>


        {/* Map container */}
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
            />
          )}

          {/* <LinearGradient
            colors={['transparent', '#F4F5FC']}
            style={styles.gradientOverlay}
          /> */}
          {/* <LinearGradient
            colors={['#F4F5FC', 'transparent']}
            style={styles.gradientOverlay}
          /> */}

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
    justifyContent: 'flex-start',
    alignItems: 'flex-start',
  },
  header: {
    width: '100%',
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'space-between',
    alignItems: 'center',
    textAlign: 'center',
    paddingHorizontal: 16
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
  },
  searchBar: {
    flex: 1,
    height: 60,
    borderRadius: 32,
    paddingHorizontal: 20,
    backgroundColor: '#fff',
  },
  searchButton: {
    width: 50,
    height: 50,
    borderRadius: 60,
    backgroundColor: '#000',
    justifyContent: 'center',
    alignItems: 'center',
  },
  filterSection: {
    marginTop: 24,
    width: '100%',
    display: 'flex',
    flexDirection: 'row',
    position: 'absolute',
    top: '100%',
    zIndex: 1,
  },
  filters: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 32,
    backgroundColor: '#000',
  },
  mapContainer: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    width: '100%',
    height: '55%',
    marginTop: 48,
  },
  map: {
    width: '100%',
    height: '100%'
  },
  upcomingSchedules: {
    width: '100%',
    height: 300,
    marginTop: 20,
  },
  gradientOverlay: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    height: '100%',
  },
  scheduleContent: {
    width: '100%',
    maxHeight: '100%',
    paddingVertical: 8,
    position: 'absolute',
    zIndex: 1,
    top: '55%',
    left: 16,
    marginRight: 16,
  }
});