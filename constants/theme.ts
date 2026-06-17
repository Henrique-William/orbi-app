
const palette = {
  backgroundDark: '#111111',
  backgroundDarkSecondary: '#18181b',
  primary: '#6366f1',
  green: '#38CB89',
  gray: '#a1a1a1',
  offWhite: '#F4F5FC',
  white: '#ffffff',
  black: '#000000',
};

export const Colors = {
  light: {
    text: palette.backgroundDark,
    background: palette.offWhite,
    backgroundCard: palette.white,
    icon: palette.backgroundDark,
    tabIconDefault: palette.gray,
    ...palette,
  },
  dark: {
    text: palette.offWhite,
    background: palette.backgroundDark,
    backgroundCard: palette.backgroundDarkSecondary,
    icon: palette.gray,
    tabIconDefault: palette.gray,
    ...palette,
  },
};