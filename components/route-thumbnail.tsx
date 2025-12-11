import { Ionicons } from '@expo/vector-icons';
import { format, isToday } from 'date-fns';
import { enUS } from 'date-fns/locale';
import React, { useMemo } from 'react';
import { Image, StyleSheet, View } from 'react-native';

import { Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';
import { ThemedText } from './themed-text';

// Importe os tipos centralizados (ajuste o caminho conforme onde criou o arquivo)
import { RouteData } from '@/constants/types/interfaces';

interface RouteThumbnailProps {
    route: RouteData;
}

export default function RouteThumbnail({ route }: RouteThumbnailProps) {
    const theme = useColorScheme() ?? 'light';

    // Cores derivadas do tema
    const colors = {
        cardBackground: theme === 'light' ? '#F4F4F5' : '#27272a',
        iconPrimary: Colors[theme].tint,
        inactive: Colors[theme].inactive,
        textSecondary: Colors[theme].icon,
    };

    // Lógica de Data (useMemo evita recálculos desnecessários)
    const formattedDate = useMemo(() => {
        const dateToUse = route.createdAt ? new Date(route.createdAt) : new Date();
        if (isToday(dateToUse)) {
            return `Today, ${format(dateToUse, 'MMMM d', { locale: enUS })}`;
        }
        return format(dateToUse, 'EEEE, MMMM d', { locale: enUS });
    }, [route.createdAt]);

    const firstDelivery = route.deliveries.at(0);
    const lastDelivery = route.deliveries.length > 1 ? route.deliveries.at(-1) : null;

    // Sub-componente interno para renderizar uma linha de entrega (DRY - Don't Repeat Yourself)
    const DeliveryRow = ({
        icon,
        order,
        timeOrStatus,
        address,
        isLast = false
    }: {
        icon: keyof typeof Ionicons.glyphMap;
        order: number;
        timeOrStatus: string;
        address: string;
        isLast?: boolean;
    }) => (
        <View style={styles.destinationInfo}>
            <Ionicons name={icon} size={20} color={colors.inactive} />
            <View style={styles.destinationAddress}>
                <View style={styles.destinationTitle}>
                    <ThemedText type='thin'>Stop #{order}</ThemedText>
                    <ThemedText type='thin'>{timeOrStatus}</ThemedText>
                </View>
                <View>
                    <ThemedText type='small' style={isLast ? { color: colors.inactive } : undefined}>
                        {address}
                    </ThemedText>
                </View>
            </View>
        </View>
    );

    return (
        <View style={styles.container}>
            <ThemedText type="subtitle" style={styles.dateHeader}>
                {formattedDate}
            </ThemedText>

            <View style={styles.card}>
                {/* Header do Card */}
                <View style={[styles.cardHeader, { backgroundColor: colors.cardBackground }]}>
                    <ThemedText type="title2">Route #{route.id}</ThemedText>

                    <View style={styles.metaContainer}>
                        <Ionicons name="cube-outline" size={16} color={colors.iconPrimary} />
                        <ThemedText type="default">{route.deliveries.length} Stops</ThemedText>
                    </View>
                </View>

                {/* Conteúdo do Card */}
                <View style={[styles.cardContent, { backgroundColor: colors.cardBackground }]}>

                    {firstDelivery ? (
                        <DeliveryRow
                            icon="play-outline"
                            order={firstDelivery.order}
                            timeOrStatus={firstDelivery.deliveredAt
                                ? format(new Date(firstDelivery.deliveredAt), 'HH:mm')
                                : '--:--'}
                            address={firstDelivery.dropoffAddress}
                        />
                    ) : (
                        <ThemedText type='small'>No deliveries assigned.</ThemedText>
                    )}

                    {lastDelivery && (
                        <DeliveryRow
                            icon="flag-outline"
                            order={lastDelivery.order}
                            timeOrStatus={lastDelivery.deliveredAt
                                ? format(new Date(lastDelivery.deliveredAt), 'HH:mm')
                                : '--:--'}
                            address={lastDelivery.dropoffAddress}
                            isLast
                        />
                    )}

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
        padding: 12, // Aumentado ligeiramente para melhor toque visual
    },
    metaContainer: {
        flexDirection: 'row',
        gap: 6,
        alignItems: 'center',
    },
    cardContent: {
        width: '100%',
        padding: 12,
        flexDirection: 'column',
        gap: 16, // Mais espaçamento entre elementos internos
    },
    destinationInfo: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    destinationAddress: {
        flexDirection: 'column',
        paddingLeft: 12,
        flex: 1, // Garante que o texto ocupe o espaço restante corretamente
    },
    destinationTitle: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginBottom: 2,
    },
    mapImage: {
        width: '100%',
        height: 150,
        borderRadius: 12,
        marginTop: 4,
    }
});