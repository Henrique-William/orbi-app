
const palette = {
  backgroundDark: '#111111',
  backgroundDarkSecondary: '#18181b',
  primary: '#6366f1',
  inactive: '#d0d0d6ff',
  white: '#FFFFFF',
  black: '#000000',
};

export const Colors = {
  light: {
    text: palette.backgroundDark,
    background: palette.white,
    tint: palette.primary,
    icon: palette.backgroundDark,
    tabIconDefault: palette.inactive,
    tabIconSelected: palette.primary,
    ...palette,
  },
  dark: {
    text: palette.white,
    background: palette.backgroundDark,
    tint: palette.primary,
    icon: palette.inactive,
    tabIconDefault: palette.inactive,
    tabIconSelected: palette.primary,
    ...palette,
  },
};