import { Ionicons } from '@expo/vector-icons';
import React from 'react';
import { Image, StyleSheet, View } from 'react-native';

import { Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';
import { ThemedText } from './themed-text';

import { format, isToday } from 'date-fns';
import { enUS } from 'date-fns/locale'; // Use ptBR se quiser em português

// 1. Definimos a interface das props
export interface Delivery {
    id: number;
    order: number;
    status: string;
    dropoffAddress: string;
    // Datas em JSON sempre vêm como string
    pickUpDateTime?: string;
    deliveredAt?: string;
}

export interface RouteData {
    id: number;
    driverName: string;
    deliveries: Delivery[];
    // Tornamos opcional e string para evitar o crash
    createdAt?: string;
}

interface RouteThumbnailProps {
    route: RouteData;
}

// 2. Recebemos a prop 'route'
export default function RouteThumbnail({ route }: RouteThumbnailProps) {
    const theme = useColorScheme() ?? 'light';
    const cardBackgroundColor = theme === 'light' ? '#F4F4F5' : '#27272a';
    const iconPrimaryColor = Colors[theme].tint;
    const inactiveColor = Colors[theme].inactive;

    console.log("DADOS DA ROTA:", JSON.stringify(route, null, 2));
    // Formatação da data de criação da rota
    const dateToUse = route.createdAt ? new Date(route.createdAt) : new Date();
    let formattedDate = '';

    if (isToday(dateToUse)) {
        // Se for hoje: "Today, December 10"
        formattedDate = `Today, ${format(dateToUse, 'MMMM d', { locale: enUS })}`;
    } else {
        // Se NÃO for hoje: "Tuesday, December 9" (Dia da semana + Mês + Dia)
        formattedDate = format(dateToUse, 'EEEE, MMMM d', { locale: enUS });
    }

    // Lógica para pegar o primeiro e último ponto para exibição
    const firstDelivery = route.deliveries.length > 0 ? route.deliveries[0] : null;
    const lastDelivery = route.deliveries.length > 1 ? route.deliveries[route.deliveries.length - 1] : null;

    const formatTime = (dateString?: string) => {
        if (!dateString) return '--:--';

        const date = new Date(dateString);

        // Formata para 21:05
        return new Intl.DateTimeFormat('pt-BR', {
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        }).format(date);
    };


    return (
        <View style={styles.container}>
            {/* Usando ID da rota como "Data" ou identificador por enquanto */}
            <ThemedText type="subtitle" style={styles.dateHeader}>
                {formattedDate}
            </ThemedText>

            <View style={styles.card}>
                {/* Topo do Card */}
                <View style={[styles.cardHeader, { backgroundColor: cardBackgroundColor }]}>
                    {/* Placeholder de Preço (Seu backend ainda não envia preço, então deixei fixo ou você pode calcular) */}
                    <ThemedText type="title2">Route #{route.id}</ThemedText>

                    <View style={styles.metaContainer}>
                        <View style={styles.metaItem}>
                            <Ionicons name="cube-outline" size={16} color={iconPrimaryColor} />
                            <ThemedText type="default">{route.deliveries.length} Stops</ThemedText>
                        </View>
                    </View>
                </View>

                {/* Corpo do Card */}
                <View style={[styles.cardContent, { backgroundColor: cardBackgroundColor }]}>

                    {/* Renderiza a primeira entrega (ou ponto de partida simulado) */}
                    {firstDelivery ? (
                        <View style={styles.destinationInfo}>
                            <Ionicons name="play-outline" size={20} color={inactiveColor} />
                            <View style={styles.destinationAddress}>
                                <View style={styles.destinationTitle}>
                                    <ThemedText type='thin'>Stop #{firstDelivery.order}</ThemedText>
                                    <ThemedText type='thin'>{firstDelivery.deliveredAt
                                        ? format(new Date(firstDelivery.deliveredAt), 'HH:mm')
                                        : '--:--'}
                                    </ThemedText>
                                </View>
                                <View><ThemedText type='small'>{firstDelivery.dropoffAddress}</ThemedText></View>
                            </View>
                        </View>
                    ) : (
                        <ThemedText type='small'>No deliveries assigned.</ThemedText>
                    )}

                    {/* Renderiza a última entrega se houver mais de uma */}
                    {lastDelivery && (
                        <View style={styles.destinationInfo}>
                            <Ionicons name="flag-outline" size={20} color={inactiveColor} />
                            <View style={styles.destinationAddress}>
                                <View style={styles.destinationTitle}>
                                    <ThemedText type='thin'>Stop #{lastDelivery.order}</ThemedText>
                                    <ThemedText type='thin'>{lastDelivery.status}</ThemedText>
                                </View>
                                <View><ThemedText type='small' style={{ color: inactiveColor }}>{lastDelivery.dropoffAddress}</ThemedText></View>
                            </View>
                        </View>
                    )}

                    {/* Mapa Estático */}
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
        marginBottom: 16,
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
    cardContent: {
        width: '100%',
        padding: 8,
        flexDirection: 'column',
        gap: 12,
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
    mapImage: {
        width: '100%',
        height: 150,
        borderRadius: 12,
    }
});