import { useState } from 'react';
import { ActivityIndicator, FlatList, StyleSheet, TextInput, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';

interface LocationData {
    cep: string;
    address: string;
    latitude: number;
    longitude: number;
}

interface NominatimResult {
    place_id: number;
    lat: string;
    lon: string;
    display_name: string;
    address?: {
        road?: string;
        pedestrian?: string;
        suburb?: string;
        neighbourhood?: string;
        residential?: string;
        city?: string;
        town?: string;
        municipality?: string;
        village?: string;
        state?: string;
        postcode?: string;
    };
}

const getUF = (stateName?: string) => {
    if (!stateName) return '';
    const map: Record<string, string> = {
        'acre': 'AC', 'alagoas': 'AL', 'amapá': 'AP', 'amazonas': 'AM', 'bahia': 'BA', 'ceará': 'CE', 'distrito federal': 'DF', 'espírito santo': 'ES', 'goiás': 'GO', 'maranhão': 'MA', 'mato grosso': 'MT', 'mato grosso do sul': 'MS', 'minas gerais': 'MG', 'pará': 'PA', 'paraíba': 'PB', 'paraná': 'PR', 'pernambuco': 'PE', 'piauí': 'PI', 'rio de janeiro': 'RJ', 'rio grande do norte': 'RN', 'rio grande do sul': 'RS', 'rondônia': 'RO', 'roraima': 'RR', 'santa catarina': 'SC', 'são paulo': 'SP', 'sergipe': 'SE', 'tocantins': 'TO'
    };
    return map[stateName.toLowerCase()] || '';
};

export default function ride() {
    const [query, setQuery] = useState('');
    const [suggestions, setSuggestions] = useState<NominatimResult[]>([]);
    const [selectedLocation, setSelectedLocation] = useState<LocationData | null>(null);
    const [loading, setLoading] = useState(false);
    const [debounceTimer, setDebounceTimer] = useState<ReturnType<typeof setTimeout> | null>(null);

    const theme = useColorScheme() ?? 'light';
    const textColor = Colors[theme].text;
    const inputBg = theme === 'dark' ? '#2c2c2e' : '#f0f0f0';

    const searchAddress = async (text: string) => {
        setLoading(true);
        try {
            const response = await fetch(
                `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(text)}&addressdetails=1&limit=5&countrycodes=br`,
                { headers: { 'User-Agent': 'OrbiApp/1.0' } }
            );
            const data = await response.json();
            setSuggestions(data);
        } catch (error) {
            console.error('Error fetching address:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleTextChange = (text: string) => {
        setQuery(text);
        if (debounceTimer) clearTimeout(debounceTimer);
        if (text.length > 2) {
            const timer = setTimeout(() => searchAddress(text), 800);
            setDebounceTimer(timer);
        } else {
            setSuggestions([]);
        }
    };

    const formatAddress = (item: NominatimResult) => {
        const addr = item.address;
        if (!addr) return item.display_name;

        const street = addr.road || addr.pedestrian || '';
        const neighborhood = addr.suburb || addr.neighbourhood || addr.residential || '';
        const city = addr.city || addr.town || addr.municipality || addr.village || '';
        const state = addr.state || '';

        const parts = [street, neighborhood, city].filter(Boolean);
        let text = parts.join(', ');
        if (state) text += ` - ${state}`;

        return text || item.display_name;
    };

    const handleSelect = async (item: NominatimResult) => {
        let finalAddress = formatAddress(item);
        let finalCep = item.address?.postcode || 'N/A';

        // Tenta corrigir dados usando ViaCEP
        if (item.address) {
            const { state, city, town, municipality, village, road, pedestrian } = item.address;
            const cityName = city || town || municipality || village;
            const streetName = road || pedestrian;
            const uf = getUF(state);

            if (uf && cityName && streetName) {
                try {
                    const cleanCity = cityName.normalize("NFD").replace(/[\u0300-\u036f]/g, "");
                    const cleanStreet = streetName.normalize("NFD").replace(/[\u0300-\u036f]/g, "");

                    const response = await fetch(`https://viacep.com.br/ws/${uf}/${encodeURIComponent(cleanCity)}/${encodeURIComponent(cleanStreet)}/json/`);
                    const data = await response.json();

                    if (Array.isArray(data) && data.length > 0) {
                        finalCep = data[0].cep;
                        finalAddress = `${data[0].logradouro}, ${data[0].bairro}, ${data[0].localidade} - ${data[0].uf}`;
                    }
                } catch (error) {
                    console.log('Erro ao buscar no ViaCEP:', error);
                }
            }
        }

        const location: LocationData = {
            cep: finalCep,
            address: finalAddress,
            latitude: parseFloat(item.lat),
            longitude: parseFloat(item.lon),
        };
        setSelectedLocation(location);
        setQuery(finalAddress);
        setSuggestions([]);
    };

    return (
        <ThemedView style={styles.container}>
            <SafeAreaView style={styles.content}>
                <ThemedText type='titleBold'>Home</ThemedText>

                <View style={styles.inputContainer}>
                    <ThemedText type="defaultSemiBold" style={styles.label}>Search Address</ThemedText>
                    <View style={styles.inputWrapper}>
                        <TextInput
                            style={[styles.input, { color: textColor, backgroundColor: inputBg }]}
                            placeholder="Type an address..."
                            placeholderTextColor="#888"
                            value={query}
                            onChangeText={handleTextChange}
                        />
                        {loading && <ActivityIndicator size="small" style={styles.loader} />}
                    </View>
                    {suggestions.length > 0 && (
                        <View style={[styles.suggestionsList, { backgroundColor: inputBg }]}>
                            <FlatList
                                data={suggestions}
                                keyExtractor={(item) => item.place_id.toString()}
                                keyboardShouldPersistTaps="handled"
                                renderItem={({ item }) => (
                                    <TouchableOpacity onPress={() => handleSelect(item)} style={styles.suggestionItem}>
                                        <ThemedText numberOfLines={2} style={{ fontSize: 14 }}>{formatAddress(item)}</ThemedText>
                                    </TouchableOpacity>
                                )}
                            />
                        </View>
                    )}
                </View>

                {selectedLocation && (
                    <View style={styles.resultContainer}>
                        <ThemedText type="subtitle">Selected Location:</ThemedText>
                        <ThemedText>CEP: {selectedLocation.cep}</ThemedText>
                        <ThemedText>Lat: {selectedLocation.latitude}</ThemedText>
                        <ThemedText>Lon: {selectedLocation.longitude}</ThemedText>
                        <ThemedText style={{ marginTop: 4 }}>Address: {selectedLocation.address}</ThemedText>
                    </View>
                )}
            </SafeAreaView>
        </ThemedView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    content: {
        paddingHorizontal: 16,
        justifyContent: 'flex-start',
        alignItems: 'flex-start',
        flex: 1,
    },
    inputContainer: {
        width: '100%',
        marginTop: 20,
        zIndex: 10,
    },
    label: {
        marginBottom: 8,
    },
    inputWrapper: {
        position: 'relative',
        justifyContent: 'center',
    },
    input: {
        height: 50,
        borderRadius: 8,
        paddingHorizontal: 12,
        paddingRight: 40,
        fontSize: 16,
    },
    loader: {
        position: 'absolute',
        right: 12,
    },
    suggestionsList: {
        position: 'absolute',
        top: '100%',
        left: 0,
        right: 0,
        marginTop: 4,
        borderRadius: 8,
        maxHeight: 200,
        zIndex: 10,
        elevation: 5,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.25,
        shadowRadius: 3.84,
    },
    suggestionItem: {
        padding: 12,
        borderBottomWidth: 0.5,
        borderBottomColor: 'rgba(150,150,150,0.2)',
    },
    resultContainer: {
        marginTop: 24,
        padding: 16,
        borderRadius: 8,
        backgroundColor: 'rgba(150, 150, 150, 0.1)',
        gap: 4,
        zIndex: 1,
    },
});
