import { useRouter } from 'expo-router';
import { useState } from 'react';
import {
  Alert,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  View
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { API_URLS } from '@/constants/api';

export default function CreateRouteScreen() {
  const router = useRouter();
  const [locations, setLocations] = useState<any[]>([]);
  const [formData, setFormData] = useState({
    address: '',
    latitude: '',
    longitude: '',
    recipientName: '',
    recipientPhone: '',
    recipientEmail: '',
    packageDetails: '',
  });
  const [isOptimizing, setIsOptimizing] = useState(false);

  const handleChange = (name: string, value: string) => {
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleAddLocation = () => {
    if (!formData.address || !formData.recipientName) {
      Alert.alert('Erro', 'Preencha endereço e nome do destinatário.');
      return;
    }

    const lat = parseFloat(formData.latitude.replace(',', '.'));
    const lng = parseFloat(formData.longitude.replace(',', '.'));

    if (isNaN(lat) || lat < -90 || lat > 90) {
      Alert.alert('Erro', 'Latitude inválida. O valor deve estar entre -90 e 90.');
      return;
    }

    if (isNaN(lng) || lng < -180 || lng > 180) {
      Alert.alert('Erro', 'Longitude inválida. O valor deve estar entre -180 e 180.');
      return;
    }

    const newLocation = {
      ...formData,
      latitude: lat,
      longitude: lng,
      driverId: 'b3bbb79b-1182-4b20-94c0-ebe2a17dfbb6', // MOCK
    };
    setLocations((prev) => [...prev, newLocation]);

    setFormData({
      address: '',
      latitude: '',
      longitude: '',
      recipientName: '',
      recipientPhone: '',
      recipientEmail: '',
      packageDetails: '',
    });
  };

  const handleRemoveLocation = (indexToRemove: number) => {
    setLocations((prev) => prev.filter((_, index) => index !== indexToRemove));
  };

  const handleOptimizeRoute = async () => {
    if (locations.length < 2) {
      Alert.alert('Erro', 'Adicione pelo menos 2 paradas.');
      return;
    }

    setIsOptimizing(true);
    try {
      const response = await fetch(API_URLS.OPTIMIZE_ROUTE, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(locations),
      });

      if (!response.ok) {
        throw new Error('Falha ao criar rota');
      }

      Alert.alert('Sucesso', 'Rota criada e otimizada com sucesso!');
      router.back();
    } catch (err) {
      Alert.alert('Erro', 'Falha na otimização');
    } finally {
      setIsOptimizing(false);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={styles.scrollContent}>
          <ThemedText type="title" style={{ marginBottom: 16 }}>Criar Rota</ThemedText>

          <ThemedView style={styles.formContainer}>
            <View style={styles.inputGroup}>
              <ThemedText style={styles.label}>Endereço</ThemedText>
              <TextInput style={styles.input} value={formData.address} onChangeText={(t) => handleChange('address', t)} />
            </View>
            
            <View style={{ flexDirection: 'row', gap: 8 }}>
              <View style={{ flex: 1 }}>
                <ThemedText style={styles.label}>Latitude</ThemedText>
                <TextInput style={styles.input} keyboardType="numeric" value={formData.latitude} onChangeText={(t) => handleChange('latitude', t)} />
              </View>
              <View style={{ flex: 1 }}>
                <ThemedText style={styles.label}>Longitude</ThemedText>
                <TextInput style={styles.input} keyboardType="numeric" value={formData.longitude} onChangeText={(t) => handleChange('longitude', t)} />
              </View>
            </View>

            <View style={styles.inputGroup}>
              <ThemedText style={styles.label}>Nome destinatário</ThemedText>
              <TextInput style={styles.input} value={formData.recipientName} onChangeText={(t) => handleChange('recipientName', t)} />
            </View>

            <View style={{ flexDirection: 'row', gap: 8 }}>
              <View style={{ flex: 1 }}>
                <ThemedText style={styles.label}>Telefone</ThemedText>
                <TextInput style={styles.input} keyboardType="phone-pad" value={formData.recipientPhone} onChangeText={(t) => handleChange('recipientPhone', t)} />
              </View>
              <View style={{ flex: 1 }}>
                <ThemedText style={styles.label}>Email</ThemedText>
                <TextInput style={styles.input} keyboardType="email-address" value={formData.recipientEmail} onChangeText={(t) => handleChange('recipientEmail', t)} />
              </View>
            </View>

            <View style={styles.inputGroup}>
              <ThemedText style={styles.label}>Detalhes</ThemedText>
              <TextInput style={styles.input} multiline={true} value={formData.packageDetails} onChangeText={(t) => handleChange('packageDetails', t)} />
            </View>

            <TouchableOpacity style={styles.addButton} onPress={handleAddLocation}>
              <ThemedText style={{ color: '#fff', fontWeight: 'bold' }}>Adicionar Parada</ThemedText>
            </TouchableOpacity>
          </ThemedView>

          <ThemedText type="subtitle" style={{ marginTop: 24, marginBottom: 12 }}>Paradas ({locations.length})</ThemedText>
          {locations.map((loc, index) => (
            <ThemedView key={index} style={styles.locationItem}>
              <View style={{ flex: 1 }}>
                <ThemedText type="defaultSemiBold">Parada {index + 1}</ThemedText>
                <ThemedText style={{ fontSize: 12 }}>{loc.address}</ThemedText>
              </View>
              <TouchableOpacity onPress={() => handleRemoveLocation(index)}>
                <ThemedText style={{ color: 'red', fontWeight: 'bold' }}>X</ThemedText>
              </TouchableOpacity>
            </ThemedView>
          ))}

          <TouchableOpacity
            style={[styles.optimizeButton, locations.length < 2 && { opacity: 0.5 }]}
            onPress={handleOptimizeRoute}
            disabled={locations.length < 2 || isOptimizing}
          >
            <ThemedText style={{ color: '#fff', fontWeight: 'bold' }}>
              {isOptimizing ? 'Otimizando...' : 'Otimizar Rota'}
            </ThemedText>
          </TouchableOpacity>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  scrollContent: {
    padding: 16,
    paddingBottom: 100,
  },
  formContainer: {
    padding: 16,
    borderRadius: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 2,
  },
  inputGroup: {
    marginBottom: 12,
  },
  label: {
    fontSize: 12,
    marginBottom: 4,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 8,
    padding: 10,
  },
  addButton: {
    backgroundColor: '#450693',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 8,
  },
  locationItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 12,
    borderLeftWidth: 4,
    borderLeftColor: '#450693',
    backgroundColor: '#f8f8f8',
    marginBottom: 8,
    borderRadius: 4,
  },
  optimizeButton: {
    backgroundColor: '#2868de',
    padding: 16,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 16,
  },
});