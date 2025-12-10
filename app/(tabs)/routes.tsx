import { ScrollView, StyleSheet, View } from 'react-native'; // 1. Importe ScrollView
import { SafeAreaView } from 'react-native-safe-area-context';

import RouteThumbnail from '@/components/route-thumbnail';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';

export default function Routes() {
  return (
    <ThemedView style={styles.container}>
      {/* 2. SafeAreaView com flex: 1 para garantir que o ScrollView tenha espaço */}
      <SafeAreaView style={styles.safeArea}>

        {/* 3. Adicione o ScrollView envolvendo o conteúdo */}
        <ScrollView
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false} // Opcional: esconde a barra de rolagem
        >
          <View style={styles.dropdown}></View>
          <ThemedText type='title' style={styles.title}>My rides</ThemedText>

          {/* Seus componentes de rota */}
          <RouteThumbnail />
          <RouteThumbnail />
          <RouteThumbnail />
          <RouteThumbnail />

          {/* <FlatList
            data={suaListaDeRotas} // Array com os dados
            renderItem={({ item }) => <RouteThumbnail data={item} />} // Como renderizar cada item
            contentContainerStyle={styles.scrollContent}
            ListHeaderComponent={
              // O que aparece em cima da lista rolando junto
              <>
                <View style={styles.dropdown}></View>
                <ThemedText type='title' style={styles.title}>My rides</ThemedText>
              </>
            }
          /> */}

        </ScrollView>

      </SafeAreaView>
    </ThemedView>
  )
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