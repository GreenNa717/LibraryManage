import { ref } from 'vue'
import { applyTheme, getStoredTheme } from '../theme'
import { THEME_OPTIONS } from '../theme/themes'

const initialTheme = typeof document !== 'undefined'
  ? document.documentElement.getAttribute('data-theme')
  : getStoredTheme()
const currentTheme = ref(initialTheme || getStoredTheme())

export const useTheme = () => {
  const setTheme = (themeKey) => {
    const applied = applyTheme(themeKey)
    if (currentTheme.value !== applied) {
      currentTheme.value = applied
    }
  }

  return {
    currentTheme,
    themeOptions: THEME_OPTIONS,
    setTheme
  }
}
