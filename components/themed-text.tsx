import { useThemeColor } from '@/hooks/use-theme-color';
import { StyleSheet, Text, type TextProps } from 'react-native';



export type ThemedTextProps = TextProps & {
  lightColor?: string;
  darkColor?: string;
  type?: 'small' | 'thin' | 'default' | 'titleBold' | 'title' | 'subtitle' | 'defaultSemiBold' | 'link';
};

export function ThemedText({
  style,
  lightColor,
  darkColor,
  type = 'default',
  ...rest
}: ThemedTextProps) {
  const color = useThemeColor({ light: lightColor, dark: darkColor }, 'text');
  return (
    <Text
      style={[
        { color },
        type === 'small' ? styles.small : undefined,
        type === 'thin' ? styles.thin : undefined,
        type === 'default' ? styles.default : undefined,
        type === 'titleBold' ? styles.titleBold : undefined,
        type === 'title' ? styles.title : undefined,
        type === 'subtitle' ? styles.subtitle : undefined,
        type === 'defaultSemiBold' ? styles.defaultSemiBold : undefined,
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
    lineHeight: 10,
    fontFamily: 'Poppins_400Regular',
  },
  thin: {
    fontSize: 14,
    lineHeight: 20,
    fontFamily: 'Poppins_400Regular',
  },
  default: {
    fontSize: 16,
    lineHeight: 24,
    fontFamily: 'Poppins_400Regular',
  },
  defaultSemiBold: {
    fontSize: 16,
    lineHeight: 20,
    fontFamily: 'Poppins_600SemiBold',
  },
  titleBold: {
    fontSize: 32,
    lineHeight: 40,
    fontFamily: 'Poppins_700Bold',
  },
  title: {
    fontSize: 32,
    lineHeight: 40,
    fontFamily: 'Poppins_400Regular',
  },
  subtitle: {
    fontSize: 20,
    lineHeight: 28,
    fontFamily: 'Poppins_400Regular',
  },
  link: {
    lineHeight: 26,
    fontSize: 18,
    fontFamily: 'Poppins_400Regular',
  },
});