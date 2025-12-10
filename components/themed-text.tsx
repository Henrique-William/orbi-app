import { useThemeColor } from '@/hooks/use-theme-color';
import { StyleSheet, Text, type TextProps } from 'react-native';

export type ThemedTextProps = TextProps & {
  lightColor?: string;
  darkColor?: string;
  type?: 'small' | 'thin' | 'default' | 'title' | 'title2' | 'defaultSemiBold' | 'subtitle' | 'link';
};

export function ThemedText({
  style,
  lightColor,
  darkColor,
  type = 'default',
  ...rest
}: ThemedTextProps) {
  // Isso garante a regra 3: cor muda baseada no tema automaticamente
  const color = useThemeColor({ light: lightColor, dark: darkColor }, 'text');

  return (
    <Text
      style={[
        { color },
        // Aplica a fonte Poppins baseada no estilo
        type === 'small' ? styles.small : undefined,
        type === 'thin' ? styles.thin : undefined,
        type === 'default' ? styles.default : undefined,
        type === 'title' ? styles.title : undefined,
        type === 'title2' ? styles.title2 : undefined,
        type === 'defaultSemiBold' ? styles.defaultSemiBold : undefined,
        type === 'subtitle' ? styles.subtitle : undefined,
        type === 'link' ? styles.link : undefined,
        style,
      ]}
      {...rest}
    />
  );
}

const styles = StyleSheet.create({
  small: {
    fontSize: 10,
    lineHeight: 16,
    fontFamily: 'Poppins_400Regular', // Regra 2
  },
  thin: {
    fontSize: 14,
    lineHeight: 20,
    fontFamily: 'Poppins_400Regular', // Regra 2
  },
  default: {
    fontSize: 16,
    lineHeight: 24,
    fontFamily: 'Poppins_400Regular', // Regra 2
  },
  defaultSemiBold: {
    fontSize: 16,
    lineHeight: 24,
    fontFamily: 'Poppins_600SemiBold', // Regra 2
  },
  title: {
    fontSize: 32,
    lineHeight: 40,
    fontFamily: 'Poppins_700Bold', // Regra 2
  },
  title2: {
    fontSize: 24,
    lineHeight: 32,
    fontFamily: 'Poppins_700Bold', // Regra 2
  },
  subtitle: {
    fontSize: 12,
    fontFamily: 'Poppins_400Regular', // Regra 2
  },
  link: {
    lineHeight: 30,
    fontSize: 16,
    color: '#0a7ea4',
    fontFamily: 'Poppins_400Regular', // Regra 2
  },
});