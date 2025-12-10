import { Ionicons } from '@expo/vector-icons';
import React from 'react';

import { Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';
import { Image, StyleSheet, View } from 'react-native';
import { ThemedText } from './themed-text';

export default function RouteThumbnail() {
    const theme = useColorScheme() ?? 'light';
    const cardBackgroundColor = theme === 'light' ? '#F4F4F5' : '#27272a'; // Zinc-100 ou Zinc-800
    const iconPrimaryColor = Colors[theme].tint;
    const inactiveColor = Colors[theme].inactive;

    return (
        <View style={styles.container}>
            {/* Cabeçalho da Seção */}
            <ThemedText type="subtitle" style={styles.dateHeader}>
                Today, December 9
            </ThemedText>

            {/* Card Principal */}
            <View style={styles.card}>

                {/* Topo do Card (Preço e Infos) */}
                <View style={[styles.cardHeader, { backgroundColor: cardBackgroundColor }]}>
                    <ThemedText type="title2">$ 43.12</ThemedText>

                    <View style={styles.metaContainer}>
                        {/* Distância */}
                        <View style={styles.metaItem}>
                            <Ionicons name="map-outline" size={16} color={iconPrimaryColor} />
                            <ThemedText type="default">26.5 mi</ThemedText>
                        </View>

                        {/* Tempo */}
                        <View style={styles.metaItem}>
                            <Ionicons name="time-outline" size={16} color="#ddb810" />
                            <ThemedText type="default">1h 17m</ThemedText>
                        </View>
                    </View>
                </View>

                {/* Corpo do Card (Placeholder para mapa ou imagem) */}
                <View style={[styles.cardContent, { backgroundColor: cardBackgroundColor }]}>

                    {/* Inicio da Rota */}
                    <View style={styles.destinationInfo}>
                        <Ionicons name="play-outline" size={20} color={inactiveColor} />
                        <View style={styles.destinationAddress}>
                            <View style={styles.destinationTitle}><ThemedText type='thin'>Plaza Gate</ThemedText><ThemedText type='thin'>8:02 AM</ThemedText></View>
                            <View><ThemedText type='small' >Syosset, NY 11791, USA</ThemedText></View>
                        </View>
                    </View>

                    {/* Termino da Rota */}
                    <View style={styles.destinationInfo}>
                        <Ionicons name="flag-outline" size={20} color={inactiveColor} />
                        <View style={styles.destinationAddress}>
                            <View style={styles.destinationTitle}><ThemedText type='thin'>142-21 26th Ave</ThemedText><ThemedText type='thin'>9:19 AM</ThemedText></View>
                            <View><ThemedText type='small' style={{ color: inactiveColor }}>Queens, Ny 11354, USA</ThemedText></View>
                        </View>
                    </View>

                    {/* Mapa */}
                    <Image
                        source={require('@/assets/images/map.png')}
                        style={styles.mapImage}
                        resizeMode="cover"
                    />

                </View>
            </View>
        </View>
    );
}
const styles = StyleSheet.create({
    container: {
        width: '100%',
        gap: 8,
        marginBottom: 16, // Espaço abaixo do card
    },
    dateHeader: {
        marginBottom: 4,
        paddingHorizontal: 4,
    },
    card: {
        borderRadius: 16,
        overflow: 'hidden',
        gap: 4,
    },

    // Card Header
    cardHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: 8,
    },
    metaContainer: {
        flexDirection: 'row',
        gap: 12,
        alignItems: 'center',
    },
    metaItem: {
        flexDirection: 'row',
        gap: 4,
        alignItems: 'center',
    },

    // Card body
    cardContent: {
        width: '100%',
        padding: 8,
        flexDirection: 'column',
        gap: 12,
    },
    destinationIcon: {
        height: '100%',
        justifyContent: 'center',
        alignItems: 'center',
        flexDirection: 'column',
        gap: 8,
        width: '10%',
    },
    destinationInfo: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    destinationAddress: {
        flexDirection: 'column',
        paddingLeft: 12,
        width: '90%',
    },
    destinationTitle: {
        flexDirection: 'row',
        justifyContent: 'space-between',
    },
    // Mapa 
    mapImage: {
        width: '100%',
        height: 150,
        borderRadius: 12,
    }
});
