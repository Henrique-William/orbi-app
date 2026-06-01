import { Link, useRouter } from 'expo-router';
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

export default function RegisterScreen() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phoneNumber: '',
    password: '',
    confirmPassword: '',
  });
  const [loading, setLoading] = useState(false);

  const handleChange = (name: string, value: string) => {
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleRegister = async () => {
    if (formData.password !== formData.confirmPassword) {
      Alert.alert('Erro', 'As senhas não coincidem.');
      return;
    }

    if (!formData.name || !formData.email || !formData.password || !formData.phoneNumber) {
      Alert.alert('Erro', 'Por favor preencha todos os campos obrigatórios.');
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(API_URLS.REGISTER, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: formData.name,
          email: formData.email,
          password: formData.password,
          confirmPassword: formData.confirmPassword,
          phoneNumber: formData.phoneNumber,
        }),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Erro ao cadastrar');
      }

      Alert.alert('Sucesso', 'Cadastro realizado com sucesso!');
      router.replace('/login');
    } catch (err: any) {
      Alert.alert('Erro', err.message || 'Erro desconhecido ao cadastrar.');
    } finally {
      setLoading(false);
    }
  };


  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.keyboardView}
      >
        <ScrollView contentContainerStyle={{ flexGrow: 1 }}>
          <View style={styles.topSection}>
            {/* <Image
              source={require('../assets/images/react-logo@2x.png')} // Relative path bypasses alias bugs
              style={styles.logo}
              resizeMode="contain"
            /> */}
          </View>

          <ThemedView style={styles.formSection}>
            <ThemedText type="title" style={styles.title}>
              CADASTRO
            </ThemedText>

            <View style={styles.inputContainer}>
              <ThemedText style={styles.label}>Nome Completo</ThemedText>
              <TextInput
                style={styles.input}
                placeholder="Seu Nome Completo"
                placeholderTextColor="#999"
                value={formData.name}
                onChangeText={(val) => handleChange('name', val)}
              />
            </View>

            <View style={styles.inputContainer}>
              <ThemedText style={styles.label}>Email</ThemedText>
              <TextInput
                style={styles.input}
                placeholder="exemplo@email.com"
                placeholderTextColor="#999"
                value={formData.email}
                onChangeText={(val) => handleChange('email', val)}
                autoCapitalize="none"
                keyboardType="email-address"
              />
            </View>

            <View style={styles.inputContainer}>
              <ThemedText style={styles.label}>Telefone</ThemedText>
              <TextInput
                style={styles.input}
                placeholder="(XX) XXXXX-XXXX"
                placeholderTextColor="#999"
                value={formData.phoneNumber}
                onChangeText={(val) => handleChange('phoneNumber', val)}
                keyboardType="phone-pad"
              />
            </View>

            <View style={styles.inputContainer}>
              <ThemedText style={styles.label}>Senha</ThemedText>
              <TextInput
                style={styles.input}
                placeholder="Sua Senha"
                placeholderTextColor="#999"
                value={formData.password}
                onChangeText={(val) => handleChange('password', val)}
                secureTextEntry
              />
            </View>

            <View style={styles.inputContainer}>
              <ThemedText style={styles.label}>Confirmar Senha</ThemedText>
              <TextInput
                style={styles.input}
                placeholder="Confirme a Sua Senha"
                placeholderTextColor="#999"
                value={formData.confirmPassword}
                onChangeText={(val) => handleChange('confirmPassword', val)}
                secureTextEntry
              />
            </View>

            <TouchableOpacity
              style={styles.registerButton}
              onPress={handleRegister}
              disabled={loading}
            >
              <ThemedText style={styles.registerButtonText}>
                {loading ? 'Cadastrando...' : 'Cadastrar'}
              </ThemedText>
            </TouchableOpacity>

            <View style={styles.footerText}>
              <ThemedText>Já tem uma conta? </ThemedText>
              <Link href="/login" asChild>
                <TouchableOpacity>
                  <ThemedText style={styles.linkText}>Entrar</ThemedText>
                </TouchableOpacity>
              </Link>
            </View>
          </ThemedView>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#450693',
  },
  keyboardView: {
    flex: 1,
  },
  topSection: {
    height: 150,
    justifyContent: 'center',
    alignItems: 'center',
  },
  logo: {
    width: 100,
    height: 100,
  },
  formSection: {
    flex: 1,
    backgroundColor: '#fff',
    borderTopLeftRadius: 30,
    borderTopRightRadius: 30,
    padding: 24,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -2 },
    shadowOpacity: 0.1,
    shadowRadius: 10,
    elevation: 5,
  },
  title: {
    textAlign: 'center',
    marginBottom: 24,
    color: '#000',
  },
  inputContainer: {
    marginBottom: 16,
  },
  label: {
    marginBottom: 8,
    color: '#333',
    fontSize: 14,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    color: '#000',
  },
  registerButton: {
    backgroundColor: '#450693',
    borderRadius: 8,
    padding: 16,
    alignItems: 'center',
    marginTop: 16,
  },
  registerButtonText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 16,
  },
  footerText: {
    flexDirection: 'row',
    justifyContent: 'center',
    marginTop: 24,
    marginBottom: 40,
  },
  linkText: {
    color: '#450693',
    fontWeight: 'bold',
  },
});
